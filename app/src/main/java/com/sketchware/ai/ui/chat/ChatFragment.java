package com.sketchware.ai.ui.chat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import pro.sketchware.R;
import com.sketchware.ai.agent.AgentListener;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.ui.settings.ProviderIconResolver;
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
import com.sketchware.ai.ui.chat.adapter.ChatThreadsAdapter;
import com.sketchware.ai.ui.chat.sheet.AiModelPickerSheet;
import com.sketchware.ai.ui.chat.sheet.AiToolCatalogSheet;
import com.sketchware.ai.ui.chat.sheet.AiToolsBottomSheet;
import com.sketchware.ai.ui.settings.AISettingsActivity;
import com.sketchware.ai.ui.settings.AutoApproveFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;

public final class ChatFragment extends Fragment {

    private RecyclerView recycler;
    private TextInputEditText input;
    private View btnSend;
    private View btnStop;
    private View btnAttach;
    private View btnTools;
    /**
     * Segmented Act/Plan toggle container. Two TextViews inside a pill —
     * tapping one side selects that mode. The container itself holds the
     * id {@code btn_mode} (kept from the previous MaterialSwitch for
     * layout continuity); the two segment labels are found by their own
     * ids {@code mode_segment_act} and {@code mode_segment_plan}.
     */
    private android.view.ViewGroup btnMode;
    private android.widget.TextView modeSegmentAct;
    private android.widget.TextView modeSegmentPlan;
    /**
     * {@code true} when the segmented toggle is in PLAN position,
     * {@code false} when in ACT position. Mirrors what the previous
     * MaterialSwitch's {@code isChecked()} returned.
     */
    private boolean modePlanActive = false;
    /**
     * YOLO mode disables the toggle (Act/Plan distinction is meaningless).
     * Tracked separately so the visuals can be faded without losing state.
     */
    private boolean modeToggleDisabled = false;
    /**
     * Listener invoked when the user taps a segment. Receives
     * {@code true} when PLAN is selected, {@code false} when ACT.
     */
    public interface OnModeToggleListener {
        void onModeToggle(boolean planActive);
    }
    private OnModeToggleListener modeToggleListener;
    private android.widget.TextView modeLabelInline;
    private com.google.android.material.materialswitch.MaterialSwitch autoApproveToggle;
    private com.google.android.material.progressindicator.LinearProgressIndicator contextProgress;
    private View statusDot;
    private android.widget.TextView statusText;
    private android.widget.TextView tokensText;
    private android.widget.TextView tokensPercent;
    private android.widget.TextView modeLabel;
    private android.widget.TextView chatSubtitle;
    private android.widget.ImageView chatModelIcon;
    private android.view.View runStatusRow;
    private android.widget.TextView runStatusText;
    private com.sketchware.ai.ui.chat.TypingDotsView runStatusDots;
    private android.view.View btnModelSelector;
    private android.widget.ImageView btnModelSelectorIcon;
    private android.widget.TextView btnModelSelectorLabel;
    private android.widget.ProgressBar contextProgressBar;
    private androidx.drawerlayout.widget.DrawerLayout chatDrawerRoot;

    // ---- Drawer / threads side panel ----
    private RecyclerView drawerThreadsList;
    private View drawerEmptyState;
    private EditText drawerSearchInput;
    private ChatThreadsAdapter threadsAdapter;

    // ---- Image attachment thumbnails ----
    private HorizontalScrollView thumbnailsScroll;
    private LinearLayout thumbnailsContainer;
    /** Base64-encoded JPEG thumbnails (max 8) attached to the next outgoing message. */
    private final List<String> attachedImages = new ArrayList<>();
    private static final int MAX_ATTACHED_IMAGES = 8;

    // ---- Activity-result launchers for the attach popup ----
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> photosLauncher;
    private ActivityResultLauncher<String[]> uploadLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri pendingCameraUri;

    private ChatAdapter adapter;
    private final MessageReducer reducer = new MessageReducer();
    private AgentRuntime agent;
    private ToolRegistry toolRegistry;
    private ToolPermissionGate permissionGate;
    private ProviderConfigStore.Profile profile;

    /**
     * ID of the currently-loaded task in {@link #taskHistoryStore}, or null
     * if the user is on a fresh conversation that hasn't been saved yet.
     * <p>
     * When the user sends the first message in a new chat, {@link #autoSaveTask()}
     * calls {@code store.save(...)} to create a new task file and stores the
     * returned ID here. Subsequent saves for the same conversation call
     * {@code store.update(currentTaskId, ...)} so all messages land in the
     * same task file instead of spawning a new file per turn.
     * <p>
     * Reset to null by {@link #clearConversation()} (the "new chat" button)
     * so the next run starts a fresh task file again.
     */
    private String currentTaskId;

    /**
     * Lazily-initialized task history store. Persists completed conversations
     * so the user can resume or branch them later. Mirrors Cline's
     * {@code HistoryItem} + task-history controller.
     *
     * <p>Marked {@code volatile} because the field is initialised lazily from
     * both the UI thread (via {@link #onResume()} → {@link #refreshThreads()})
     * and the agent's background thread (via {@link #autoSaveTask()}). Without
     * {@code volatile} the background thread could see a stale {@code null}
     * even after the UI thread had already cached a store, and the resulting
     * second initialisation could land on the {@code /tmp} fallback path
     * (because {@link #getContext()} returns null once the fragment is
     * detached) — producing a store that writes to {@code /tmp} while the
     * drawer's adapter still reads from {@code getFilesDir()}, so new chats
     * silently disappeared. Synchronising {@link #getTaskHistoryStore()} plus
     * this {@code volatile} tag closes that race.
     */
    private volatile TaskHistoryStore taskHistoryStore;

