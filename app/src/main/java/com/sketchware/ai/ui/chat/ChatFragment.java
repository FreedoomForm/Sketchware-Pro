package com.sketchware.ai.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import pro.sketchware.R;
import com.sketchware.ai.agent.AgentListener;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.agent.AgentRuntime;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.providers.AnthropicProvider;
import com.sketchware.ai.llm.providers.GeminiProvider;
import com.sketchware.ai.llm.providers.OllamaProvider;
import com.sketchware.ai.llm.providers.OpenAiCompatProvider;
import com.sketchware.ai.llm.providers.OpenAiProvider;
import com.sketchware.ai.llm.storage.ProviderConfigStore;
import com.sketchware.ai.prompt.SystemPromptBuilder;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolPermissionGate;
import com.sketchware.ai.tools.ToolRegistry;
import com.sketchware.ai.tools.ToolRegistryInitializer;
import com.sketchware.ai.ui.chat.adapter.ChatAdapter;
import com.sketchware.ai.ui.settings.AISettingsActivity;

public final class ChatFragment extends Fragment {

    private RecyclerView recycler;
    private com.google.android.material.textfield.TextInputEditText input;
    private com.google.android.material.button.MaterialButton btnSend;
    private com.google.android.material.button.MaterialButton btnStop;
    private com.google.android.material.button.MaterialButton btnAttach;
    private com.google.android.material.materialswitch.MaterialSwitch planActToggle;
    private com.google.android.material.progressindicator.LinearProgressIndicator contextProgress;

