package com.sketchware.ai.ui.chat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
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
import com.sketchware.ai.ui.settings.AutoApproveFragment;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class ChatFragment extends Fragment {

    private RecyclerView recycler;
    private TextInputEditText input;
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

    /**
     * Tracks whether the agent loop is currently running. Set to {@code true}
     * in {@link #send()} and reset to {@code false} in every terminal listener
     * callback ({@code onComplete}, {@code onError}, {@code onAborted},
     * {@code onMaxIterationsReached}). Prevents the user from queueing a
     * second message while the first is still in flight — previously the
     * second click cleared the input box BEFORE {@code agent.execute()}
     * rejected the duplicate run, silently losing the user's text.
     */
    private volatile boolean isRunning = false;

    /**
     * Cached "signature" of the last profile used to build {@link #agent}.
     * When the user returns from {@link AISettingsActivity} and the signature
     * differs, the old agent is discarded so the next {@link #send()} rebuilds
     * it with the new provider / API key / model. Without this, changes made
     * in the settings UI were silently ignored once the agent had been built.
     */
    private String lastProfileSignature = "";

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init registry + permission gate once.
        toolRegistry = ToolRegistryInitializer.createDefault();
        permissionGate = new ToolPermissionGate();
        // Load active profile from storage.
        ProviderConfigStore store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
        lastProfileSignature = profileSignature(profile);
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

        // Restore the send/stop button state to match the agent's running
        // state. If the view was destroyed and recreated while the agent was
        // still running (e.g. config change, or the user navigated away and
        // came back), the buttons must reflect that — otherwise the Stop
        // button would be hidden (default XML state) and the user would have
        // no way to abort the in-flight run, while send() would reject new
        // messages with "Agent is already running".
        if (isRunning) {
            btnAttach.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);
        } else {
            btnAttach.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            btnSend.setEnabled(true);
        }

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
                clearConversation();
                return true;
            } else if (id == R.id.menu_ai_export) {
                if (getView() != null) {
                    Snackbar.make(getView(), "Export conversation: not yet implemented", Snackbar.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // Send on Enter (without shift). Only consume the action when it's
        // actually the IME's "send" action — returning true unconditionally
        // used to swallow every actionId, including unknown ones.
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });

        btnSend.setOnClickListener(v -> send());
        btnStop.setOnClickListener(v -> {
            if (agent != null) agent.abort();
            // The onAborted listener will hide the stop button; but set it
            // gone immediately so the user gets instant feedback.
            btnStop.setVisibility(View.GONE);
            btnAttach.setVisibility(View.VISIBLE);
        });
        planActToggle.setOnCheckedChangeListener((b, checked) -> {
            AgentMode mode = checked ? AgentMode.PLAN : AgentMode.ACT;
            if (agent != null) agent.setMode(mode);
            // If agent is null, the mode will be read from the toggle when
            // the agent is first built in send().
        });

        adapter.submitList(reducer.getMessages());
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        // Reload profile from storage (in case the user changed it in
        // AISettingsActivity). If the profile's key fields changed, discard
        // the old agent so the next send() rebuilds it with the new config.
        ProviderConfigStore store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
        String sig = profileSignature(profile);
        if (!sig.equals(lastProfileSignature)) {
            if (agent != null) {
                agent.abort();
                agent = null;
            }
            lastProfileSignature = sig;
            isRunning = false;
            // Restore the send button in case we aborted mid-run.
            if (btnSend != null) btnSend.setEnabled(true);
            if (btnStop != null) btnStop.setVisibility(View.GONE);
            if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        // Abort the agent so the background thread stops; null out the
        // reference so the listener (which holds an implicit reference to
        // this fragment via the anonymous inner class) doesn't keep the
        // destroyed Activity alive via the agent's executor.
        if (agent != null) {
            agent.abort();
            agent = null;
        }
        isRunning = false;
    }

    // ------------------------------------------------------------------
    // Conversation actions
    // ------------------------------------------------------------------

    private void clearConversation() {
        // Abort any in-flight agent run and discard the agent so the next
        // send() rebuilds it with a fresh conversation history. Previously
        // "Clear" only wiped the UI reducer — the agent kept running and
        // its listener kept appending to the now-empty reducer, while the
        // agent's internal conversationHistory also survived, so the user's
        // "clear" didn't actually clear anything.
        if (agent != null) {
            agent.abort();
            agent = null;
        }
        isRunning = false;
        reducer.reset();
        if (adapter != null) adapter.submitList(reducer.getMessages());
        if (btnSend != null) btnSend.setEnabled(true);
        if (btnStop != null) btnStop.setVisibility(View.GONE);
        if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);
    }

    private void send() {
        if (isRunning) {
            // A run is already in flight. Don't clear the input — let the
            // user decide whether to abort (Stop button) or wait.
            View v = getView();
            if (v != null) {
                Snackbar.make(v, "Agent is already running", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        Editable e = input == null ? null : input.getText();
        if (e == null) return;
        String text = e.toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        // Hide keyboard
        Context ctx = getContext();
        if (ctx != null) {
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            View focused = getView();
            if (imm != null && focused != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }

        // Show stop button while running, disable send button to prevent
        // duplicate submissions.
        isRunning = true;
        if (btnAttach != null) btnAttach.setVisibility(View.GONE);
        if (btnStop != null) btnStop.setVisibility(View.VISIBLE);
        if (btnSend != null) btnSend.setEnabled(false);

        // Append user message
        reducer.addUserMessage(text);
        if (adapter != null) adapter.submitList(reducer.getMessages());
        if (recycler != null) recycler.scrollToPosition(reducer.getMessages().size() - 1);

        // Build or reuse agent.
        if (agent == null) {
            LlmProvider provider = buildProvider(profile);
            ModelInfo model = provider.getModel(profile.modelId);
            String scId = readScIdFromActivity();
            String javaName = readJavaNameFromActivity();
            String systemPrompt = SystemPromptBuilder.build(
                    AgentMode.ACT, toolRegistry,
                    "/sdcard/.sketchware/data/" + scId, "Sketchware Project", "com.example", 21, 34);
            agent = new AgentRuntime(provider, toolRegistry, permissionGate, profile, systemPrompt);
            // Read user-configured max iterations from the Auto-Approve
            // settings page (defaults to 50 if the user never changed it).
            int maxIter = readMaxIterations();
            agent.setMaxIterations(maxIter);
            // Determine the initial agent mode. The Auto-Approve "YOLO mode"
            // master switch overrides the plan/act toggle when enabled.
            AgentMode initialMode = AgentMode.ACT;
            if (isYoloEnabled()) {
                initialMode = AgentMode.YOLO;
            } else if (planActToggle != null && planActToggle.isChecked()) {
                initialMode = AgentMode.PLAN;
            }
            agent.setMode(initialMode);
        }

        // Always refresh the tool context: the user may have navigated to a
        // different project (different sc_id) since the last send, and the
        // agent's stored context would then point at the wrong project.
        // This is cheap (just a volatile field write) so it's safe to call
        // on every send.
        String scId = readScIdFromActivity();
        String javaName = readJavaNameFromActivity();
        SketchwareToolContext toolCtx = new SketchwareToolContext(
                requireActivity(), scId, javaName, permissionGate,
                /* viewRefresh */ () -> {},
                /* logicRefresh */ () -> {},
                /* eventRefresh */ () -> {},
                /* componentRefresh */ () -> {});
        // Set the tool execution context. MUST be the instance method, NOT
        // the legacy static AgentRuntime.setContext() — that one was backed
        // by a ThreadLocal and silently returned null on the executor
        // thread, causing every tool call to fail with
        // "No tool context available." The instance method stores the
        // context in a volatile field visible across threads.
        agent.setContext(toolCtx);

        // Listener
        AgentListener listener = new AgentListener() {
            @Override public void onTextDelta(String delta) {
                runOnUiIfAlive(() -> {
                    reducer.appendText(delta);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onReasoningDelta(String delta) {
                runOnUiIfAlive(() -> {
                    reducer.appendReasoning(delta);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onToolCalls(List<AgentMessage.ToolCall> calls) {
                runOnUiIfAlive(() -> {
                    for (AgentMessage.ToolCall c : calls) {
                        reducer.addToolCall(c.name, c.argumentsJson);
                    }
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onToolStart(String toolCallId, String toolName, String argsJson) {}
            @Override public void onToolResult(String toolCallId, AgentMessage.ToolResultContent result) {
                runOnUiIfAlive(() -> {
                    reducer.addToolResult(result.toolName, result.output, result.isError);
                    adapter.submitList(reducer.getMessages());
                    recycler.scrollToPosition(reducer.getMessages().size() - 1);
                });
            }
            @Override public void onUsage(int inT, int outT, int reasoningTokens, double cost) {
                runOnUiIfAlive(() -> {
                    reducer.addUsage(inT, outT, cost);
                    adapter.submitList(reducer.getMessages());
                });
            }
            @Override public void onComplete(String finalText) {
                // Reset isRunning on the background thread so the flag is
                // cleared even if the fragment view is gone (runOnUiIfAlive
                // would otherwise skip the reset inside finishRun).
                isRunning = false;
                runOnUiIfAlive(() -> {
                    reducer.finishStreaming();
                    // Only add a separate completion row if the agent didn't
                    // stream any text (e.g. submit_and_exit immediately
                    // after a tool call). When text WAS streamed, the
                    // streaming row already shows it — adding it again as a
                    // completion row duplicated the whole message.
                    if (finalText != null && !finalText.isEmpty()
                            && !reducer.lastMessageIsStreamingText()) {
                        reducer.addCompletion(finalText);
                    }
                    adapter.submitList(reducer.getMessages());
                    if (!reducer.getMessages().isEmpty()) {
                        recycler.scrollToPosition(reducer.getMessages().size() - 1);
                    }
                    finishRun("Task complete");
                });
            }
            @Override public void onAborted(String partialText) {
                // Distinguish abort from completion in the UI: finish the
                // streaming indicator but show a "Stopped" snackbar instead
                // of "Task complete".
                isRunning = false;
                runOnUiIfAlive(() -> {
                    reducer.finishStreaming();
                    // Same duplicate-avoidance as onComplete: only emit a
                    // completion row if no text was streamed.
                    if (partialText != null && !partialText.isEmpty()
                            && !reducer.lastMessageIsStreamingText()) {
                        reducer.addCompletion(partialText);
                    }
                    adapter.submitList(reducer.getMessages());
                    finishRun("Stopped");
                });
            }
            @Override public void onWarning(String message) {
                runOnUiIfAlive(() -> {
                    reducer.addError("Warning: " + message);
                    adapter.submitList(reducer.getMessages());
                });
            }
            @Override public void onError(Throwable error) {
                isRunning = false;
                String msg = error.getMessage() == null
                        ? error.getClass().getSimpleName() : error.getMessage();
                runOnUiIfAlive(() -> {
                    reducer.addError(msg);
                    adapter.submitList(reducer.getMessages());
                    finishRun("Error: " + msg);
                });
            }
            @Override public void onMaxIterationsReached(int max) {
                isRunning = false;
                runOnUiIfAlive(() -> {
                    reducer.addError("Reached max iterations (" + max + ")");
                    adapter.submitList(reducer.getMessages());
                    finishRun("Max iterations reached");
                });
            }
            @Override public boolean requestApproval(AgentMessage.ToolCall call) {
                // Show a real approval dialog on the UI thread and block the
                // agent's background thread until the user responds. The
                // default implementation returns true (auto-approve) which
                // silently bypasses the permission gate for every mutating
                // tool in ACT mode — that defeats the whole point of the
                // gate. This override enforces the audit prompt's "Bug
                // category 7: Permission gate UI" requirement.
                return ChatFragment.this.requestApproval(call);
            }
        };

        try {
            agent.execute(text, listener);
        } catch (Throwable t) {
            String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            reducer.addError(msg);
            if (adapter != null) adapter.submitList(reducer.getMessages());
            finishRun("Error: " + msg);
        }
    }

    /**
     * Show a synchronous approval dialog for a mutating tool call. Blocks
     * the calling (background) thread on a {@link CountDownLatch} until the
     * user taps Approve / Deny / outside-the-dialog. Returns {@code false}
     * (deny) if the fragment is no longer attached or the user dismisses
     * the dialog without explicitly approving.
     */
    private boolean requestApproval(AgentMessage.ToolCall call) {
        Activity a = getActivity();
        if (a == null) return false; // Fragment detached: deny.
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] approved = {false};
        a.runOnUiThread(() -> {
            if (a.isFinishing() || a.isDestroyed()) {
                latch.countDown();
                return;
            }
            String args = call.argumentsJson;
            if (args != null && args.length() > 500) {
                args = args.substring(0, 500) + "...(" + args.length() + " chars)";
            }
            new AlertDialog.Builder(a)
                    .setTitle("Approve tool call?")
                    .setMessage("Tool: " + call.name + "\nArgs: " + (args == null ? "{}" : args))
                    .setPositiveButton("Approve", (d, w) -> { approved[0] = true; latch.countDown(); })
                    .setNegativeButton("Deny", (d, w) -> { approved[0] = false; latch.countDown(); })
                    .setOnCancelListener(d -> { approved[0] = false; latch.countDown(); })
                    .show();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return approved[0];
    }

    /**
     * Helper that runs {@code r} on the UI thread ONLY if the fragment is
     * still attached and its view is still alive. This replaces the previous
     * pattern of calling {@code requireActivity().runOnUiThread(...)} inside
     * every listener callback — {@code requireActivity()} throws
     * {@link IllegalStateException} once the fragment is detached, which
     * crashed the agent's background thread when the user navigated away
     * mid-stream.
     */
    private void runOnUiIfAlive(Runnable r) {
        Activity a = getActivity();
        if (a == null) return;
        a.runOnUiThread(() -> {
            if (getView() == null) return; // view destroyed — skip UI update
            r.run();
        });
    }

    /** Common tail for terminal listener callbacks: hide Stop, show Attach, re-enable Send, show snackbar. */
    private void finishRun(String snackbarText) {
        isRunning = false;
        if (btnStop != null) btnStop.setVisibility(View.GONE);
        if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);
        if (btnSend != null) btnSend.setEnabled(true);
        View v = getView();
        if (v != null) {
            Snackbar.make(v, snackbarText, Snackbar.LENGTH_SHORT).show();
        }
    }

    private LlmProvider buildProvider(ProviderConfigStore.Profile profile) {
        String pid = profile.providerId == null ? "" : profile.providerId;
        switch (pid) {
            case "anthropic":   return new AnthropicProvider();
            case "openai":      return new OpenAiProvider();
            case "gemini":      return new GeminiProvider();
            case "ollama":      return new OllamaProvider();
            case "mistral":     return new OpenAiCompatProvider("mistral", "https://api.mistral.ai/v1");
            case "openrouter":  return new OpenAiCompatProvider("openrouter", "https://openrouter.ai/api");
            case "deepseek":    return new OpenAiCompatProvider("deepseek", "https://api.deepseek.com");
            case "zai":         return new OpenAiCompatProvider("zai", "https://api.z.ai/api/paas/v4");
            case "together":    return new OpenAiCompatProvider("together", "https://api.together.xyz");
            case "fireworks":   return new OpenAiCompatProvider("fireworks", "https://api.fireworks.ai/inference");
            case "openai-compat":
            default:            return new OpenAiCompatProvider("openai-compat", profile.baseUrl);
        }
    }

    private String readScIdFromActivity() {
        try {
            // DesignActivity.sc_id is a public static field.
            String scId = com.besome.sketch.design.DesignActivity.sc_id;
            return scId == null || scId.isEmpty() ? "0" : scId;
        } catch (Throwable t) {
            return "0";
        }
    }

    private String readJavaNameFromActivity() {
        // For MVP, return "main" - the actual current layout file.
        // The real implementation would query DesignActivity for the active ProjectFileBean.
        return "main";
    }

    /**
     * Read the user-configured "max requests per run" from the Auto-Approve
     * preferences. Falls back to {@link AutoApproveFragment#DEFAULT_MAX_ITERATIONS}
     * if the preference is unset or the preference file is inaccessible.
     */
    private int readMaxIterations() {
        try {
            android.content.Context ctx = getContext();
            if (ctx == null) return AutoApproveFragment.DEFAULT_MAX_ITERATIONS;
            android.content.SharedPreferences prefs = ctx.getApplicationContext()
                    .getSharedPreferences(AutoApproveFragment.PREFS_NAME, android.content.Context.MODE_PRIVATE);
            return prefs.getInt(AutoApproveFragment.KEY_MAX_ITERATIONS, AutoApproveFragment.DEFAULT_MAX_ITERATIONS);
        } catch (Throwable t) {
            return AutoApproveFragment.DEFAULT_MAX_ITERATIONS;
        }
    }

    /**
     * Read the YOLO-mode master switch from the Auto-Approve preferences.
     * When enabled, the agent auto-approves every tool call (bypassing the
     * permission gate entirely).
     */
    private boolean isYoloEnabled() {
        try {
            android.content.Context ctx = getContext();
            if (ctx == null) return false;
            android.content.SharedPreferences prefs = ctx.getApplicationContext()
                    .getSharedPreferences(AutoApproveFragment.PREFS_NAME, android.content.Context.MODE_PRIVATE);
            return prefs.getBoolean(AutoApproveFragment.KEY_YOLO, false);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Compute a signature string covering the profile fields that influence
     * agent construction. Used by {@link #onResume()} to detect whether the
     * user changed any setting that requires rebuilding the agent.
     */
    private static String profileSignature(ProviderConfigStore.Profile p) {
        if (p == null) return "";
        return p.providerId + "|" + p.baseUrl + "|" + p.apiKey + "|" + p.modelId
                + "|" + p.enableReasoning + "|" + p.reasoningEffort
                + "|" + p.maxOutputTokens + "|" + p.contextWindowSize
                + "|" + p.enableStreaming + "|" + p.imageSupport
                + "|" + p.promptCaching;
    }
}