    /**
     * Handler tied to the main Looper, used to schedule deferred UI work from
     * background threads. Used by {@link #autoSaveTask()} to fire a backup
     * {@link #refreshThreads()} 300ms after the agent's {@code onComplete}
     * callback, in case the immediate {@code runOnUiIfAlive(this::refreshThreads)}
     * was a no-op because {@link #getView()} was momentarily null (which
     * happens during fragment recreation).
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Delegates to {@link #refreshThreads()}. Kept as a field so we can call
     * {@link Handler#removeCallbacks(Runnable)} on it — without a stable
     * reference, repeated {@code onComplete} callbacks would pile up
     * duplicate delayed refreshes.
     */
    private final Runnable refreshThreadsRunnable = this::refreshThreads;

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
        // Register activity-result launchers BEFORE onStart (lifecycle
        // requirement for registerForActivityResult).
        registerAttachmentLaunchers();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_chat, container, false);
        recycler = root.findViewById(R.id.recycler);
        input = root.findViewById(R.id.input);
        btnSend = root.findViewById(R.id.btn_send);
        btnStop = root.findViewById(R.id.btn_stop);
        btnAttach = root.findViewById(R.id.btn_attach);
        btnTools = root.findViewById(R.id.btn_tools);
        btnMode = root.findViewById(R.id.btn_mode);
        modeSegmentAct = root.findViewById(R.id.mode_segment_act);
        modeSegmentPlan = root.findViewById(R.id.mode_segment_plan);
        modeLabelInline = root.findViewById(R.id.mode_label_inline);
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
        adapter.setMessageActionListener(new ChatAdapter.MessageActionListener() {
            @Override public void onCopy(ChatMessage message) { copyMessage(message); }
            @Override public void onRetry(ChatMessage message) { retryMessage(message); }
            @Override public void onEdit(ChatMessage message) { editMessage(message); }
            @Override public void onDelete(ChatMessage message) { deleteMessage(message); }
            @Override public void onMore(ChatMessage message, View anchor) { showMessageActions(message, anchor); }
        });
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
        chatDrawerRoot = root.findViewById(R.id.chat_drawer_root);
        // Use a slightly darker scrim (60% black) than the default (40%)
        // so the chat content behind the open drawer is more dimmed and
        // less visually distracting.
        if (chatDrawerRoot != null) {
            chatDrawerRoot.setScrimColor(0x99000000);
        }
        View btnChatMenu = root.findViewById(R.id.btn_chat_menu);
        View btnChatSettings = root.findViewById(R.id.btn_chat_settings);
        View btnChatClear = root.findViewById(R.id.btn_chat_clear);
        if (btnChatMenu != null) {
            // "Menu" — opens the chat threads side drawer.
            btnChatMenu.setOnClickListener(v -> {
                if (chatDrawerRoot != null) {
                    chatDrawerRoot.openDrawer(androidx.core.view.GravityCompat.START);
                    refreshThreads();
                }
            });
        }
        if (btnChatSettings != null) {
            btnChatSettings.setOnClickListener(v ->
                    startActivity(AISettingsActivity.newIntent(requireContext(), AISettingsActivity.FRAGMENT_PROVIDER)));
        }
        if (btnChatClear != null) {
            btnChatClear.setOnClickListener(v -> clearConversation());
        }

        // ---- Drawer: threads list + search + footer buttons ----
        drawerThreadsList = root.findViewById(R.id.drawer_threads_list);
        drawerEmptyState = root.findViewById(R.id.drawer_empty_state);
        drawerSearchInput = root.findViewById(R.id.drawer_search);

        if (drawerThreadsList != null) {
            drawerThreadsList.setLayoutManager(new LinearLayoutManager(getContext()));
            threadsAdapter = new ChatThreadsAdapter(new ChatThreadsAdapter.Callback() {
                @Override
                public void onOpen(TaskHistoryStore.TaskMetadata thread) {
                    if (chatDrawerRoot != null) {
                        chatDrawerRoot.closeDrawer(androidx.core.view.GravityCompat.START);
                    }
                    loadTask(thread.id);
                }

                @Override
                public void onMore(TaskHistoryStore.TaskMetadata thread, View anchor) {
                    showThreadActionsSheet(thread);
                }
            });
            drawerThreadsList.setAdapter(threadsAdapter);
        }
        if (drawerSearchInput != null) {
            drawerSearchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (threadsAdapter != null) threadsAdapter.filter(s == null ? "" : s.toString());
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }

        // Drawer header: "new chat" shortcut button. Starts a fresh
        // conversation and closes the drawer so the user can begin typing
        // immediately. Previously the drawer had two redundant buttons
        // (history = just refreshed the list; settings = duplicated the
        // hammer icon in the chat header) which cluttered the UI.
        View btnDrawerNewChat = root.findViewById(R.id.btn_drawer_new_chat);
        if (btnDrawerNewChat != null) {
            btnDrawerNewChat.setOnClickListener(v -> {
                if (chatDrawerRoot != null) {
                    chatDrawerRoot.closeDrawer(androidx.core.view.GravityCompat.START);
                }
                clearConversation();
            });
        }

        // ---- Image thumbnails strip ----
        thumbnailsScroll = root.findViewById(R.id.thumbnails_scroll);
        thumbnailsContainer = root.findViewById(R.id.thumbnails_container);

        // ---- Attach button popup (Camera / Photos / Upload) ----
        // The launchers themselves were registered in onCreate so they're
        // ready before onStart. Here we just wire the click listener.
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> showAttachSheet());
        }
        if (btnTools != null) {
            btnTools.setOnClickListener(v -> AiToolCatalogSheet.show(requireContext(), toolRegistry));
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

        // Mode toggle (Act/Plan) — now a segmented toggle: two labels
        // "Act" and "Plan" side by side inside a pill track. Tapping a
        // label selects that mode (Act = execute tools, Plan = think only).
        // Updates the top status chip and notifies the agent if running.
        java.util.concurrent.atomic.AtomicReference<AgentMode> currentMode =
                new java.util.concurrent.atomic.AtomicReference<>(AgentMode.ACT);
        updateModeUi(currentMode.get());
        modeToggleListener = planActive -> {
            if (updatingModeUi) return;  // ignore programmatic updates
            AgentMode next = planActive ? AgentMode.PLAN : AgentMode.ACT;
            currentMode.set(next);
            updateModeUi(next);
            if (agent != null) agent.setMode(next);
        };
        if (modeSegmentAct != null) {
            modeSegmentAct.setOnClickListener(v -> {
                if (modeToggleDisabled || !modePlanActive) return;
                modePlanActive = false;
                applyModeSegmentVisuals();
                if (modeToggleListener != null) modeToggleListener.onModeToggle(false);
            });
        }
        if (modeSegmentPlan != null) {
            modeSegmentPlan.setOnClickListener(v -> {
                if (modeToggleDisabled || modePlanActive) return;
                modePlanActive = true;
                applyModeSegmentVisuals();
                if (modeToggleListener != null) modeToggleListener.onModeToggle(true);
            });
        }

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

    /** Guard flag so updateModeUi's programmatic state changes don't re-enter the click listener. */
    private boolean updatingModeUi = false;

    /** Update the segmented toggle + chip text based on AgentMode. */
    private void updateModeUi(AgentMode mode) {
        // The top status-row chip shows the current mode name. The segmented
        // toggle below the input also reflects it (Act segment highlighted
        // in ACT mode, Plan segment highlighted in PLAN mode, both faded
        // and disabled in YOLO mode).
        String label;
        boolean planChecked;
        switch (mode) {
            case RESEARCH: label = "Research"; planChecked = false; break;
            case PLAN:     label = "Plan";     planChecked = true;  break;
            case YOLO:     label = "Yolo";     planChecked = false; break;
            default:       label = "Act";      planChecked = false; break;
        }
        updatingModeUi = true;
        try {
            modePlanActive = planChecked;
            modeToggleDisabled = (mode == AgentMode.YOLO);
            applyModeSegmentVisuals();
        } finally {
            updatingModeUi = false;
        }
        if (modeLabelInline != null) modeLabelInline.setText(label);
        if (modeLabel != null) modeLabel.setText(label);
    }

    /**
     * Refresh the segmented Act/Plan toggle's visuals based on
     * {@link #modePlanActive} and {@link #modeToggleDisabled}.
     *
     * <p>The active segment gets the {@code selected} state (accent
     * background + white text via the {@code ai_mode_segment_item} and
     * {@code ai_mode_segment_text} selectors). When disabled (YOLO mode),
     * both segments get the {@code enabled=false} state so the text color
     * selector fades them to the hint color.
     */
    private void applyModeSegmentVisuals() {
        if (modeSegmentAct != null) {
            modeSegmentAct.setSelected(!modePlanActive);
            modeSegmentAct.setEnabled(!modeToggleDisabled);
        }
        if (modeSegmentPlan != null) {
            modeSegmentPlan.setSelected(modePlanActive);
            modeSegmentPlan.setEnabled(!modeToggleDisabled);
        }
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
     * Show the full-featured model picker bottom sheet (ported from
     * FabioSilva11/Sketchware-IA's {@code KelivoModelBottomSheet}).
     *
     * <p>Lists ALL configured providers and their models in one sheet,
     * with a Favorites section pinned to the top, search filter, and
     * provider-chip quick-jump. The user can switch provider AND model
     * in one tap. On selection, the sheet persists the new (provider,
     * model) to the active profile; this callback then reloads the
     * profile, rebuilds the agent, and refreshes the chat header / chip.
     */
    private void showModelPicker() {
        if (getActivity() == null) return;
        AiModelPickerSheet.show(getActivity(), (providerId, modelId) -> {
            // The sheet already persisted the selection; reload it.
            try {
                ProviderConfigStore s = new ProviderConfigStore(requireContext());
                profile = s.getActiveProfile();
            } catch (Throwable ignored) { }
            // Force agent rebuild on next send so it uses the new model
            // (and possibly the new provider's API client).
            if (agent != null) {
                agent.abort();
                agent = null;
            }
            lastProfileSignature = ""; // force refresh
            refreshChatHeader();
            refreshModelSelectorChip();
        });
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
        // Refresh the past-conversations list in the side drawer so the user
        // always sees the latest tasks after returning from a run.
        refreshThreads();
        // One-shot in-app update check — fires on the first resume after
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
        // Drop the current task ID so the next autoSaveTask() creates a NEW
        // task file instead of overwriting the previous conversation the
        // user just cleared.
        currentTaskId = null;
        reducer.reset();
        if (adapter != null) adapter.submitList(reducer.getMessages());
        if (btnSend != null) btnSend.setEnabled(true);
        if (btnStop != null) btnStop.setVisibility(View.GONE);
        if (btnAttach != null) btnAttach.setVisibility(View.VISIBLE);
        // Drop any pending image attachments — they belong to the cleared
        // conversation, not the next one.
        if (!attachedImages.isEmpty()) {
            attachedImages.clear();
            renderThumbnails();
        }
    }

    private void copyMessage(ChatMessage message) {
        if (message == null || getContext() == null) return;
        try {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            String value = message.text;
            if (value == null || value.isEmpty()) value = message.toolResult;
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("AI message", value == null ? "" : value));
                showActionFeedback(R.string.ai_chat_action_copy_done);
            }
        } catch (Throwable error) {
            showActionFeedback("Copy failed");
        }
    }

    private void retryMessage(ChatMessage message) {
        String prompt = reducer.userPromptFor(message);
        if (prompt == null || prompt.trim().isEmpty()) return;
        if (isRunning) {
            showActionFeedback("Stop the current run first");
            return;
        }
        reducer.removeTurn(message);
        if (adapter != null) adapter.submitList(reducer.getMessages());
        if (agent != null) {
            agent.abort();
            agent = null;
        }
        input.setText(prompt);
        input.setSelection(input.length());
        showActionFeedback(R.string.ai_chat_action_retry_done);
        send();
    }

    private void editMessage(ChatMessage message) {
        String prompt = reducer.userPromptFor(message);
        if (prompt == null) return;
        if (isRunning) {
            showActionFeedback("Stop the current run first");
            return;
        }
        reducer.removeTurn(message);
        if (adapter != null) adapter.submitList(reducer.getMessages());
        if (agent != null) {
            agent.abort();
            agent = null;
        }
        input.setText(prompt);
        input.setSelection(input.length());
        input.requestFocus();
        input.postDelayed(() -> {
            Context context = getContext();
            if (context != null) {
                InputMethodManager imm = (InputMethodManager) context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 120L);
    }

    private void deleteMessage(ChatMessage message) {
        if (message == null || isRunning) {
            if (isRunning) showActionFeedback("Stop the current run first");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_chat_action_delete)
                .setMessage(R.string.ai_chat_action_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ai_chat_action_delete, (dialog, which) -> {
                    if (reducer.removeTurn(message)) {
                        if (agent != null) {
                            agent.abort();
                            agent = null;
                        }
                        if (adapter != null) adapter.submitList(reducer.getMessages());
                        autoSaveTask();
                        showActionFeedback(R.string.ai_chat_action_deleted);
                    }
                })
                .show();
    }

    private void showMessageActions(ChatMessage message, View anchor) {
        if (message == null || anchor == null) return;
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        final int copyId = 1;
        final int retryId = 2;
        final int editId = 3;
        final int deleteId = 4;
        popup.getMenu().add(0, copyId, 0, R.string.ai_chat_action_copy);
        popup.getMenu().add(0, retryId, 1, R.string.ai_chat_action_refresh);
        popup.getMenu().add(0, editId, 2, R.string.ai_chat_action_edit);
        popup.getMenu().add(0, deleteId, 3, R.string.ai_chat_action_delete);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case copyId: copyMessage(message); return true;
                case retryId: retryMessage(message); return true;
                case editId: editMessage(message); return true;
                case deleteId: deleteMessage(message); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void showActionFeedback(String text) {
        View view = getView();
        if (view != null && text != null) Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    private void showActionFeedback(int stringRes) {
        View view = getView();
        if (view != null) Snackbar.make(view, stringRes, Snackbar.LENGTH_SHORT).show();
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
            } else if (modePlanActive) {
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
                // Auto-save the partial conversation so the user can resume
                // the aborted task later from the chat list. Same logic as
                // onComplete — uses currentTaskId to update vs. create.
                autoSaveTask();
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
                // Auto-save even on error so the user's input is preserved in
                // the chat list. Previously only onComplete / onAborted saved,
                // which meant a 401 from the provider (or any other failure
                // before the first assistant chunk landed) silently dropped
                // the conversation — the user's message vanished from the
                // drawer list and they had to retype it.
                autoSaveTask();
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
            //
            // If the user attached any images via the paperclip button, pass
            // them along as base64-encoded JPEGs. The agent's userWithImages
            // factory packages them into the next outgoing message.
            List<String> imagesToSend = attachedImages.isEmpty() ? null : new ArrayList<>(attachedImages);
            agent.execute(expandedText, imagesToSend, listener);
            // Clear the thumbnails strip after the message is dispatched —
            // the images are now part of the conversation.
            if (!attachedImages.isEmpty()) {
                attachedImages.clear();
                renderThumbnails();
            }
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
            // AgentRouter — multi-model aggregator (Claude Opus, GPT-5.5, GLM-5.2, ...).
            // Exposed as OpenAI-compatible at https://agentrouter.org/v1; reasoning
            // is forwarded the same way as OpenRouter (object form {effort, max_tokens}).
            case "agentrouter": return new OpenAiCompatProvider("agentrouter", "https://agentrouter.org/v1");
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
     *   <li>{@code /mode <research|plan|act|yolo>} — switch agent mode.</li>
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
                    case "act":      newMode = AgentMode.ACT; break;
                    case "plan":     newMode = AgentMode.PLAN; break;
                    case "research": newMode = AgentMode.RESEARCH; break;
                    case "yolo":     newMode = AgentMode.YOLO; break;
                    default:
                        reducer.addError("Unknown mode: " + cmd.arg
                                + ". Use research, plan, act, or yolo.");
                        if (adapter != null) adapter.submitList(reducer.getMessages());
                        return true;
                }
                if (agent != null) agent.setMode(newMode);
                // Always sync the mode button + chip so the UI stays
                // consistent with the agent's actual mode (including YOLO).
                updateModeUi(newMode);
                // If the user explicitly switched to/from YOLO via slash
                // command, also sync the auto-approve toggle so it stays
                // in sync with the settings page.
                if (autoApproveToggle != null) {
                    autoApproveToggle.setChecked(newMode == AgentMode.YOLO);
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
                reducer.addCompletion(AiToolCatalogSheet.summary(toolRegistry));
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
        // Track the loaded task ID so subsequent autoSaveTask() calls update
        // this task file instead of creating a new one for every reply.
        currentTaskId = taskId;
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
     * onComplete, onAborted AND onError listeners.
     *
     * <p>Saving on error is intentional — without it, any failed request
     * (provider 401, network drop, tool-internal exception, ...) silently
     * discards the user's input. The chat list never grows a new entry and
     * the user can't retry from the drawer; they have to retype the prompt.
     * Saving the partial conversation lets them open it, fix the cause of
     * the failure (e.g. swap the API key), and resend.
     *
     * <p>If {@link #currentTaskId} is null (fresh conversation), creates a new
     * task file via {@code store.save(...)} and stores the returned ID. If
     * non-null (continuing an existing conversation), updates the existing
     * task file in-place via {@code store.update(currentTaskId, ...)} so all
     * messages land in the same task file instead of spawning a new file per
     * turn — which previously caused the chat list to fill up with one entry
     * per assistant reply.
     *
     * <p>After writing, calls {@link #refreshThreads()} so the drawer list
     * reflects the new/updated entry immediately (previously the user had to
     * close and reopen the drawer to see new chats).
     */
    private void autoSaveTask() {
        if (agent == null) return;
        try {
            java.util.LinkedList<AgentMessage> conv = agent.getConversationHistory();
            // Save as soon as the user has sent at least one message — even if
            // the assistant never replied (e.g. provider returned 401, network
            // dropped, or the user aborted before the first token arrived).
            // Previously we required >= 2 messages (user + assistant), which
            // meant failed conversations never made it to the chat list and
            // the user had no way to retry them from the drawer.
            if (conv == null || conv.isEmpty()) {
                android.util.Log.w("ChatFragment",
                        "autoSaveTask: conversation empty, skipping save.");
                return;
            }
            TaskHistoryStore store = getTaskHistoryStore();
            String scId = readScIdFromActivity();
            // Record which provider/model this chat used so the chat list
            // can show the provider's emblem instead of a generic bot icon.
            String pid = profile != null ? profile.providerId : null;
            String mid = profile != null ? profile.modelId : null;
            String savedTaskId;
            if (currentTaskId == null) {
                savedTaskId = store.save(conv, scId, "Sketchware Project", pid, mid);
                currentTaskId = savedTaskId;
            } else {
                try {
                    store.update(currentTaskId, conv, pid, mid);
                    savedTaskId = currentTaskId;
                } catch (IOException updateFailed) {
                    // The task file may have been deleted from disk (e.g. the
                    // user wiped app data, or removed the chat from the
                    // drawer while the agent was running). Fall back to
                    // creating a new task so the conversation is not lost.
                    savedTaskId = store.save(conv, scId, "Sketchware Project", pid, mid);
                    currentTaskId = savedTaskId;
                }
            }
            android.util.Log.i("ChatFragment",
                    "autoSaveTask: saved task " + savedTaskId
                            + " (" + conv.size() + " msgs) to " + store.getHistoryDir());
            // Refresh the drawer list so the new/updated entry shows up
            // immediately. Without this, the user only saw new chats after
            // closing and reopening the drawer.
            runOnUiIfAlive(this::refreshThreads);
            // Defensive backup refresh: if the immediate refresh was a no-op
            // because getView() was momentarily null (e.g. fragment is in
            // the middle of a recreation cycle while the agent's bg thread
            // is delivering onComplete), schedule another refresh 300ms later
            // so the drawer still picks up the new entry once the view is
            // back. Without this, a chat that completed during a config
            // change would silently fail to appear in the drawer until the
            // user manually reopened it.
            mainHandler.removeCallbacks(refreshThreadsRunnable);
            mainHandler.postDelayed(refreshThreadsRunnable, 300);
        } catch (Throwable t) {
            // Surface save errors so they aren't completely invisible. The
            // previous `catch (Throwable ignored)` here swallowed EVERY
            // failure — disk-full, permission denied, Gson serialization
            // errors, NoSuchFileException from mkdirs() races — and the user
            // just saw "the chat doesn't appear in the list" with zero
            // diagnostic output. Now we log to logcat AND show a snackbar
            // so the user knows something went wrong.
            android.util.Log.e("ChatFragment",
                    "autoSaveTask: FAILED to save conversation", t);
            runOnUiIfAlive(() -> {
                View v = getView();
                if (v == null) return;
                String msg = t.getMessage() == null
                        ? t.getClass().getSimpleName() : t.getMessage();
                Snackbar.make(v, "Chat not saved: " + msg,
                        Snackbar.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Get the lazily-initialised task history store.
     *
     * <p>Synchronised + volatile-tagged field: the store can be initialised
     * from either the UI thread (via {@link #onResume()} → {@link #refreshThreads()})
     * or the agent's background thread (via {@link #autoSaveTask()}). Without
     * synchronisation, two threads could race and end up with two different
     * store instances pointing at different directories — e.g. the UI thread
     * creates a store backed by {@code getFilesDir()/ai_task_history} while
     * the background thread (seeing the field still null) creates a fallback
     * store backed by {@code /tmp}. The agent would then save to {@code /tmp}
     * while the drawer reads from {@code getFilesDir()}, and new chats would
     * silently fail to appear in the list.
     *
     * <p>The {@code /tmp} fallback is now NEVER cached: if {@link #getContext()}
     * returns null (fragment detached), we return a throwaway store for the
     * current call but leave {@link #taskHistoryStore} null, so a later call
     * from an attached fragment can populate it with the proper filesDir path.
     */
    private synchronized TaskHistoryStore getTaskHistoryStore() {
        if (taskHistoryStore != null) return taskHistoryStore;
        android.content.Context ctx = getContext();
        if (ctx == null) {
            // Fragment detached — return a transient store but DON'T cache it.
            // Previously we cached /tmp forever, which meant if the first call
            // happened to land while the fragment was briefly detached, all
            // subsequent saves went to /tmp (which on Android is not a real
            // app-writable dir) and the drawer stayed empty forever.
            android.util.Log.w("ChatFragment",
                    "getTaskHistoryStore: getContext() returned null — returning "
                            + "transient /tmp store, NOT caching.");
            return new TaskHistoryStore(new File("/tmp"));
        }
        taskHistoryStore = new TaskHistoryStore(ctx.getFilesDir());
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

    // ==================================================================
    // Side drawer: past-conversations list
    // ==================================================================

    /**
     * Refresh the threads list shown in the side drawer from
     * {@link TaskHistoryStore}. Runs on a background thread (file IO),
     * then posts the result back to the UI thread.
     */
    private void refreshThreads() {
        final TaskHistoryStore store = getTaskHistoryStore();
        new Thread(() -> {
            try {
                final List<TaskHistoryStore.TaskMetadata> tasks = store.list();
                android.util.Log.i("ChatFragment",
                        "refreshThreads: list() returned " + tasks.size()
                                + " task(s) from " + store.getHistoryDir());
                runOnUiIfAlive(() -> {
                    if (threadsAdapter != null) {
                        threadsAdapter.submitAll(tasks);
                        // Keep the star indicators in sync with the pinned set.
                        android.content.SharedPreferences prefs = requireContext()
                                .getSharedPreferences(PINNED_THREADS_PREFS, Context.MODE_PRIVATE);
                        java.util.Set<String> pinned = prefs.getStringSet(
                                PINNED_THREADS_KEY, java.util.Collections.emptySet());
                        threadsAdapter.setPinnedIds(pinned);
                    } else {
                        android.util.Log.w("ChatFragment",
                                "refreshThreads: threadsAdapter is null — "
                                        + "drawer not yet initialised?");
                    }
                    if (drawerEmptyState != null) {
                        drawerEmptyState.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    if (drawerThreadsList != null) {
                        drawerThreadsList.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
            } catch (Throwable t) {
                // Was `catch (Throwable ignored)` — best-effort, but completely
                // silent. Now we log so a broken filesystem / corrupted JSON
                // doesn't look like "user has no chats".
                android.util.Log.e("ChatFragment",
                        "refreshThreads: list() threw — drawer will show empty", t);
            }
        }, "ai-threads-refresh").start();
    }

    /**
     * Show the actions bottom-sheet for a thread in the drawer.
     * Triggered by long-press or the overflow (three-dots) icon on a thread
     * row. Shows a header (provider icon + title + subtitle) and five
     * full-width action rows: Open / Rename / Export / Pin / Delete.
     *
     * <p>Previously this used an {@link AlertDialog} with {@code setMessage(title)}
     * which on some themes crowded the action list out — the user saw only
     * the chat title in the dialog. Switching to a BottomSheetDialog gives
     * a proper Material bottom sheet with explicit action rows + icons.
     */
    private void showThreadActionsSheet(TaskHistoryStore.TaskMetadata thread) {
        Activity a = getActivity();
        if (a == null) return;
        if (thread == null || thread.id == null) return;

        final View sheet = getLayoutInflater().inflate(
                R.layout.ai_thread_actions_sheet, null);
        final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(a);
        dialog.setContentView(sheet);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        // ---- Header ----
        String title = thread.firstUserMessage == null
                ? getString(R.string.ai_thread_rename_hint)
                : thread.firstUserMessage;
        if (title.length() > 60) title = title.substring(0, 60) + "...";
        TextView titleView = sheet.findViewById(R.id.sheet_thread_title);
        TextView subtitleView = sheet.findViewById(R.id.sheet_thread_subtitle);
        ImageView iconView = sheet.findViewById(R.id.sheet_thread_icon);
        if (titleView != null) titleView.setText(title);
        if (subtitleView != null) {
            StringBuilder sb = new StringBuilder();
            if (thread.projectName != null && !thread.projectName.isEmpty()) {
                sb.append(thread.projectName).append(" · ");
            }
            sb.append(formatRelativeTimestamp(thread.updatedAt));
            sb.append(" · ").append(thread.messageCount).append(" msgs");
            subtitleView.setText(sb.toString());
        }
        if (iconView != null) {
            // Mirror the same provider-icon resolution used by the list row,
            // so the sheet's header icon matches the chat list's icon.
            int iconRes = 0;
            if (thread.lastProviderId != null && !thread.lastProviderId.isEmpty()) {
                iconRes = ProviderIconResolver.resolveProvider(thread.lastProviderId, null);
            }
            if (iconRes == 0 || iconRes == R.drawable.ic_ai) {
                if (thread.lastModelId != null && !thread.lastModelId.isEmpty()) {
                    iconRes = ProviderIconResolver.resolveModel(thread.lastModelId);
                }
            }
            if (iconRes != 0) {
                iconView.setImageResource(iconRes);
                iconView.setImageTintList(null);
            } else {
                iconView.setImageResource(R.drawable.kelivo_lucide_bot_message_square);
                iconView.setImageTintList(androidx.core.content.ContextCompat
                        .getColorStateList(a, R.color.ai_avatar_text));
            }
        }

        // ---- Pin/unpin label + icon swap ----
        boolean pinned = isThreadPinned(thread.id);
        TextView pinLabel = sheet.findViewById(R.id.sheet_pin_label);
        ImageView pinIcon = sheet.findViewById(R.id.sheet_pin_icon);
        if (pinLabel != null) {
            pinLabel.setText(pinned
                    ? getString(R.string.ai_thread_action_unpin)
                    : getString(R.string.ai_thread_action_pin));
        }
        if (pinIcon != null) {
            pinIcon.setImageResource(pinned
                    ? R.drawable.ic_star
                    : R.drawable.ic_mtrl_star);
        }

        // ---- Action handlers ----
        sheet.findViewById(R.id.sheet_action_open).setOnClickListener(v -> {
            dialog.dismiss();
            if (chatDrawerRoot != null) {
                chatDrawerRoot.closeDrawer(androidx.core.view.GravityCompat.START);
            }
            loadTask(thread.id);
        });
        sheet.findViewById(R.id.sheet_action_rename).setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(thread);
        });
        sheet.findViewById(R.id.sheet_action_export).setOnClickListener(v -> {
            dialog.dismiss();
            exportThread(thread);
        });
        sheet.findViewById(R.id.sheet_action_pin).setOnClickListener(v -> {
            dialog.dismiss();
            togglePinThread(thread);
        });
        sheet.findViewById(R.id.sheet_action_delete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteThread(thread);
        });

        dialog.show();
    }

    /** Format a unix-ms timestamp as a short relative string ("5m ago"). */
    private String formatRelativeTimestamp(long ts) {
        long now = System.currentTimeMillis();
        long diff = now - ts;
        if (diff < 60_000L) return "just now";
        if (diff < 3_600_000L) return (diff / 60_000L) + "m ago";
        if (diff < 86_400_000L) return (diff / 3_600_000L) + "h ago";
        if (diff < 7L * 86_400_000L) return (diff / 86_400_000L) + "d ago";
        return android.text.format.DateUtils.formatDateTime(
                requireContext(), ts,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
                        | android.text.format.DateUtils.FORMAT_ABBREV_MONTH
                        | android.text.format.DateUtils.FORMAT_SHOW_DATE);
    }

    // ------------------------------------------------------------------
    // Thread export + pin (star)
    // ------------------------------------------------------------------

    /** SharedPreferences-backed set of pinned (starred) thread ids. */
    private static final String PINNED_THREADS_PREFS = "ai_chat_prefs";
    private static final String PINNED_THREADS_KEY = "pinned_thread_ids";

    private boolean isThreadPinned(String threadId) {
        if (threadId == null) return false;
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences(PINNED_THREADS_PREFS, Context.MODE_PRIVATE);
        return prefs.getStringSet(PINNED_THREADS_KEY, java.util.Collections.emptySet())
                .contains(threadId);
    }

    private void togglePinThread(TaskHistoryStore.TaskMetadata thread) {
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences(PINNED_THREADS_PREFS, Context.MODE_PRIVATE);
        java.util.Set<String> current = new java.util.HashSet<>(
                prefs.getStringSet(PINNED_THREADS_KEY, java.util.Collections.emptySet()));
        boolean wasPinned = !current.add(thread.id);
        if (wasPinned) current.remove(thread.id);
        prefs.edit().putStringSet(PINNED_THREADS_KEY, current).apply();
        View v = getView();
        if (v != null) {
            Snackbar.make(v, wasPinned ? "Unpinned" : "Pinned", Snackbar.LENGTH_SHORT).show();
        }
        refreshThreads();
    }

    /**
     * Export a saved thread (by id) to a text file and share it via the
     * system share-sheet. Loads the conversation from {@link TaskHistoryStore},
     * replays it into a temporary {@link MessageReducer}, then renders the
     * transcript with {@link ChatExporter}.
     */
    private void exportThread(TaskHistoryStore.TaskMetadata thread) {
        Activity a = getActivity();
        if (a == null) return;
        View v = getView();
        new Thread(() -> {
            try {
                TaskHistoryStore store = getTaskHistoryStore();
                java.util.LinkedList<AgentMessage> conv = store.load(thread.id);
                if (conv == null || conv.isEmpty()) {
                    runOnUiIfAlive(() -> {
                        if (v != null) Snackbar.make(v,
                                R.string.ai_thread_export_empty, Snackbar.LENGTH_SHORT).show();
                    });
                    return;
                }
                // Rebuild a MessageReducer from the stored conversation so
                // ChatExporter.renderTranscript() can format it.
                MessageReducer tmp = new MessageReducer();
                for (AgentMessage m : conv) {
                    if (AgentMessage.ROLE_USER.equals(m.role)) {
                        if (m.hasToolResults()) {
                            for (AgentMessage.ToolResultContent r : m.toolResults) {
                                tmp.addToolResult(r.toolName, r.output, r.isError);
                            }
                        } else {
                            tmp.addUserMessage(m.text == null ? "" : m.text);
                        }
                    } else if (AgentMessage.ROLE_ASSISTANT.equals(m.role)) {
                        if (m.hasToolCalls()) {
                            for (AgentMessage.ToolCall c : m.toolCalls) {
                                tmp.addToolCall(c.name, c.argumentsJson);
                            }
                        }
                        if (m.text != null && !m.text.isEmpty()) {
                            tmp.addCompletion(m.text);
                        }
                    }
                }
                java.io.File file = ChatExporter.writeToCacheFile(a, tmp.getMessages());
                Intent share = ChatExporter.createShareIntent(a, file);
                runOnUiIfAlive(() -> {
                    if (v != null) Snackbar.make(v,
                            R.string.ai_thread_export_done, Snackbar.LENGTH_SHORT).show();
                    startActivity(share);
                });
            } catch (Exception e) {
                runOnUiIfAlive(() -> {
                    if (v != null) Snackbar.make(v,
                            getString(R.string.ai_thread_export_failed, e.getMessage()),
                            Snackbar.LENGTH_LONG).show();
                });
            }
        }, "ai-thread-export").start();
    }

    private void showRenameDialog(TaskHistoryStore.TaskMetadata thread) {
        Activity a = getActivity();
        if (a == null) return;
        final EditText et = new EditText(a);
        et.setText(thread.firstUserMessage);
        et.setHint(R.string.ai_thread_rename_hint);
        et.setSingleLine(true);
        new AlertDialog.Builder(a)
                .setTitle(R.string.ai_thread_rename_title)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) return;
                    renameThread(thread, name);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void renameThread(TaskHistoryStore.TaskMetadata thread, String newName) {
        final TaskHistoryStore store = getTaskHistoryStore();
        new Thread(() -> {
            try {
                File f = new File(store.getHistoryDir(), thread.id + ".json");
                if (!f.exists()) return;
                String raw = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonObject root = new com.google.gson.Gson().fromJson(raw, com.google.gson.JsonObject.class);
                root.addProperty("firstUserMessage", newName);
                java.nio.file.Files.write(f.toPath(),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                runOnUiIfAlive(this::refreshThreads);
            } catch (Throwable ignored) { }
        }, "ai-thread-rename").start();
    }

    private void confirmDeleteThread(TaskHistoryStore.TaskMetadata thread) {
        Activity a = getActivity();
        if (a == null) return;
        new AlertDialog.Builder(a)
                .setTitle(R.string.ai_thread_action_delete)
                .setMessage(R.string.ai_thread_delete_confirm)
                .setPositiveButton(R.string.ai_thread_action_delete, (d, w) -> {
                    final TaskHistoryStore store = getTaskHistoryStore();
                    new Thread(() -> {
                        store.delete(thread.id);
                        runOnUiIfAlive(this::refreshThreads);
                    }, "ai-thread-delete").start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Attach button: bottom-sheet popup + image handling
    // ==================================================================

    /**
     * Register the activity-result launchers used by the attach popup. Must
     * be called from {@link #onCreateView} (or earlier — fragment lifecycle
     * requirement: {@code registerForActivityResult} can only be called
     * before {@code STARTED}).
     */
    private void registerAttachmentLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (ok != null && ok && pendingCameraUri != null) {
                        addImageFromUri(pendingCameraUri);
                    }
                    pendingCameraUri = null;
                });
        photosLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) addImageFromUri(uri);
                });
        uploadLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) addImageFromUri(uri);
                });
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted != null && granted) {
                        doLaunchCamera();
                    } else {
                        toast(R.string.ai_attach_no_camera);
                    }
                });
    }

    /** Show the attach-tools bottom sheet (Camera / Photos / Upload tiles). */
    private void showAttachSheet() {
        Activity a = getActivity();
        if (a == null) return;
        AiToolsBottomSheet.show(a, new AiToolsBottomSheet.Callback() {
            @Override public void onCamera() { launchCamera(); }
            @Override public void onPhotos() { launchPhotos(); }
            @Override public void onUpload() { launchUpload(); }
        });
    }

    /** Launch the camera app, writing the captured image to a FileProvider URI. */
    private void launchCamera() {
        // CAMERA is a runtime permission on Android 6+. Request it on demand
        // when the user picks the Camera tile.
        if (cameraPermissionLauncher == null) return;
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            doLaunchCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void doLaunchCamera() {
        if (cameraLauncher == null) return;
        try {
            File out = new File(requireContext().getCacheDir(), "ai_camera_"
                    + System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", out);
            pendingCameraUri = uri;
            cameraLauncher.launch(uri);
        } catch (Throwable t) {
            View v = getView();
            if (v != null) Snackbar.make(v, R.string.ai_attach_no_camera, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Launch the system photo picker. */
    private void launchPhotos() {
        if (photosLauncher == null) return;
        try {
            photosLauncher.launch("image/*");
        } catch (Throwable t) {
            View v = getView();
            if (v != null) Snackbar.make(v, R.string.ai_attach_no_gallery, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Launch the system file picker (any MIME type). */
    private void launchUpload() {
        if (uploadLauncher == null) return;
        try {
            uploadLauncher.launch(new String[]{"*/*"});
        } catch (Throwable t) {
            View v = getView();
            if (v != null) Snackbar.make(v, R.string.ai_attach_no_file_picker, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Read the image at the supplied URI, downscale to a reasonable size,
     * base64-encode it as JPEG, and add it to {@link #attachedImages}.
     * Refreshes the thumbnails strip afterwards.
     */
    private void addImageFromUri(Uri uri) {
        new Thread(() -> {
            try {
                Bitmap bmp = loadDownscaledBitmap(uri, 1024);
                if (bmp == null) {
                    runOnUiIfAlive(() -> toast(R.string.ai_attach_image_failed));
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] bytes = baos.toByteArray();
                String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                runOnUiIfAlive(() -> {
                    if (attachedImages.size() >= MAX_ATTACHED_IMAGES) {
                        toast(R.string.ai_attach_max_reached);
                        return;
                    }
                    attachedImages.add(b64);
                    renderThumbnails();
                    toast(R.string.ai_attach_image_added);
                });
            } catch (Throwable t) {
                runOnUiIfAlive(() -> toast(R.string.ai_attach_image_failed));
            }
        }, "ai-image-attach").start();
    }

    /** Decode + downscale a bitmap from a content URI. */
    private Bitmap loadDownscaledBitmap(Uri uri, int maxDim) throws IOException {
        Context ctx = getContext();
        if (ctx == null) return null;
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);
        is.close();
        int sample = 1;
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2;
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        is = ctx.getContentResolver().openInputStream(uri);
        Bitmap bmp = is == null ? null : BitmapFactory.decodeStream(is, null, opts);
        if (is != null) is.close();
        return bmp;
    }

    /** Rebuild the thumbnails strip from {@link #attachedImages}. */
    private void renderThumbnails() {
        if (thumbnailsContainer == null || thumbnailsScroll == null) return;
        thumbnailsContainer.removeAllViews();
        if (attachedImages.isEmpty()) {
            thumbnailsScroll.setVisibility(View.GONE);
            return;
        }
        thumbnailsScroll.setVisibility(View.VISIBLE);
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (int i = 0; i < attachedImages.size(); i++) {
            final int idx = i;
            View chip = inf.inflate(R.layout.ai_chat_thumbnail_item, thumbnailsContainer, false);
            ImageView img = chip.findViewById(R.id.thumb_image);
            View remove = chip.findViewById(R.id.thumb_remove);
            // Decode just enough for the thumbnail preview.
            new Thread(() -> {
                try {
                    byte[] bytes = Base64.decode(attachedImages.get(idx), Base64.NO_WRAP);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiIfAlive(() -> {
                        if (bmp != null && img != null) img.setImageBitmap(bmp);
                    });
                } catch (Throwable ignored) { }
            }, "ai-thumb-" + idx).start();
            if (remove != null) {
                remove.setOnClickListener(v -> {
                    if (idx < attachedImages.size()) {
                        attachedImages.remove(idx);
                        renderThumbnails();
                        toast(R.string.ai_attach_image_removed);
                    }
                });
            }
            thumbnailsContainer.addView(chip);
        }
    }

    private void toast(int resId) {
        View v = getView();
        if (v != null) Snackbar.make(v, resId, Snackbar.LENGTH_SHORT).show();
    }
}
