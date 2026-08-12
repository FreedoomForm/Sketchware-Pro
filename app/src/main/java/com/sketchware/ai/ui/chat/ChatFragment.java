package com.sketchware.ai.ui.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import pro.sketchware.R;
import com.sketchware.ai.agent.AgentListener;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.agent.AgentRuntime;
import com.sketchware.ai.context.ContextMentionParser;
import com.sketchware.ai.context.TaskHistoryStore;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.UsageTracker;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public final class ChatFragment extends Fragment {

    private RecyclerView recycler;
    private TextInputEditText input;
    private com.google.android.material.button.MaterialButton btnSend;
    private com.google.android.material.button.MaterialButton btnStop;
    private com.google.android.material.button.MaterialButton btnAttach;
    private com.google.android.material.button.MaterialButton btnMode;
    private com.google.android.material.materialswitch.MaterialSwitch autoApproveToggle;
    private com.google.android.material.progressindicator.LinearProgressIndicator contextProgress;
    private View statusDot;
    private android.widget.TextView statusText;
    private android.widget.TextView tokensText;
    private android.widget.TextView tokensPercent;
    private android.widget.TextView modeLabel;
    private android.widget.TextView chatSubtitle;
    private android.widget.ImageView chatModelIcon;
    private android.widget.View runStatusRow;
    private android.widget.TextView runStatusText;
    private com.sketchware.ai.ui.chat.TypingDotsView runStatusDots;
    private android.view.View btnModelSelector;
    private android.widget.ImageView btnModelSelectorIcon;
    private android.widget.TextView btnModelSelectorLabel;
    private android.widget.ProgressBar contextProgressBar;

    private ChatAdapter adapter;
    private final MessageReducer reducer = new MessageReducer();
    private AgentRuntime agent;
    private ToolRegistry toolRegistry;
    private ToolPermissionGate permissionGate;
    private ProviderConfigStore.Profile profile;

    /**
     * Lazily-initialized task history store. Persists completed conversations
     * so the user can resume or branch them later. Mirrors Cline's
     * {@code HistoryItem} + task-history controller.
     */
    private TaskHistoryStore taskHistoryStore;

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
     * Tracks whether the in-app update check has already been performed
     * during this fragment instance. Set in {@link #onResume()} on the
     * first call; subsequent resumes skip the check (the user can still
     * trigger a manual refresh from the Versions screen).
     */
    private boolean updateCheckDone = false;

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
        btnMode = root.findViewById(R.id.btn_mode);
        autoApproveToggle = root.findViewById(R.id.auto_approve_toggle);
        contextProgress = root.findViewById(R.id.context_progress);
        statusDot = root.findViewById(R.id.status_dot);
        statusText = root.findViewById(R.id.status_text);
        tokensText = root.findViewById(R.id.tokens_text);
        tokensPercent = root.findViewById(R.id.tokens_percent);
        modeLabel = root.findViewById(R.id.mode_label);

        // New UI elements (enriched layout).
        chatSubtitle = root.findViewById(R.id.chat_subtitle);
        chatModelIcon = root.findViewById(R.id.chat_model_icon);
        runStatusRow = root.findViewById(R.id.run_status_row);
        runStatusText = root.findViewById(R.id.run_status_text);
        runStatusDots = root.findViewById(R.id.run_status_dots);
        btnModelSelector = root.findViewById(R.id.btn_model_selector);
        btnModelSelectorIcon = root.findViewById(R.id.btn_model_selector_icon);
        btnModelSelectorLabel = root.findViewById(R.id.btn_model_selector_label);
        contextProgressBar = root.findViewById(R.id.context_progress_bar);

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

        // The old toolbar-based menu has been replaced by individual icon
        // buttons in the new chat header. Wire each one to its action.
        View btnChatMenu = root.findViewById(R.id.btn_chat_menu);
        View btnChatSettings = root.findViewById(R.id.btn_chat_settings);
        View btnChatClear = root.findViewById(R.id.btn_chat_clear);
        if (btnChatMenu != null) {
            // "Menu" — for now, opens settings (no drawer in chat itself).
            btnChatMenu.setOnClickListener(v ->
                    startActivity(AISettingsActivity.newIntent(requireContext(), AISettingsActivity.FRAGMENT_PROVIDER)));
        }
        if (btnChatSettings != null) {
            btnChatSettings.setOnClickListener(v ->
                    startActivity(AISettingsActivity.newIntent(requireContext(), AISettingsActivity.FRAGMENT_PROVIDER)));
        }
        if (btnChatClear != null) {
            btnChatClear.setOnClickListener(v -> clearConversation());
        }

        // Model selector chip in the input bar — opens the model picker
        // dialog so the user can switch models without leaving the chat.
        if (btnModelSelector != null) {
            btnModelSelector.setOnClickListener(v -> showModelPicker());
        }

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

        // Mode toggle (Act/Plan) — cycles between ACT and PLAN. Updates the
        // button label + mode chip, and notifies the agent if running.
        java.util.concurrent.atomic.AtomicReference<AgentMode> currentMode =
                new java.util.concurrent.atomic.AtomicReference<>(AgentMode.ACT);
        updateModeUi(currentMode.get());
        btnMode.setOnClickListener(v -> {
            AgentMode next = currentMode.get() == AgentMode.ACT
                    ? AgentMode.PLAN : AgentMode.ACT;
            currentMode.set(next);
            updateModeUi(next);
            if (agent != null) agent.setMode(next);
        });

        // Auto-approve (YOLO) toggle — reads/writes the AutoApproveFragment
        // shared preference so it stays in sync with the settings page.
        android.content.SharedPreferences aaPrefs = requireContext()
                .getSharedPreferences(AutoApproveFragment.PREFS_NAME, Context.MODE_PRIVATE);
        autoApproveToggle.setChecked(aaPrefs.getBoolean(AutoApproveFragment.KEY_YOLO, false));
        autoApproveToggle.setOnCheckedChangeListener((b, checked) -> {
            aaPrefs.edit().putBoolean(AutoApproveFragment.KEY_YOLO, checked).apply();
            // Update the AutoApprover used by the agent immediately so the
            // change takes effect on the next tool call without a restart.
            if (agent != null) {
                agent.setAutoApprover(buildAutoApprover(checked));
            }
            if (statusText != null) {
                statusText.setText(checked ? "Auto-approve ON" : "Idle");
            }
        });

        adapter.submitList(reducer.getMessages());
        return root;
    }

    /** Update the mode button label + chip text based on AgentMode. */
    private void updateModeUi(AgentMode mode) {
        if (btnMode == null || modeLabel == null) return;
        String label = mode == AgentMode.PLAN ? "Plan" : "Act";
        btnMode.setText(label);
        modeLabel.setText(mode.name());
    }

    /**
     * Build an AutoApprover matching the YOLO toggle state. When yolo=true,
     * returns an AutoApprover in YOLO mode; otherwise returns one in ACT mode
     * with the default rule set.
     */
    private com.sketchware.ai.tools.AutoApprover buildAutoApprover(boolean yolo) {
        com.sketchware.ai.tools.AutoApprover aa = com.sketchware.ai.tools.AutoApprover.withDefaults();
        aa.setMode(yolo ? AgentMode.YOLO : AgentMode.ACT);
        return aa;
    }

    /** Update the status dot + text. Called from agent listener callbacks. */
    private void setStatus(String text, boolean active) {
        if (statusText != null) statusText.setText(text);
        if (statusDot != null) {
            statusDot.setBackgroundColor(active
                    ? androidx.core.content.ContextCompat.getColor(
                        requireContext(), com.google.android.material.R.color.design_default_color_primary)
                    : androidx.core.content.ContextCompat.getColor(
                        requireContext(), com.google.android.material.R.color.design_default_color_secondary));
        }
    }

    /**
     * Refresh the chat header subtitle ("Provider • Model") and the small
     * provider icon to the left of the subtitle. Called from {@link #onResume}
     * and after every profile change so the user always sees which model
     * they're talking to.
     */
    private void refreshChatHeader() {
        if (profile == null) return;
        String providerId = profile.providerId == null ? "" : profile.providerId;
        String providerLabel = com.sketchware.ai.llm.ProviderCatalog.safeDisplayName(providerId);
        String model = profile.modelId == null ? "" : profile.modelId;
        String subtitle = model.isEmpty()
                ? providerLabel
                : providerLabel + " • " + model;
        if (chatSubtitle != null) chatSubtitle.setText(subtitle);
        if (chatModelIcon != null) {
            int iconRes = com.sketchware.ai.ui.settings.ProviderIconResolver
                    .resolveProvider(providerId, providerLabel);
            chatModelIcon.setImageResource(iconRes);
            chatModelIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Refresh the model-selector chip in the input bar so it shows the
     * currently-active model id and the right provider icon.
     */
    private void refreshModelSelectorChip() {
        if (profile == null) return;
        String providerId = profile.providerId == null ? "" : profile.providerId;
        String model = profile.modelId == null ? "" : profile.modelId;
        if (model.isEmpty()) model = "Pick model";
        if (btnModelSelectorLabel != null) btnModelSelectorLabel.setText(model);
        if (btnModelSelectorIcon != null) {
            int iconRes = com.sketchware.ai.ui.settings.ProviderIconResolver
                    .resolveProvider(providerId, com.sketchware.ai.llm.ProviderCatalog.safeDisplayName(providerId));
            btnModelSelectorIcon.setImageResource(iconRes);
        }
    }

    /**
     * Show a dialog letting the user switch models for the active provider.
     * The list combines built-in catalog models + any user-saved custom
     * models for that provider. Picking one updates the active profile,
     * saves it, and refreshes the chat header / chip.
     */
    private void showModelPicker() {
        if (profile == null) return;
        String providerId = profile.providerId == null ? "" : profile.providerId;
        com.sketchware.ai.llm.ProviderCatalog.Entry entry =
                com.sketchware.ai.llm.ProviderCatalog.getOrDefault(providerId);

        java.util.List<String> models = new java.util.ArrayList<>(entry.builtinModels);
        // Append any user-saved custom models for this provider.
        try {
            String raw = requireContext()
                    .getSharedPreferences("ai_custom_models", Context.MODE_PRIVATE)
                    .getString("models_" + providerId, "");
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split("\n")) {
                    String t = s.trim();
                    if (!t.isEmpty() && !models.contains(t)) models.add(t);
                }
            }
        } catch (Throwable ignored) { }

        if (models.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(btnSend,
                    getString(R.string.ai_model_sheet_no_models),
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            // Take the user to the provider detail so they can add one.
            com.sketchware.ai.ui.settings.ProviderDetailActivity.start(
                    requireContext(), providerId, entry.displayName);
            return;
        }

        String[] arr = models.toArray(new String[0]);
        int currentIdx = -1;
        String current = profile.modelId == null ? "" : profile.modelId;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(current)) { currentIdx = i; break; }
        }
        final String[] selected = new String[]{currentIdx >= 0 ? arr[currentIdx] : ""};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_model_sheet_title)
                .setSingleChoiceItems(arr, currentIdx, (d, w) -> selected[0] = arr[w])
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (selected[0] == null || selected[0].isEmpty()) return;
                    profile.modelId = selected[0];
                    // Persist the change so it survives a fragment recreate.
                    try {
                        ProviderConfigStore s = new ProviderConfigStore(requireContext());
                        s.upsertProfile(profile);
                        s.setActiveProfile(profile.id);
                    } catch (Throwable ignored) { }
                    // Force agent rebuild on next send so it uses the new model.
                    if (agent != null) {
                        agent.abort();
                        agent = null;
                    }
                    lastProfileSignature = ""; // force refresh
                    refreshChatHeader();
                    refreshModelSelectorChip();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Show or hide the running-status row (typing dots + label) above the
     * input box. Called from agent listener callbacks when a turn starts or
     * ends.
     */
    private void setRunStatusVisible(boolean visible, String label) {
        if (runStatusRow == null) return;
        runStatusRow.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible && runStatusText != null) {
            runStatusText.setText(label == null ? "" : label);
        }
    }

    /**
     * Update the token-meter progress bar (top of chat) and the percentage
     * label. Uses the active profile's context window as the denominator;
     * if the profile's contextWindowSize is 0 (auto), falls back to the
     * provider's catalog default context window (best effort — actual model
     * context may differ).
     */
    private void updateTokenMeter(int totalTokens) {
        if (profile == null) return;
        int window = profile.contextWindowSize > 0
                ? profile.contextWindowSize
                : 128_000; // safe default; most modern models are 128k+.
        int pct = window > 0 ? (int) Math.min(100L, (100L * totalTokens) / window) : 0;
        if (contextProgressBar != null) contextProgressBar.setProgress(pct);
        if (tokensPercent != null) tokensPercent.setText(pct + "%");
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
        // Refresh the chat header subtitle & model selector chip to reflect
        // the current profile. This runs every resume — including the first
        // one — so the user always sees which model they're talking to.
        refreshChatHeader();
        refreshModelSelectorChip();
        if (adapter != null && profile != null) {
            adapter.setProviderId(profile.providerId == null ? "" : profile.providerId);
        }
        // One-shot in-app update check — fires on the first resume after
        // the fragment is created. If a newer GitHub Release exists, the
        // UpdateDialog is shown. We deliberately do NOT auto-download —
        // the user taps "Download" in the dialog to open the APK URL in
        // the browser, which is the standard sideload flow on Android.
        if (!updateCheckDone) {
            updateCheckDone = true;
            com.sketchware.ai.release.UpdateChecker.checkAsync(
                    requireContext().getApplicationContext(),
                    new com.sketchware.ai.release.UpdateChecker.Callback() {
                        @Override public void onUpdateAvailable(com.sketchware.ai.release.GitHubRelease latest) {
                            if (getActivity() == null || !isAdded()) return;
                            com.sketchware.ai.release.UpdateDialog dialog =
                                    com.sketchware.ai.release.UpdateDialog.newInstance(
                                            latest, requireContext().getApplicationContext());
                            try {
                                dialog.show(getChildFragmentManager(), "update_dialog");
                            } catch (IllegalStateException ignored) {
                                // onSaveInstanceState already called — skip this round.
                            }
                        }
                        @Override public void onUpToDate() { /* silent */ }
                        @Override public void onError(Exception error) { /* silent */ }
                    });
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

    /**
     * Export the current conversation to a text file and share it via
     * Android's share-sheet. The user can then send the transcript (e.g.
     * via Telegram, email, or upload to GitHub issue) so a developer can
     * see the full conversation history including errors, tool calls, and
     * tool results.
     *
     * <p>If the conversation is empty, shows a Snackbar and aborts.
     * If the file write fails (e.g. storage full), shows the error.
     */
    private void exportConversation() {
        View v = getView();
        if (v == null) return;
        List<ChatMessage> messages = reducer.getMessages();
        if (messages == null || messages.isEmpty()) {
            Snackbar.make(v, "Nothing to export — conversation is empty", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            java.io.File file = ChatExporter.writeToCacheFile(requireContext(), messages);
            Intent share = ChatExporter.createShareIntent(requireContext(), file);
            startActivity(share);
        } catch (Exception e) {
            Snackbar.make(v, "Export failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
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

        // Intercept slash commands BEFORE sending to the LLM. Commands like
        // /clear, /help, /mode, /cost, /tools are handled locally; the LLM
        // never sees them. If the command returns a "consumed" result, we
        // clear the input and return without invoking the agent.
        if (text.startsWith("/")) {
            SlashCommandProcessor.ParsedWithRemaining parsed =
                    SlashCommandProcessor.parseWithRemaining(text);
            if (parsed != null) {
                boolean consumed = handleSlashCommand(parsed.command, parsed.remaining);
                if (consumed) {
                    input.setText("");
                    return;
                }
                // If not consumed (e.g. unknown command), fall through and
                // send the raw text to the LLM — the LLM may know how to
                // interpret it.
            }
        }

        // Expand @-mentions in the user's text. Mentions like @file:path or
        // @layout:name are replaced with their expanded content before the
        // message reaches the LLM. If expansion fails (e.g. file not found),
        // the original mention text is preserved so the LLM can ask for
        // clarification.
        String expandedText = expandMentions(text);

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

        // Append user message. Show the ORIGINAL text (with @mentions intact)
        // in the UI — the expanded version is sent to the LLM but the user
        // should see what they typed, not the inlined file content.
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
            } else if (btnMode != null && "Plan".equals(btnMode.getText().toString())) {
                initialMode = AgentMode.PLAN;
            }
            agent.setMode(initialMode);
            updateModeUi(initialMode);
        }

        // Always refresh the tool context: the user may have navigated to a
        // different project (different sc_id) since the last send, and the
        // agent's stored context would then point at the wrong project.
        // This is cheap (just a volatile field write) so it's safe to call
        // on every send.
        String scId = readScIdFromActivity();
        String javaName = readJavaNameFromActivity();
        // Wire the SketchwareToolContext refresh callbacks to DesignActivity's
        // real editor refresh methods. Previously these were no-op lambdas,
        // which meant every tool call reported success but the editor canvas
        // never updated — the user saw no visible change and would say
        // "кнопка не видна" even though the widget WAS added to disk.
        //
        // The view refresh callback now receives the layout name the AI just
        // modified, so DesignActivity can SWITCH the editor to that layout if
        // the user is viewing a different one. This is the fix for
        // "в окне view не видно то что он сделал": the AI created 'calculator'
        // and added widgets to it, but the editor was still showing 'main',
        // so the user saw nothing change.
        android.app.Activity hostActivity = requireActivity();
        java.util.function.Consumer<String> viewRefresh = (xmlName) -> {
            if (hostActivity instanceof com.besome.sketch.design.DesignActivity) {
                ((com.besome.sketch.design.DesignActivity) hostActivity).refreshViewForAi(xmlName);
            }
        };
        Runnable logicRefresh = () -> {
            if (hostActivity instanceof com.besome.sketch.design.DesignActivity) {
                ((com.besome.sketch.design.DesignActivity) hostActivity).refreshLogicForAi();
            }
        };
        Runnable eventRefresh = () -> {
            if (hostActivity instanceof com.besome.sketch.design.DesignActivity) {
                ((com.besome.sketch.design.DesignActivity) hostActivity).refreshEventsForAi();
            }
        };
        Runnable componentRefresh = () -> {
            if (hostActivity instanceof com.besome.sketch.design.DesignActivity) {
                ((com.besome.sketch.design.DesignActivity) hostActivity).refreshComponentsForAi();
            }
        };
        SketchwareToolContext toolCtx = new SketchwareToolContext(
                requireActivity(), scId, javaName, permissionGate,
                viewRefresh, logicRefresh, eventRefresh, componentRefresh);
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
            @Override public void onToolStart(String toolCallId, String toolName, String argsJson) {
                runOnUiIfAlive(() -> {
                    setStatus("Running " + toolName, true);
                    setRunStatusVisible(true, "Running " + toolName);
                });
            }
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
                    if (tokensText != null) {
                        tokensText.setText(inT + " in · " + outT + " out");
                    }
                    // Update the token-meter progress bar based on context
                    // window fill. We compare total tokens seen so far
                    // against the active profile's context window (or the
                    // provider's default if the profile has it set to 0).
                    updateTokenMeter(inT + outT);
                });
            }
            @Override public void onComplete(String finalText) {
                // Reset isRunning on the background thread so the flag is
                // cleared even if the fragment view is gone (runOnUiIfAlive
                // would otherwise skip the reset inside finishRun).
                isRunning = false;
                // Auto-save the completed conversation to task history.
                // Best-effort: failures are silent.
                autoSaveTask();
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
                    setStatus("Complete", false);
                    setRunStatusVisible(false, null);
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
                    setStatus("Stopped", false);
                    setRunStatusVisible(false, null);
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
                    setStatus("Error", false);
                    setRunStatusVisible(false, null);
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
            // Send the EXPANDED text (with @mentions inlined) to the LLM.
            // The user sees the original text in the UI (added via
            // reducer.addUserMessage(text) above), but the LLM needs the
            // expanded version so it can see the file contents, layout trees,
            // etc. that the user referenced.
            agent.execute(expandedText, listener);
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
            // 5-minute timeout: prevents the agent from being parked forever
            // if the user backgrounds the app or never taps Approve/Deny.
            // On timeout we treat as deny and let the agent continue with a
            // denied tool result rather than hanging the whole run. The
            // AbortController.abort() path also interrupts this thread — see
            // AgentRuntime.abort() which calls currentRun.cancel(true) — so
            // the user's Stop button is responsive during an approval prompt.
            if (!latch.await(5, java.util.concurrent.TimeUnit.MINUTES)) {
                return false;
            }
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
        // Resolve base URL via the catalog so every provider — including the
        // six new ones (groq, grok_xai, huggingface, minimax, litellm, vllm,
        // lm_studio) — gets the right well-known URL even if the user left
        // profile.baseUrl empty. Previously the switch below only handled the
        // original 11 providers; anything else fell through to OpenAiCompat
        // with an empty base URL and the request failed with a confusing
        // "no host" error.
        String baseUrl = (profile.baseUrl == null || profile.baseUrl.isEmpty())
                ? com.sketchware.ai.llm.ProviderCatalog.defaultBaseUrlFor(pid)
                : profile.baseUrl;
        switch (pid) {
            case "anthropic":   return new AnthropicProvider();
            case "openai":      return new OpenAiProvider();
            case "gemini":      return new GeminiProvider();
            case "ollama":      return new OllamaProvider();
            case "mistral":     return new OpenAiCompatProvider("mistral", "https://api.mistral.ai/v1");
            case "openrouter":  return new OpenAiCompatProvider("openrouter", "https://openrouter.ai/api/v1");
            case "deepseek":    return new OpenAiCompatProvider("deepseek", "https://api.deepseek.com");
            case "zai":         return new OpenAiCompatProvider("zai", "https://api.z.ai/api/paas/v4");
            case "together":    return new OpenAiCompatProvider("together", "https://api.together.xyz/v1");
            case "fireworks":   return new OpenAiCompatProvider("fireworks", "https://api.fireworks.ai/inference/v1");
            case "groq":        return new OpenAiCompatProvider("groq", "https://api.groq.com/openai/v1");
            case "grok_xai":    return new OpenAiCompatProvider("grok_xai", "https://api.x.ai/v1");
            case "huggingface": return new OpenAiCompatProvider("huggingface", "https://router.huggingface.co/v1");
            case "minimax":     return new OpenAiCompatProvider("minimax", "https://api.minimax.io/v1");
            case "litellm":     return new OpenAiCompatProvider("litellm", baseUrl);
            case "vllm":        return new OpenAiCompatProvider("vllm", baseUrl);
            case "lm_studio":   return new OpenAiCompatProvider("lm_studio", baseUrl);
            case "openai-compat":
            default:            return new OpenAiCompatProvider("openai-compat", baseUrl);
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
        // Query DesignActivity for the actually-displayed layout, so the AI
        // targets the layout the user is looking at, not a hardcoded "main".
        // Previously this always returned "main", which meant if the user
        // had 'calculator' open and asked the AI to "add a button", the AI
        // would add the button to 'main' instead — and then the user would
        // see nothing change in their 'calculator' view.
        try {
            android.app.Activity host = getActivity();
            if (host instanceof com.besome.sketch.design.DesignActivity) {
                com.besome.sketch.beans.ProjectFileBean bean =
                        ((com.besome.sketch.design.DesignActivity) host).getCurrentProjectFile();
                if (bean != null) {
                    String xml = bean.getXmlName();
                    if (xml != null && !xml.isEmpty()) return xml;
                }
            }
        } catch (Throwable t) {
            // fall through
        }
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

    // ------------------------------------------------------------------
    // Slash command handling
    // ------------------------------------------------------------------

    /**
     * Handle a parsed slash command locally. Returns true if the command was
     * consumed (the LLM never sees it); false to fall through and send the
     * raw text to the LLM.
     *
     * <p>Commands handled here:
     * <ul>
     *   <li>{@code /new} {@code /clear} — discard the conversation.</li>
     *   <li>{@code /help} — show command help as a system message.</li>
     *   <li>{@code /mode <act|plan|yolo>} — switch agent mode.</li>
     *   <li>{@code /cost} — show token usage / cost summary.</li>
     *   <li>{@code /tools} — list registered tool names.</li>
     *   <li>{@code /context} — show context window usage estimate.</li>
     *   <li>{@code /maxiter <n>} — set max iterations.</li>
     *   <li>{@code /export} — export conversation.</li>
     *   <li>{@code /undo} — placeholder (not yet implemented).</li>
     *   <li>{@code /compact} — placeholder (not yet implemented).</li>
     * </ul>
     */
    private boolean handleSlashCommand(SlashCommandProcessor.ParsedCommand cmd, String remaining) {
        if (cmd == null) return false;
        View v = getView();
        switch (cmd.name) {
            case "new":
            case "clear":
                clearConversation();
                if (v != null) Snackbar.make(v, "Conversation cleared", Snackbar.LENGTH_SHORT).show();
                return true;
            case "help":
                reducer.addCompletion(SlashCommandProcessor.helpText());
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            case "mode": {
                String modeArg = cmd.arg == null ? "" : cmd.arg.toLowerCase();
                AgentMode newMode;
                switch (modeArg) {
                    case "act":  newMode = AgentMode.ACT; break;
                    case "plan": newMode = AgentMode.PLAN; break;
                    case "yolo": newMode = AgentMode.YOLO; break;
                    default:
                        reducer.addError("Unknown mode: " + cmd.arg + ". Use act, plan, or yolo.");
                        if (adapter != null) adapter.submitList(reducer.getMessages());
                        return true;
                }
                if (agent != null) agent.setMode(newMode);
                // Sync the mode button (except for YOLO which is set via the
                // auto-approve toggle).
                if (btnMode != null && newMode != AgentMode.YOLO) {
                    updateModeUi(newMode);
                }
                reducer.addCompletion("Switched to " + newMode + " mode.");
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            }
            case "cost": {
                showCostSummary();
                return true;
            }
            case "tools": {
                if (toolRegistry == null) return true;
                StringBuilder sb = new StringBuilder("Registered tools (" + toolRegistry.size() + "):\n");
                for (SketchwareToolInterface t : listToolInterfaces()) {
                    sb.append("  ").append(t.name).append(" — ").append(t.category).append("\n");
                }
                reducer.addCompletion(sb.toString());
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            }
            case "context": {
                if (agent == null) {
                    reducer.addError("No active agent. Send a message first.");
                } else {
                    int tokens = estimateContextTokens();
                    reducer.addCompletion("Context window usage:\n  Estimated tokens: " + tokens + "\n  (No model context size available)");
                }
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            }
            case "maxiter": {
                try {
                    int n = Integer.parseInt(cmd.arg);
                    if (agent != null) agent.setMaxIterations(n);
                    reducer.addCompletion("Max iterations set to " + n + ".");
                } catch (NumberFormatException e) {
                    reducer.addError("Invalid number: " + cmd.arg);
                }
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            }
            case "export":
                exportConversation();
                return true;
            case "undo":
                reducer.addError("/undo is not yet implemented.");
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            case "compact":
                reducer.addError("/compact is triggered automatically when the context window overflows. Manual trigger not yet wired.");
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            case "exit":
                Activity a = getActivity();
                if (a != null) a.onBackPressed();
                return true;
            case "model":
                reducer.addCompletion("Active model: " + (profile == null ? "?" : profile.modelId)
                        + "\n(To change the model, open AI Settings.)");
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            case "approve":
                reducer.addCompletion("Per-tool auto-approval can be configured in AI Settings → Auto-Approve.");
                if (adapter != null) adapter.submitList(reducer.getMessages());
                return true;
            default:
                return false;
        }
    }

    /** Lightweight wrapper for displaying tool info in /tools. */
    private static final class SketchwareToolInterface {
        final String name;
        final String category;
        SketchwareToolInterface(String name, String category) {
            this.name = name;
            this.category = category;
        }
    }

    private java.util.List<SketchwareToolInterface> listToolInterfaces() {
        java.util.List<SketchwareToolInterface> out = new java.util.ArrayList<>();
        if (toolRegistry == null) return out;
        for (com.sketchware.ai.tools.SketchwareTool t : toolRegistry.all()) {
            out.add(new SketchwareToolInterface(t.name(), t.category()));
        }
        return out;
    }

    private int estimateContextTokens() {
        if (agent == null) return 0;
        int tokens = 0;
        for (AgentMessage m : agent.getConversationHistory()) {
            tokens += m.estimateTokens();
        }
        return tokens;
    }

    // ------------------------------------------------------------------
    // Context mention expansion
    // ------------------------------------------------------------------

    /**
     * Expand {@code @}-mentions in the user's input text. Each mention is
     * replaced with its expanded content (file contents, layout tree, etc.).
     * If a mention cannot be resolved, the original text is preserved.
     */
    private String expandMentions(String input) {
        if (input == null || input.isEmpty()) return input;
        return ContextMentionParser.expand(input, this::expandMention);
    }

    /**
     * Expand a single mention to its inline text.
     */
    private String expandMention(ContextMentionParser.Mention mention) {
        if (mention == null) return null;
        try {
            switch (mention.type) {
                case FILE:
                    return expandFileMention(mention.value);
                case URL:
                    // Don't auto-fetch URLs — let the LLM decide whether to
                    // use web_fetch. We just mark it as a URL reference.
                    return "[URL: " + mention.value + " — use web_fetch to retrieve]";
                case PROBLEMS:
                    return "[Build problems: run a build to populate this]";
                case GIT_CHANGES:
                    return "[Git changes: not available on Sketchware-Pro]";
                case PROJECT:
                    return "[Project: " + readScIdFromActivity() + "]";
                case LAYOUT:
                    return "[Layout: " + (mention.value == null ? "?" : mention.value)
                            + " — use view_list_widgets to inspect]";
                case COMPONENT:
                    return "[Component: " + (mention.value == null ? "?" : mention.value) + "]";
                case IMAGE:
                    return "[Image: " + (mention.value == null ? "?" : mention.value) + " — attach separately]";
                default:
                    return null;
            }
        } catch (Throwable t) {
            // On any error, preserve the original mention.
            return null;
        }
    }

    /** Inline a file's content (truncated to 4000 chars). */
    private String expandFileMention(String path) {
        if (path == null || path.isEmpty()) return null;
        String scId = readScIdFromActivity();
        String[] candidates = {
            "/sdcard/.sketchware/data/" + scId + "/" + path,
            "/storage/emulated/0/.sketchware/data/" + scId + "/" + path,
            path  // treat as absolute
        };
        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists() && f.isFile()) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    if (content.length() > 4000) {
                        content = content.substring(0, 4000) + "\n... (truncated, " + content.length() + " chars total)";
                    }
                    return "--- File: " + path + " ---\n" + content + "\n--- End of " + path + " ---";
                } catch (Throwable ignored) {
                    // Fall through to next candidate.
                }
            }
        }
        return "[File not found: " + path + "]";
    }

    // ------------------------------------------------------------------
    // Task history UI
    // ------------------------------------------------------------------

    /**
     * Show a dialog listing past saved tasks. Tapping a task loads its
     * conversation into the current agent; long-pressing deletes it.
     */
    private void showTaskHistory() {
        Activity a = getActivity();
        if (a == null) return;
        TaskHistoryStore store = getTaskHistoryStore();
        java.util.List<TaskHistoryStore.TaskMetadata> tasks = store.list();
        if (tasks.isEmpty()) {
            new AlertDialog.Builder(a)
                    .setTitle("Task history")
                    .setMessage("No saved tasks yet.\n\nTasks are saved automatically when a conversation completes successfully.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] items = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            TaskHistoryStore.TaskMetadata t = tasks.get(i);
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(t.updatedAt));
            String title = t.firstUserMessage == null ? "(no title)" : t.firstUserMessage;
            if (title.length() > 60) title = title.substring(0, 60) + "...";
            items[i] = date + "  " + title + "  (" + t.messageCount + " msgs)";
        }
        new AlertDialog.Builder(a)
                .setTitle("Task history (" + tasks.size() + ")")
                .setItems(items, (dlg, idx) -> {
                    TaskHistoryStore.TaskMetadata t = tasks.get(idx);
                    loadTask(t.id);
                })
                .setNeutralButton("Delete all", (dlg, w) -> {
                    new AlertDialog.Builder(a)
                            .setTitle("Delete all tasks?")
                            .setMessage("This will permanently delete all " + tasks.size() + " saved tasks.")
                            .setPositiveButton("Delete", (d2, w2) -> {
                                for (TaskHistoryStore.TaskMetadata t : tasks) store.delete(t.id);
                                Snackbar.make(getView(), "All tasks deleted", Snackbar.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Load a saved task's conversation into the current agent. Replaces the
     * current conversation history (after confirming with the user).
     */
    private void loadTask(String taskId) {
        Activity a = getActivity();
        if (a == null) return;
        TaskHistoryStore store = getTaskHistoryStore();
        try {
            java.util.LinkedList<AgentMessage> conv = store.load(taskId);
            if (conv == null || conv.isEmpty()) {
                Snackbar.make(getView(), "Task not found or empty", Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Confirm overwrite of current conversation.
            if (!reducer.getMessages().isEmpty()) {
                new AlertDialog.Builder(a)
                        .setTitle("Load task?")
                        .setMessage("This will replace the current conversation. Continue?")
                        .setPositiveButton("Load", (d, w) -> doLoadTask(store, taskId, conv))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                doLoadTask(store, taskId, conv);
            }
        } catch (Throwable t) {
            Snackbar.make(getView(), "Load failed: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void doLoadTask(TaskHistoryStore store, String taskId, java.util.LinkedList<AgentMessage> conv) {
        // Rebuild the agent if needed, then restore the conversation history.
        if (agent == null) {
            // Force rebuild on next send() by clearing the agent.
            agent = null;
        } else {
            agent.abort();
            agent.setConversationHistory(conv);
        }
        // Rebuild the UI reducer from the loaded conversation.
        reducer.reset();
        for (AgentMessage m : conv) {
            if (AgentMessage.ROLE_USER.equals(m.role)) {
                if (m.hasToolResults()) {
                    for (AgentMessage.ToolResultContent r : m.toolResults) {
                        reducer.addToolResult(r.toolName, r.output, r.isError);
                    }
                } else {
                    reducer.addUserMessage(m.text == null ? "" : m.text);
                }
            } else if (AgentMessage.ROLE_ASSISTANT.equals(m.role)) {
                if (m.hasToolCalls()) {
                    for (AgentMessage.ToolCall c : m.toolCalls) {
                        reducer.addToolCall(c.name, c.argumentsJson);
                    }
                }
                if (m.text != null && !m.text.isEmpty()) {
                    reducer.addCompletion(m.text);
                }
            }
        }
        if (adapter != null) adapter.submitList(reducer.getMessages());
        View v = getView();
        if (v != null) Snackbar.make(v, "Loaded task (" + conv.size() + " messages)", Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Auto-save the current conversation to task history. Called from the
     * onComplete listener.
     */
    private void autoSaveTask() {
        if (agent == null) return;
        try {
            java.util.LinkedList<AgentMessage> conv = agent.getConversationHistory();
            if (conv.size() < 2) return;  // nothing to save
            TaskHistoryStore store = getTaskHistoryStore();
            String scId = readScIdFromActivity();
            store.save(conv, scId, "Sketchware Project");
        } catch (Throwable ignored) {
            // Auto-save failures should be silent.
        }
    }

    private TaskHistoryStore getTaskHistoryStore() {
        if (taskHistoryStore == null) {
            android.content.Context ctx = getContext();
            if (ctx == null) {
                // Fallback — should not happen since this is called from a live fragment.
                taskHistoryStore = new TaskHistoryStore(new File("/tmp"));
            } else {
                taskHistoryStore = new TaskHistoryStore(ctx.getFilesDir());
            }
        }
        return taskHistoryStore;
    }

    // ------------------------------------------------------------------
    // Cost / usage display
    // ------------------------------------------------------------------

    /**
     * Show a dialog with the current session's token usage and cost breakdown.
     * Reads from {@link AgentRuntime#getUsageTracker()}.
     */
    private void showCostSummary() {
        Activity a = getActivity();
        if (a == null) return;
        if (agent == null) {
            new AlertDialog.Builder(a)
                    .setTitle("Token usage")
                    .setMessage("No active session. Send a message first to start tracking usage.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        UsageTracker.Snapshot snap = agent.getUsageTracker().snapshot();
        new AlertDialog.Builder(a)
                .setTitle("Token usage & cost")
                .setMessage(snap.summary())
                .setPositiveButton("OK", null)
                .show();
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