    private ChatAdapter adapter;
    private final MessageReducer reducer = new MessageReducer();
    private AgentRuntime agent;
    private ToolRegistry toolRegistry;
    private ToolPermissionGate permissionGate;
    private ProviderConfigStore.Profile profile;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init registry + permission gate once.
        toolRegistry = ToolRegistryInitializer.createDefault();
        permissionGate = new ToolPermissionGate();
        // Load active profile from storage.
        ProviderConfigStore store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_chat, container, false);
        recycler = root.findViewById(R.id.recycler);
        input = root.findViewById(R.id.input);
        btnSend = root.findViewById(R.id.btn_send);
        btnStop = root.findViewById(R.id.btn_stop);
        btnAttach = root.findViewById(R.id.btn_attach);
        planActToggle = root.findViewById(R.id.plan_act_toggle);
        contextProgress = root.findViewById(R.id.context_progress);

        adapter = new ChatAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // Wire up toolbar menu (AI Settings / Clear / Export).
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // No drawer here; ignore.
        });
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_ai_settings) {
                startActivity(AISettingsActivity.newIntent(requireContext(), AISettingsActivity.FRAGMENT_PROVIDER));
                return true;
            } else if (id == R.id.menu_ai_clear) {
                reducer.reset();
                adapter.submitList(reducer.getMessages());
                return true;
            } else if (id == R.id.menu_ai_export) {
                if (getView() != null) {
                    Snackbar.make(getView(), "Export conversation: not yet implemented", Snackbar.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // Send on Enter (without shift)
        input.setOnEditorActionListener((v, actionId, event) -> {
            send();
            return true;
        });

        btnSend.setOnClickListener(v -> send());
        btnStop.setOnClickListener(v -> {
            if (agent != null) agent.abort();
            btnStop.setVisibility(View.GONE);
            btnAttach.setVisibility(View.VISIBLE);
        });
        planActToggle.setOnCheckedChangeListener((b, checked) -> {
            AgentMode mode = checked ? AgentMode.PLAN : AgentMode.ACT;
            if (agent != null) agent.setMode(mode);
        });

        // Reload profile when returning from settings (onResume handles this).
        adapter.submitList(reducer.getMessages());
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        // Reload profile from storage (in case the user changed it in AISettingsActivity).
        ProviderConfigStore store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
        // If the agent hasn't started yet, we'll use the new profile next time.
        // If it has started, the user can clear the conversation to start fresh.
    }

    private void send() {
        Editable e = input.getText();
        if (e == null) return;
        String text = e.toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);

        // Show stop button while running
        btnAttach.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);

        // Append user message
        reducer.addUserMessage(text);
        adapter.submitList(reducer.getMessages());
        recycler.scrollToPosition(reducer.getMessages().size() - 1);

        // Build or reuse agent
        if (agent == null) {
            LlmProvider provider = buildProvider(profile);
            ModelInfo model = provider.getModel(profile.modelId);
            String scId = readScIdFromActivity();
            String javaName = readJavaNameFromActivity();
            String systemPrompt = SystemPromptBuilder.build(
                    AgentMode.ACT, toolRegistry,
                    "/sdcard/.sketchware/data/" + scId, "Sketchware Project", "com.example", 21, 34);
            agent = new AgentRuntime(provider, toolRegistry, permissionGate, profile, systemPrompt);
            // Set the thread-local context for tool execution.
            SketchwareToolContext toolCtx = new SketchwareToolContext(
                    requireActivity(), scId, javaName, permissionGate,
                    /* viewRefresh */ () -> {},
                    /* logicRefresh */ () -> {},
                    /* eventRefresh */ () -> {},
                    /* componentRefresh */ () -> {});
            AgentRuntime.setContext(toolCtx);
        }

        // Listener
        AgentListener listener = new AgentListener() {
            @Override public void onTextDelta(String delta) {
                requireActivity().runOnUiThread(() -> {
                    reducer.appendText(delta);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onReasoningDelta(String delta) {
                requireActivity().runOnUiThread(() -> {
                    reducer.appendReasoning(delta);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onToolCalls(java.util.List<AgentMessage.ToolCall> calls) {
                requireActivity().runOnUiThread(() -> {
                    for (AgentMessage.ToolCall c : calls) {
                        reducer.addToolCall(c.name, c.argumentsJson);
                    }
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onToolStart(String toolCallId, String toolName, String argsJson) {}
            @Override public void onToolResult(String toolCallId, AgentMessage.ToolResultContent result) {
                requireActivity().runOnUiThread(() -> {
                    reducer.addToolResult(result.toolName, result.output, result.isError);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onUsage(int inT, int outT, int reasoningTokens, double cost) {
                requireActivity().runOnUiThread(() -> {
                    reducer.addUsage(inT, outT, cost);
                    adapter.submitList(reducer.getMessages());
                });
            }
            @Override public void onComplete(String finalText) {
                requireActivity().runOnUiThread(() -> {
                    reducer.finishStreaming();
                    reducer.addCompletion(finalText);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                    btnStop.setVisibility(View.GONE);
                    btnAttach.setVisibility(View.VISIBLE);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Task complete", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onWarning(String message) {
                requireActivity().runOnUiThread(() -> {
                    reducer.addError("Warning: " + message);
                    adapter.submitList(reducer.getMessages());
                });
            }
            @Override public void onError(Throwable error) {
                requireActivity().runOnUiThread(() -> {
                    reducer.addError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
                    adapter.submitList(reducer.getMessages());
                    btnStop.setVisibility(View.GONE);
                    btnAttach.setVisibility(View.VISIBLE);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onMaxIterationsReached(int max) {
                requireActivity().runOnUiThread(() -> {
                    reducer.addError("Reached max iterations (" + max + ")");
                    adapter.submitList(reducer.getMessages());
                    btnStop.setVisibility(View.GONE);
                    btnAttach.setVisibility(View.VISIBLE);
                });
            }
        };

        try {
            agent.execute(text, listener);
        } catch (Throwable t) {
            btnStop.setVisibility(View.GONE);
            btnAttach.setVisibility(View.VISIBLE);
            reducer.addError(t.getMessage());
            adapter.submitList(reducer.getMessages());
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (agent != null) agent.abort();
        AgentRuntime.clearContext();
    }

    private LlmProvider buildProvider(ProviderConfigStore.Profile profile) {
        switch (profile.providerId) {
            case "anthropic":   return new AnthropicProvider();
            case "openai":      return new OpenAiProvider();
            case "gemini":      return new GeminiProvider();
            case "ollama":      return new OllamaProvider();
            case "mistral":    return new OpenAiCompatProvider("mistral", "https://api.mistral.ai/v1");
            case "openrouter":  return new OpenAiCompatProvider("openrouter", "https://openrouter.ai/api");
            case "deepseek":    return new OpenAiCompatProvider("deepseek", "https://api.deepseek.com");
            case "zai":
            case "z-ai":        return new OpenAiCompatProvider("zai", "https://api.z.ai/api/paas/v4");
            case "together":    return new OpenAiCompatProvider("together", "https://api.together.xyz");
            case "fireworks":   return new OpenAiCompatProvider("fireworks", "https://api.fireworks.ai/inference");
            case "openai-compat":
            default:            return new OpenAiCompatProvider("openai-compat", profile.baseUrl);
        }
    }

    private String readScIdFromActivity() {
        try {
            // DesignActivity.sc_id is a public static field.
            return com.besome.sketch.design.DesignActivity.sc_id;
        } catch (Throwable t) {
            return "0";
        }
    }

    private String readJavaNameFromActivity() {
        // For MVP, return "main" - the actual current layout file.
        // The real implementation would query DesignActivity for the active ProjectFileBean.
        return "main";
    }
}
