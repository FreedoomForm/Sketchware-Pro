=== tool classes ===
app/src/main/java/com/sketchware/ai/agent/AbortController.java
app/src/main/java/com/sketchware/ai/agent/AgentListener.java
app/src/main/java/com/sketchware/ai/agent/AgentMessage.java
app/src/main/java/com/sketchware/ai/agent/AgentMode.java
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java
app/src/main/java/com/sketchware/ai/agent/LoopDetector.java
app/src/main/java/com/sketchware/ai/context/AgenticCompactor.java
app/src/main/java/com/sketchware/ai/context/BasicCompactor.java
app/src/main/java/com/sketchware/ai/context/CompactionPrompts.java
app/src/main/java/com/sketchware/ai/context/Compactor.java
app/src/main/java/com/sketchware/ai/context/ContextMentionParser.java
app/src/main/java/com/sketchware/ai/context/ContextTruncator.java
app/src/main/java/com/sketchware/ai/context/ConversationSerializer.java
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java
app/src/main/java/com/sketchware/ai/context/OhMyPiCompactor.java
app/src/main/java/com/sketchware/ai/context/SnapCompactCompactor.java
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java
app/src/main/java/com/sketchware/ai/context/snapcompact/BdfFont.java
app/src/main/java/com/sketchware/ai/context/snapcompact/BdfFontRegistry.java
app/src/main/java/com/sketchware/ai/context/snapcompact/SilverFontRegistry.java
app/src/main/java/com/sketchware/ai/context/snapcompact/SnapCompact.java
app/src/main/java/com/sketchware/ai/context/snapcompact/SnapCompactRenderer.java
app/src/main/java/com/sketchware/ai/context/snapcompact/SnapCompactText.java
app/src/main/java/com/sketchware/ai/llm/ApiStreamChunk.java
app/src/main/java/com/sketchware/ai/llm/LlmProvider.java
app/src/main/java/com/sketchware/ai/llm/LlmRequest.java
app/src/main/java/com/sketchware/ai/llm/ModelInfo.java
app/src/main/java/com/sketchware/ai/llm/ProviderCatalog.java
app/src/main/java/com/sketchware/ai/llm/TokenEstimator.java
app/src/main/java/com/sketchware/ai/llm/UsageTracker.java
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java
app/src/main/java/com/sketchware/ai/llm/http/SseEvent.java
app/src/main/java/com/sketchware/ai/llm/http/SseParser.java
app/src/main/java/com/sketchware/ai/llm/providers/AnthropicProvider.java
app/src/main/java/com/sketchware/ai/llm/providers/GeminiProvider.java
app/src/main/java/com/sketchware/ai/llm/providers/OllamaProvider.java
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiCompatProvider.java
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java
app/src/main/java/com/sketchware/ai/llm/reasoning/ReasoningEffort.java
app/src/main/java/com/sketchware/ai/llm/reasoning/ReasoningRequest.java
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java
app/src/main/java/com/sketchware/ai/llm/routing/Phase.java
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionBuildInput.java
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionMatchInput.java
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionRule.java
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionRules.java
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionRulesEngine.java
app/src/main/java/com/sketchware/ai/llm/routing/Suppression.java
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java
app/src/main/java/com/sketchware/ai/prompt/UserInputModeWrapper.java
app/src/main/java/com/sketchware/ai/tools/AskQuestionTool.java
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java
app/src/main/java/com/sketchware/ai/tools/CategoryUmbrellaTool.java
app/src/main/java/com/sketchware/ai/tools/JsonSchemaValidator.java
app/src/main/java/com/sketchware/ai/tools/SketchwareTool.java
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java
app/src/main/java/com/sketchware/ai/tools/StubTool.java
app/src/main/java/com/sketchware/ai/tools/SubmitAndExitTool.java
app/src/main/java/com/sketchware/ai/tools/ToolExecutor.java
app/src/main/java/com/sketchware/ai/tools/ToolPermissionGate.java
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java
app/src/main/java/com/sketchware/ai/tools/ToolResult.java
app/src/main/java/com/sketchware/ai/tools/ToolResultFormatter.java
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java
app/src/main/java/com/sketchware/ai/tools/block/BlockAddTool.java
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java
app/src/main/java/com/sketchware/ai/tools/block/ControlFlowTool.java
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java
app/src/main/java/com/sketchware/ai/tools/block/MathOperationTool.java
app/src/main/java/com/sketchware/ai/tools/block/MoreblockManageTool.java
app/src/main/java/com/sketchware/ai/tools/block/StringOperationTool.java
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java
app/src/main/java/com/sketchware/ai/tools/build/BuildActionTool.java
app/src/main/java/com/sketchware/ai/tools/build/ExportActionTool.java
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java
app/src/main/java/com/sketchware/ai/tools/component/ComponentAddTool.java
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java
app/src/main/java/com/sketchware/ai/tools/diff/DiffParser.java
app/src/main/java/com/sketchware/ai/tools/diff/PatchParser.java
app/src/main/java/com/sketchware/ai/tools/event/CustomEventManageTool.java
app/src/main/java/com/sketchware/ai/tools/event/EventAttachTool.java
app/src/main/java/com/sketchware/ai/tools/event/EventListTool.java
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java
app/src/main/java/com/sketchware/ai/tools/filesystem/ListFilesTool.java
app/src/main/java/com/sketchware/ai/tools/filesystem/SearchFilesTool.java
app/src/main/java/com/sketchware/ai/tools/java/JavaEditFileTool.java
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java
app/src/main/java/com/sketchware/ai/tools/java/JavaReadFileTool.java
app/src/main/java/com/sketchware/ai/tools/library/LibraryConfigureTool.java
app/src/main/java/com/sketchware/ai/tools/library/LibraryEnableTool.java
app/src/main/java/com/sketchware/ai/tools/library/LibraryManageTool.java
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java
app/src/main/java/com/sketchware/ai/tools/library/PermissionManageTool.java
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java
app/src/main/java/com/sketchware/ai/tools/project/ProjectEnableFeatureTool.java
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetAppNameTool.java
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetPackageNameTool.java
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetPropertyTool.java
app/src/main/java/com/sketchware/ai/tools/project/ThemeManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewDeleteWidgetTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewListWidgetsTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewSetPropertyTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java
app/src/main/java/com/sketchware/ai/tools/web/WebFetchTool.java
app/src/main/java/com/sketchware/ai/tools/web/WebSearchTool.java
app/src/main/java/com/sketchware/ai/ui/chat/ChatExporter.java
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java
app/src/main/java/com/sketchware/ai/ui/chat/ChatMessage.java
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java
app/src/main/java/com/sketchware/ai/ui/chat/SlashCommandProcessor.java
app/src/main/java/com/sketchware/ai/ui/chat/TypingDotsView.java
app/src/main/java/com/sketchware/ai/ui/chat/adapter/AiModelSheetAdapter.java
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatThreadsAdapter.java
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiModelPickerSheet.java
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheet.java
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolsBottomSheet.java
app/src/main/java/com/sketchware/ai/ui/settings/AISettingsActivity.java
app/src/main/java/com/sketchware/ai/ui/settings/AdvancedSettingsFragment.java
app/src/main/java/com/sketchware/ai/ui/settings/ApiConfigurationFragment.java
app/src/main/java/com/sketchware/ai/ui/settings/AutoApproveFragment.java
app/src/main/java/com/sketchware/ai/ui/settings/ExperimentalFragment.java
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java
app/src/main/java/com/sketchware/ai/ui/settings/ProviderIconResolver.java
app/src/main/java/com/sketchware/ai/ui/settings/ProvidersListFragment.java
app/src/main/java/com/sketchware/ai/util/PathSafety.java
app/src/main/java/com/sketchware/ai/util/SketchwareApi.java
=== tool registrations/names ===
app/src/main/java/com/sketchware/ai/agent/AgentMode.java:12: *       No edits, no state-changing commands. At the end of research,
app/src/main/java/com/sketchware/ai/agent/AgentMode.java:17: *       edits, no state-changing commands. Once the user approves, they
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:213:     * Returns a copy — mutations to the returned list do not affect the
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:345:            // fails with an overflow error we compact and retry; if the retry
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:347:            // agent dead). Now we let the outer while loop retry from the
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:401:                    // Overflow recovery: compact and retry. Previously this
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:402:                    // was a single-shot retry — if it failed, onError+return
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:404:                    // chat after every compaction. Now we compact, retry, and
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:405:                    // if the retry ALSO fails we let the outer while loop take
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:407:                    // terminating. A retry budget prevents infinite loops.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:411:                    // CRITICAL: clear all buffers before retrying. The first
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:413:                    // before failing; if we don't reset, the retry would APPEND
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:414:                    // to the existing buffers, producing duplicated text and
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:423:                        LlmRequest retry = rebuildRequest(model, reasoning);
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:424:                        for (ApiStreamChunk chunk : provider.stream(retry)) {
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:448:                        // now keeps retrying with the new (compacted) context
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:449:                        // instead of giving up after one failed retry.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:474:                // Reset the compaction-retry budget so a future overflow in a
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:489:                // which confuses the model into retrying the empty-name call.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:525:                // Deduplicate tool calls by signature BEFORE execution.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:529:                // duplicate executes — e.g. view_add_widget creates a second
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:532:                // result back to the LLM so it knows the duplicate was ignored.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:546:                            + " duplicate tool call(s) in this batch to prevent repeated execution.");
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:593:                        // todo_list first, then retry.
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:651:                // Append synthetic "skipped" results for any duplicate tool
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:653:                // its duplicate was ignored (instead of silently dropping it).
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:657:                            "Skipped: this tool call is an exact duplicate of an earlier call in the same batch and was not executed again. Do not repeat identical tool calls.",
app/src/main/java/com/sketchware/ai/agent/AgentRuntime.java:753:        String inferredName = inferred.name();
app/src/main/java/com/sketchware/ai/context/AgenticCompactor.java:108: *   <li><b>File operations</b> — read/written/edited paths are extracted
app/src/main/java/com/sketchware/ai/context/BasicCompactor.java:33: *       retrying. {@code shake} is safe here because it doesn't require
app/src/main/java/com/sketchware/ai/context/CompactionPrompts.java:120:        "MUST build on prior work; NEVER duplicate prior work.\n\n" +
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:19: * Track file operations (read / written / edited) across a conversation so
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:32: *   <li>Edits: {@code edit_file}, {@code apply_patch}, {@code diff_edit_file},
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:33: *       {@code java_edit_file}, {@code java_modify_class},
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:35: *       {@code view_delete_widget}, {@code view_manage_widget}</li>
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:50:    private final Set<String> edited = new LinkedHashSet<>();
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:74:                edited.add(path);
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:87:        edited.addAll(other.edited);
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:92:     * and modifiedFiles (written or edited).
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:96:        for (String f : edited) if (!isUrlScheme(f)) modified.add(f);
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:217:        return name.equals("edit_file") || name.equals("apply_patch")
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:218:            || name.equals("diff_edit_file") || name.equals("java_edit_file")
app/src/main/java/com/sketchware/ai/context/FileOperationsTracker.java:220:            || name.equals("view_set_property") || name.equals("view_delete_widget")
app/src/main/java/com/sketchware/ai/context/OhMyPiCompactor.java:38: *   <li><b>File operations tracking</b> — read/written/edited file paths
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java:290:    /** Delete a saved task. Returns true if deleted. */
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java:291:    public boolean delete(String taskId) {
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java:293:        return file.exists() && file.delete();
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java:297:    public int deleteOlderThan(long cutoffMillis) {
app/src/main/java/com/sketchware/ai/context/TaskHistoryStore.java:303:                if (f.delete()) n++;
app/src/main/java/com/sketchware/ai/context/snapcompact/BdfFont.java:86:    public String name() { return name; }
app/src/main/java/com/sketchware/ai/context/snapcompact/BdfFontRegistry.java:53:                BdfFont font = BdfFont.load(name.name(), in);
app/src/main/java/com/sketchware/ai/llm/ModelInfo.java:12:    public final boolean supportsTools;
app/src/main/java/com/sketchware/ai/llm/ModelInfo.java:25:                     boolean supportsTools,
app/src/main/java/com/sketchware/ai/llm/ModelInfo.java:37:        this.supportsTools = supportsTools;
app/src/main/java/com/sketchware/ai/llm/ProviderCatalog.java:30: * catalog consolidates them so a single edit propagates everywhere.
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:24:    // Track the thread currently sleeping inside postStreamWithRetry's retry
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:39:                    .retryOnConnectionFailure(true)
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:95:     * Execute a POST with automatic retry on HTTP 429 (rate limited) and 5xx
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:99:     * <p>Mirrors Cline's retry logic in {@code core/api/index.ts}: up to
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:102:     * thundering-herd. The retry loop is interruptible — calling
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:114:     * @throws RuntimeException           for non-retryable HTTP errors (4xx other than 429)
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:127:            // Close any previous response before retrying.
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:139:            // Non-retryable: 4xx other than 429 — close and throw immediately.
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:146:            // Retryable (429 or 5xx). Decide whether to retry or give up.
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:157:            // Close the response body before retrying — we don't need the body
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:158:            // for the retry decision, only the status code (which we already
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:173:                    throw new RuntimeException("Request aborted during retry backoff");
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:206:        // Also interrupt the thread sleeping in retry backoff, if any.
app/src/main/java/com/sketchware/ai/llm/http/HttpClient.java:217:     * Uses {@link #postStreamWithRetry} for automatic 429/5xx retry handling.
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:9: * HTTP 429 / 5xx retry helper with exponential backoff + jitter.
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:11: * <p>Mirrors Cline's retry strategy in {@code core/api/index.ts}: honor the
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:15: * thunder-herd the server on retry.
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:21:    /** Maximum retry attempts for transient errors (429, 5xx). */
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:24:    /** Base backoff in millis. The first retry waits ~this long (plus jitter). */
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:27:    /** Cap on per-retry backoff. Even after many retries, never wait longer than this. */
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:36:     * Decide whether an HTTP response is retryable (429 or 5xx).
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:43:     * Compute the backoff delay for the given retry attempt (0-indexed).
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:51:     * @param attemptIndex 0 for the first retry, 1 for the second, etc.
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:55:        long retryAfter = parseRetryAfter(response);
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:56:        if (retryAfter > 0) {
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:57:            return Math.min(retryAfter, MAX_BACKOFF_MS);
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:126:     * Build a human-readable description of the retry decision, for logging.
app/src/main/java/com/sketchware/ai/llm/http/RateLimitHandler.java:130:                + MAX_RETRIES + ") — retrying in " + delayMs + "ms";
app/src/main/java/com/sketchware/ai/llm/providers/AnthropicProvider.java:94:        // Use postStreamWithRetry for automatic 429/5xx retry with backoff.
app/src/main/java/com/sketchware/ai/llm/providers/AnthropicProvider.java:143:        // Tools
app/src/main/java/com/sketchware/ai/llm/providers/GeminiProvider.java:108:        // Use postStreamWithRetry for automatic 429/5xx retry with backoff.
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiCompatProvider.java:128:            // Tools and images are supported; reasoning is supported on
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiCompatProvider.java:152:                    reasoning.addProperty("effort", request.reasoning.effort.name().toLowerCase());
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiCompatProvider.java:191:                    reasoning.addProperty("effort", request.reasoning.effort.name().toLowerCase());
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java:90:        // Use postStreamWithRetry to get automatic 429/5xx retry with
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java:92:        // the "retry also gets 429" cascade that previously caused the agent
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java:108:            try (java.util.Scanner scanner = new java.util.Scanner(in, java.nio.charset.StandardCharsets.UTF_8.name())) {
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java:232:        // Tools
app/src/main/java/com/sketchware/ai/llm/providers/OpenAiProvider.java:289:            root.addProperty("reasoning_effort", request.reasoning.effort.name().toLowerCase());
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:40: *     prefer: context_window >= 128K, supportsTools, supportsImages (for screenshots)
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:43: *     prefer: supportsReasoning, supportsTools, context_window >= 64K
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:48: *         prefer: small/fast model (<= $1/M input), supportsTools
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:51: *         prefer: mid-tier model (<= $5/M input), supportsTools
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:54: *         prefer: frontier model, supportsReasoning, supportsTools
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:93:            "add", "set", "remove", "delete", "list", "show", "rename", "move",
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:160:        List<ModelInfo> pool = filterByTools(candidates);
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:180:        List<ModelInfo> pool = filterByTools(candidates);
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:204:        List<ModelInfo> pool = filterByTools(candidates);
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:283:    private static List<ModelInfo> filterByTools(List<ModelInfo> ms) {
app/src/main/java/com/sketchware/ai/llm/routing/ModelSelector.java:285:        for (ModelInfo m : ms) if (m.supportsTools) out.add(m);
app/src/main/java/com/sketchware/ai/llm/routing/ProviderOptionRules.java:245:                    reasoning.addProperty("effort", r.effort.name().toLowerCase());
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java:178:        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply();
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java:194:        prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply();
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java:211:    public void deleteProfile(String profileId) {
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java:221:        // Reset active if it was the deleted one.
app/src/main/java/com/sketchware/ai/llm/storage/ProviderConfigStore.java:238:        prefs.edit().putString(KEY_AUTO_APPROVE, gson.toJson(map)).apply();
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:37: *       questions, and presents a structured plan. It does NOT edit
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:74:        "User messages arrive wrapped in a <user_input mode=\"...\"> tag. The mode attribute is the interaction mode the user was in when they sent that message: \"plan\" means plan-mode constraints applied (explore, analyze, and align on a plan -- no edits or state-changing commands), while \"act\" (or \"yolo\") means implementation was allowed. If the mode attribute changes between messages, the user switched modes -- the newest message's mode is what governs right now, regardless of what earlier messages allowed. A <mode_notice> block inside a message marks exactly when such a switch happened.";
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:83:     * In Plan mode, use only tools currently registered as read-only editor
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:94:        "- Do NOT edit files, write code, run destructive commands, or make any changes\n" +
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:145:     *   <li>Does NOT edit files, write code, run destructive commands,
app/src/main/java/com/sketchware/ai/prompt/PlanActPrompts.java:188:        "- Do NOT edit files, write code, run destructive commands, or make any changes\n" +
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:35:          .append("When a tool returns an error, read the error, correct the arguments or inspect the relevant state, then retry only when appropriate.\n\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:57:        sb.append("8. Prefer the simplest tool that achieves the goal. Avoid chained edits when one tool call suffices.\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:58:        sb.append("9. When generating Java code via java_edit_file, use the project's package name as the package declaration.\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:59:        sb.append("10. Be conservative: ask before performing destructive operations (delete widget, delete file, reset blocks).\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:63:        sb.append("13. For editing existing files, PREFER `diff_edit_file` over `java_edit_file` - it sends only the ");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:65:        sb.append("when editing multiple files in one call.\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:68:        sb.append("15. Use `list_files(path=\"...\")` to explore the project structure before editing files you ");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:93:        // Diff editing instructions
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:95:        sb.append("When using `diff_edit_file`, format your diff as one or more blocks:\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:105:        sb.append("re-read the file with `java_read_file` and retry with corrected SEARCH content.\n\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:109:        sb.append("When editing multiple files at once, use `apply_patch` with this format:\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:200:        // Tools list
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:202:            sb.append("# Tools\n\n");
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:207:            sb.append("Tools marked **(universal)** accept an `action` enum parameter that selects ")
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:211:            for (SketchwareTool t : ToolVisibilityPolicy.canonicalTools(tools)) {
app/src/main/java/com/sketchware/ai/prompt/SystemPromptBuilder.java:216:                sb.append("- `").append(t.name()).append("`");
app/src/main/java/com/sketchware/ai/prompt/UserInputModeWrapper.java:27: * edits or state-changing commands until the user toggles back to Act.
app/src/main/java/com/sketchware/ai/prompt/UserInputModeWrapper.java:153:                return "Research-mode constraints now apply: read-only exploration, no edits or state-changing commands. End with a <research_summary> block.";
app/src/main/java/com/sketchware/ai/prompt/UserInputModeWrapper.java:155:                return "Plan-mode constraints now apply: no edits or state-changing commands until the user toggles to Act.";
app/src/main/java/com/sketchware/ai/tools/AskQuestionTool.java:18:public final class AskQuestionTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/AskQuestionTool.java:20:    @Override public String name() { return "ask_question"; }
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:16: *       (e.g. auto-approve {@code view_list_widgets} but require approval for {@code view_delete_widget}).</li>
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:21: *   <li><b>Per-path rules</b>: for file-editing tools, auto-approve edits to specific paths
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:22: *       (e.g. auto-approve edits under {@code resource/values/} but require approval for
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:138:            if (r.matches(tool.name(), subcategory, action, path)) {
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:185:     * requires approval for destructive ops (delete, reset, clear).
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:205:        a.addRule(new Rule(null, null, "delete", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:208:        // Always require approval for manifest edits (umbrella-level rule).
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:213:        // ProGuard rule edits require approval (build_manage:proguard:edit_rules).
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:214:        a.addRule(new Rule("build_manage", "proguard", "edit_rules", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:216:        a.addRule(new Rule("resource_manage", "assets", "delete", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:217:        a.addRule(new Rule("resource_manage", "resource_file", "delete", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:218:        a.addRule(new Rule("resource_manage", "font", "delete", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/AutoApprover.java:219:        a.addRule(new Rule("resource_manage", "sound", "delete", null, Decision.REQUIRE_APPROVAL));
app/src/main/java/com/sketchware/ai/tools/CategoryUmbrellaTool.java:55:public final class CategoryUmbrellaTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/CategoryUmbrellaTool.java:84:    @Override public String name() { return name; }
app/src/main/java/com/sketchware/ai/tools/CategoryUmbrellaTool.java:168:        // Forward a copy of args without the "subcategory" field — the
app/src/main/java/com/sketchware/ai/tools/SketchwareTool.java:20:    String name();
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:32:     * what the editor is currently showing, the editor will switch to it.
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:34:     * was creating 'calculator' but the editor was still showing 'main'.
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:80:     *       under key {@code "main"} while the editor looked it up
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:84:     *   <li>The View editor canvas never showed AI-added widgets
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:114:     * Refresh the View editor canvas so changes are visible in real time.
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:116:     * editor to it if the user is viewing a different layout.
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:125:    /** Refresh the Logic editor canvas. */
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:146:    /** Refresh all known editors. */
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:157:     * where {@code view_list_widgets} reported N widgets but the editor showed
app/src/main/java/com/sketchware/ai/tools/SketchwareToolContext.java:159:     * never written to disk, so when the editor reloaded from disk (e.g. on
app/src/main/java/com/sketchware/ai/tools/StubTool.java:8: * Sketchware user actions (move widget, attach event, build APK, edit
app/src/main/java/com/sketchware/ai/tools/StubTool.java:22: * of</em> the stub. The registry rejects duplicates by name, so this is
app/src/main/java/com/sketchware/ai/tools/StubTool.java:25:public final class StubTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/StubTool.java:48:    @Override public String name() { return name; }
app/src/main/java/com/sketchware/ai/tools/SubmitAndExitTool.java:17:public final class SubmitAndExitTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/SubmitAndExitTool.java:19:    @Override public String name() { return "submit_and_exit"; }
app/src/main/java/com/sketchware/ai/tools/ToolPermissionGate.java:101:            Boolean subOverride = perToolSubcategoryAutoApprove.get(tool.name() + ":" + subcat);
app/src/main/java/com/sketchware/ai/tools/ToolPermissionGate.java:107:        Boolean override = perToolAutoApprove.get(tool.name());
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:24:        if (tool == null || tool.name() == null) {
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:25:            throw new IllegalArgumentException("tool and tool.name() must not be null");
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:27:        if (tools.containsKey(tool.name())) {
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:28:            throw new IllegalStateException("Tool already registered: " + tool.name());
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:30:        tools.put(tool.name(), tool);
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:65:            sb.append(t.name());
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:82:            entry.addProperty("name", t.name());
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:92:     * Build the deduplicated schema payload used by the active AI agent. Legacy
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:99:        for (SketchwareTool t : ToolVisibilityPolicy.canonicalTools(this)) {
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:103:            entry.addProperty("name", t.name());
app/src/main/java/com/sketchware/ai/tools/ToolRegistry.java:145:     * so the inference succeeds. Args {@code {"action":"delete"}} would match
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:99: *       file editing).</li>
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:268:                "Manage built-in components (add/get/list/set_enabled/delete/clone/reorder) "
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:269:                        + "and custom Java UI components (add/list/get/update/delete/import/clone). "
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:283:                "Manage project (create/open/save/list/delete/export), set project properties, "
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:328:                        + "set_launcher/edit_components/show_source), AppCompat settings, and XML commands. "
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:347:                        + "sounds, resource files (XML/import/edit/rename/delete), and assets. "
app/src/main/java/com/sketchware/ai/tools/ToolResultFormatter.java:38:        sb.append(" Provide a value and retry.");
app/src/main/java/com/sketchware/ai/tools/ToolResultFormatter.java:51:        sb.append(" Correct the value and retry.");
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java:22: * edit manifest, etc.). The dispatch method invokes Sketchware's native
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java:26:public abstract class UniversalTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java:46:        // Deduplicate while preserving insertion order.
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java:51:    @Override public final String name() { return name; }
app/src/main/java/com/sketchware/ai/tools/UniversalTool.java:57:    /** Sorted, deduplicated list of supported actions (used in JSON schema enum). */
app/src/main/java/com/sketchware/ai/tools/block/BlockAddTool.java:16:public final class BlockAddTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/block/BlockAddTool.java:18:    @Override public String name() { return "block_add"; }
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:12: * <p>Replaces 10 stubs: block_manage:delete, block_manage:duplicate, block_manage:import_from_collection, block_manage:move, block_manage:redo, block_manage:save, block_manage:save_to_collection, block_manage:set_java_method, block_manage:set_parameter, block_manage:undo
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:21:                "Manage blocks inside event handlers: delete, duplicate, import from collection, move, redo, save, save to collection, set java method, set parameter, undo.",
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:23:"delete",
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:24:                "duplicate",
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:46:        p_new_id.addProperty("description", "(duplicate/move) New block ID.");
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:69:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:74:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:75:                                    SketchwareApi.invoke(editor, "n", eventId, blockId);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:80:            case "duplicate": {
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:83:                                String newId = optString(args, "new_id", blockId + "_copy");
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:86:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:87:                                    SketchwareApi.invoke(editor, "o", eventId, blockId, newId);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:98:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:99:                                    SketchwareApi.invoke(editor, "p", eventId, content);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:110:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:111:                                    SketchwareApi.invoke(editor, "q", eventId, blockId, newIndex);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:120:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:121:                                    SketchwareApi.invoke(editor, "r", eventId);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:130:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:131:                                    SketchwareApi.invoke(editor, "s", eventId);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:141:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:142:                                    Object def = SketchwareApi.invoke(editor, "t", eventId, blockId);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:154:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:155:                                    SketchwareApi.invoke(editor, "u", eventId, blockId, value);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:168:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:169:                                    SketchwareApi.invoke(editor, "v", eventId, blockId, paramName, value);
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:178:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/BlockManageTool.java:179:                                    SketchwareApi.invoke(editor, "w", eventId);
app/src/main/java/com/sketchware/ai/tools/block/ControlFlowTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/block/ControlFlowTool.java:113:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/ControlFlowTool.java:114:            Object newBlockId = SketchwareApi.invoke(editor, "x", eventId, blockType, parentBlockId,
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:17:import mod.hey.studios.editor.manage.block.v2.BlockLoader;
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:37: *   <li><b>edit</b> — edit an existing custom block's fields</li>
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:38: *   <li><b>duplicate</b> — duplicate a block (auto-suffixes name with _copyNN)</li>
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:40: *   <li><b>delete</b> — delete a block (optionally permanent vs recycle bin)</li>
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:87:                        + "create, edit, duplicate, move, delete, restore, "
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:91:                "edit",
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:92:                "duplicate",
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:94:                "delete",
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:108:        // Block definition fields (for create / edit)
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:109:        addStringProp(props, "type", "(create/edit) Block type: regular|c|e|s|b|d|v|a|f|l|p|h. 'regular' or ' ' = generic block.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:110:        addStringProp(props, "type_name", "(create/edit) Optional type name.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:111:        addStringProp(props, "spec", "(create/edit) Block spec with parameter placeholders, e.g. 'myBlock %s.inputOnly %b %d'.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:112:        addStringProp(props, "spec2", "(create/edit) Second spec line for type 'e' (if-else).");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:113:        addStringProp(props, "color", "(create/edit) Hex color like '#FFAABB'.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:114:        addStringProp(props, "code", "(create/edit) Java code template.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:115:        addStringProp(props, "imports", "(create/edit) Java imports string (optional).");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:120:        // delete flag
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:121:        addBoolProp(props, "permanent", "(delete) If true (default), permanently remove. If false, move to recycle bin (palette='-1').");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:158:                case "edit":              return doEdit(args);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:159:                case "duplicate":         return doDuplicate(args);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:161:                case "delete":            return doDelete(args);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:291:        // Disallow duplicate names
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:293:            return err("Block name '" + name + "' already exists. Use a different name or the 'edit' action.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:354:        if (!changed) return err("No edit fields provided. Specify at least one of: spec, type, type_name, color, code, imports, spec2.");
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:370:        HashMap<String, Object> copy = new HashMap<>(source);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:372:        if (name.matches("(?s).*_copy[0-9][0-9]")) {
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:373:            newName = name.replaceAll("_copy[0-9][0-9]", "_copy" + SketchwareUtil.getRandom(11, 99));
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:375:            newName = name + "_copy" + SketchwareUtil.getRandom(11, 99);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:377:        copy.put("name", newName);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:378:        blocks.add(idx + 1, copy);
app/src/main/java/com/sketchware/ai/tools/block/CustomBlockManageTool.java:425:            return ok("Permanently deleted custom block '" + name + "'.");
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:15: *   <li><b>delete</b> — delete a project-level list</li>
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:28: * <p>The first 6 actions (create/delete/add_item/remove_item/clear/size)
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:29: * use the obfuscated project-file editor returned by {@code jC.b(sc_id)}
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:30: * via reflection — known method letters: {@code a}=create, {@code b}=delete,
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:35: * ({@code c, d, e, j, k, m}) on the project-file editor. If a method is not
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:43:                "Manage project-level lists: create, delete, add_item, "
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:50:                "delete",
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:116:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:156:                            + "(method 'c' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:173:                            + "(method 'd' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:187:                            + "(method 'e' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:208:                            + "(method 'e' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:222:                            + "(method 'j' not found on editor). Use a runtime block "
app/src/main/java/com/sketchware/ai/tools/block/ListManageTool.java:236:                            + "(method 'k' not found on editor). Use a runtime block "
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:29: * project-file editor returned by {@code jC.b(sc_id)} via reflection —
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:34: * ({@code l, m, n, o, p, i}) on the project-file editor. If a method is
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:125:                            + "(method 'l' not found on editor). Use a runtime block "
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:139:                            + "(method 'm' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:152:                            + "(method 'n' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/MapManageTool.java:165:                            + "(method 'o' not found on editor). Cause: " + t.getMessage());
app/src/main/java/com/sketchware/ai/tools/block/MathOperationTool.java:25: * the obfuscated project-file editor returned by {@code jC.b(sc_id)} via reflection
app/src/main/java/com/sketchware/ai/tools/block/MathOperationTool.java:137:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/MathOperationTool.java:138:            Object newBlockId = SketchwareApi.invoke(editor, "y", eventId, "math:" + op, a, b, resultVar);
app/src/main/java/com/sketchware/ai/tools/block/MoreblockManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/block/StringOperationTool.java:24: * the obfuscated project-file editor returned by {@code jC.b(sc_id)} via reflection
app/src/main/java/com/sketchware/ai/tools/block/StringOperationTool.java:133:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/block/StringOperationTool.java:134:            Object newBlockId = SketchwareApi.invoke(editor, "z", eventId, "string:" + op,
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java:12: * <p>Replaces 5 stubs: variable_manage:create, variable_manage:delete, variable_manage:get_value, variable_manage:rename, variable_manage:set_value
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java:21:                "Manage project-level variables: create, delete, get_value, rename, set_value.",
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java:24:                "delete",
app/src/main/java/com/sketchware/ai/tools/block/VariableManageTool.java:62:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/build/BuildActionTool.java:39: *   <li>Added 2 new actions: {@code clean_temp_files} (deletes
app/src/main/java/com/sketchware/ai/tools/build/BuildActionTool.java:41: *       (deletes {@code .sketchware/mysc/{scId}/bin/}).</li>
app/src/main/java/com/sketchware/ai/tools/build/BuildActionTool.java:313:            FileUtil.deleteFile(path);
app/src/main/java/com/sketchware/ai/tools/build/BuildActionTool.java:335:            FileUtil.deleteFile(path);
app/src/main/java/com/sketchware/ai/tools/build/ExportActionTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java:40:                        + "toggle_r8, toggle_debug, edit_rules, select_fm_libs, "
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java:47:                "edit_rules",
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java:63:        p.addProperty("description", "(edit_rules) Full content of the proguard-rules.pro file. Replaces existing content.");
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java:86:                case "edit_rules":     return doEditRules(pg, scId, args);
app/src/main/java/com/sketchware/ai/tools/build/ProguardManageTool.java:134:    //  edit_rules
app/src/main/java/com/sketchware/ai/tools/component/ComponentAddTool.java:13:public final class ComponentAddTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/component/ComponentAddTool.java:15:    @Override public String name() { return "component_add"; }
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:15: * component_delete, component_export_to_collection,
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:30: *       incrementing suffix (_copy, _copy_2, _copy_3, ...).</li>
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:53:                        + "delete, export, import, list, open, or rename.",
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:55:                "attach_event", "clone", "delete", "export_to_collection",
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:72:        newId.addProperty("description", "(clone/rename) New component ID. If omitted for clone, a unique _copy suffix is generated.");
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:90:            case "delete": return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:104:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:105:            Object components = SketchwareApi.invoke(editor, "f");
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:140:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:141:            SketchwareApi.invoke(editor, "a", compId, eventName);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:165:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:166:            SketchwareApi.invoke(editor, "b", compId, newId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:176:    //  delete
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:183:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:184:            SketchwareApi.invoke(editor, "c", compId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:207:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:208:            Object def = SketchwareApi.invoke(editor, "d", compId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:226:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:227:            SketchwareApi.invoke(editor, "e", content);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:243:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:244:            SketchwareApi.invoke(editor, "g", compId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:247:                    + "Use event_manage:open_in_logic_editor to edit a specific event.");
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:263:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:264:            SketchwareApi.invoke(editor, "h", compId, newId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:284:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:285:            Object components = SketchwareApi.invoke(editor, "f");
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:297:        // Try _copy, then _copy_2, _copy_3, ...
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:298:        String suffix = "_copy";
app/src/main/java/com/sketchware/ai/tools/component/ComponentManageTool.java:302:            suffix = "_copy_" + n;
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java:138:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java:139:            SketchwareApi.invoke(editor, "i", compId, group + ":" + propKey, value);
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java:152:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/component/ComponentSetPropertyTool.java:153:            SketchwareApi.invoke(editor, "i", compId, group + ":" + propKey, key, value);
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:31: * {@link pro.sketchware.activities.editor.component.ManageCustomComponentActivity}
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:32: * + {@link pro.sketchware.activities.editor.component.AddCustomComponentActivity}.
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:62: *   <li><b>edit</b> — edit an existing component (params: {@code name} to identify,
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:64: *   <li><b>delete</b> — delete a component (params: {@code name}).</li>
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:88:                        + "create, edit, delete, export, import, list. "
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:92:                "create", "edit", "delete", "export", "import", "list");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:98:        addStringProp(props, "type_name", "(create/edit) Type name shown in palette (e.g. 'Lottie'). Defaults to component name.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:99:        addStringProp(props, "var_name", "(create/edit) Java variable name (lowercase first letter). Auto-derived from name if absent.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:100:        addStringProp(props, "icon", "(create/edit) Old Sketchware resource ID as a string (e.g. '3d_rotation').");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:101:        addStringProp(props, "build_class", "(create/edit) Build class name (defaults to class_name).");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:103:        addStringProp(props, "java_code", "(create/edit) Additional Java code (optional).");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:104:        addStringProp(props, "description", "(create/edit) Short description of the component.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:105:        addStringProp(props, "doc_url", "(create/edit) Documentation URL.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:106:        addStringProp(props, "imports", "(create/edit) Additional Java imports (semicolon-separated).");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:107:        addStringProp(props, "additional_var", "(create/edit) Extra variable declarations.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:108:        addStringProp(props, "define_additional_var", "(create/edit) Extra variable definitions.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:125:                case "edit":   return doEdit(args);
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:126:                case "delete": return doDelete(args);
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:166:            return err("Component '" + name + "' already exists. Use 'edit' to modify.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:200:    //  edit
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:243:            return err("No edit fields provided. Specify at least one of: class_name, type_name, var_name, build_class, icon, description, doc_url, imports, additional_var, define_additional_var, layout_xml, java_code.");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:246:            return err("Internal error: edited component failed validation (missing required key).");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:253:    //  delete
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:313:        List<String> duplicates = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:319:                duplicates.add(name);
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:326:            return err("Nothing imported. Duplicates: " + duplicates + ". (Total in source: " + imported.size() + ")");
app/src/main/java/com/sketchware/ai/tools/component/CustomComponentManageTool.java:331:        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:18: * visual editor. It contains no alternative file-write or renderer path.
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:20:public final class CreatorRuntimeTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:21:    @Override public String name() { return "creator_runtime"; }
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:27:        return "Apply one transparent Creator Runtime operation to the live, user-editable project. "
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:131:                return ToolResult.error("Creator Runtime rejected " + operation.getType().name() + ": "
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:132:                        + result.getValidation().getCode().name() + " — " + result.getValidation().getMessage());
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:137:            return ToolResult.success("Creator Runtime applied " + operation.getType().name()
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java:15:public final class ActivityListTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java:16:    @Override public String name() { return "activity_list"; }
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:23: * <p>Enables single-call editing of multiple files: add new files, update
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:24: * existing files (with hunk-based line replacement), and delete files.
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:46: * and which failed, then retry the failures.
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:51:public final class ApplyPatchTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:53:    @Override public String name() { return "apply_patch"; }
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:61:                + "and deleting files in a single tool call. Use this when you need to edit multiple "
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:92:                    name(), "patch", "specify the patch to apply"));
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:104:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:109:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:175:                        return "FAIL: " + op.path + " not found (already deleted?)";
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:177:                    if (!target.delete()) {
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:178:                        return "FAIL: " + op.path + " could not delete (File.delete() returned false)";
app/src/main/java/com/sketchware/ai/tools/diff/ApplyPatchTool.java:180:                    return "OK: " + op.path + " deleted";
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:19: * diff_edit_file - apply SEARCH/REPLACE blocks to a file in the project's
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:25: * small edits to large files: a 1000-line Java file with a 5-line change
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:49: * block index and the search snippet so the LLM can correct and retry.
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:51:public final class DiffEditFileTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:53:    @Override public String name() { return "diff_edit_file"; }
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:60:                + "Prefer this over java_edit_file for small edits to large files - it saves tokens. "
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:73:        filePath.addProperty("description", "Path of the file to edit, relative to project files/.");
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:99:                    name(), "file_path", "locate the file to edit"));
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:103:                    name(), "diff", "specify the SEARCH/REPLACE blocks to apply"));
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:127:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:129:                    "Check file permissions and retry."));
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:136:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:141:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:150:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:158:            return ToolResult.success(ToolResultFormatter.toolSuccess(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:166:            return ToolResult.error(ToolResultFormatter.toolError(name(),
app/src/main/java/com/sketchware/ai/tools/diff/DiffEditFileTool.java:172:        return ToolResult.success(ToolResultFormatter.toolSuccess(name(),
app/src/main/java/com/sketchware/ai/tools/diff/PatchParser.java:60:    /** One operation in a patch: add / update / delete a file. */
app/src/main/java/com/sketchware/ai/tools/event/EventAttachTool.java:13:public final class EventAttachTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/event/EventAttachTool.java:15:    @Override public String name() { return "event_attach"; }
app/src/main/java/com/sketchware/ai/tools/event/EventListTool.java:14:public final class EventListTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/event/EventListTool.java:16:    @Override public String name() { return "event_list"; }
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:12: * <p>Replaces 10 stubs: event_manage:delete, event_manage:duplicate, event_manage:list_available, event_manage:open_in_logic_editor, event_manage:reset_blocks, event_manage:search, event_manage:set_activity_event, event_manage:set_drawer_event, event_manage:set_target, event_manage:sort
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:21:                "Manage event handlers in the current project: delete, duplicate, list available event types, open in logic editor, reset blocks, search handlers, set activity-level event, set drawer event, set target, or sort.",
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:23:"delete",
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:24:                "duplicate",
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:26:                "open_in_logic_editor",
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:38:        p_event_id.addProperty("description", "ID of the event handler (for delete/duplicate/open/reset/set_target).");
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:62:        p_new_id.addProperty("description", "(duplicate) New event handler ID.");
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:69:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:73:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:74:                                    SketchwareApi.invoke(editor, "d", eventId);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:79:            case "duplicate": {
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:81:                                String newId = optString(args, "new_id", eventId + "_copy");
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:84:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:85:                                    SketchwareApi.invoke(editor, "e", eventId, newId);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:92:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:93:                                    Object events = SketchwareApi.invoke(editor, "f");
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:97:            case "open_in_logic_editor": {
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:101:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:102:                                    SketchwareApi.invoke(editor, "g", eventId);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:104:                                    return ok("Opened event '" + eventId + "' in logic editor.");
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:111:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:112:                                    SketchwareApi.invoke(editor, "h", eventId);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:120:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:121:                                    Object results = SketchwareApi.invoke(editor, "i", q);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:129:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:130:                                    SketchwareApi.invoke(editor, "j", activityEvent);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:140:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:141:                                    SketchwareApi.invoke(editor, "k", drawerId, eventName);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:151:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:152:                                    SketchwareApi.invoke(editor, "l", eventId, targetId);
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:159:                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
app/src/main/java/com/sketchware/ai/tools/event/EventManageTool.java:160:                                    SketchwareApi.invoke(editor, "m");
app/src/main/java/com/sketchware/ai/tools/filesystem/ListFilesTool.java:32:public final class ListFilesTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/filesystem/ListFilesTool.java:40:    @Override public String name() { return "list_files"; }
app/src/main/java/com/sketchware/ai/tools/filesystem/ListFilesTool.java:82:                    + "Tried wq.b(sc_id) and fallback paths. Make sure the project is open in the editor.");
app/src/main/java/com/sketchware/ai/tools/filesystem/SearchFilesTool.java:33:public final class SearchFilesTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/filesystem/SearchFilesTool.java:52:    @Override public String name() { return "search_files"; }
app/src/main/java/com/sketchware/ai/tools/java/JavaEditFileTool.java:17: * java_edit_file - write content to a Java/Kotlin/XML file in the project's
app/src/main/java/com/sketchware/ai/tools/java/JavaEditFileTool.java:20:public final class JavaEditFileTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/java/JavaEditFileTool.java:22:    @Override public String name() { return "java_edit_file"; }
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:15: * java_create_file, java_delete_file, java_format, java_import_files,
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:49:                        + "delete_file, format, import_files, remove_from_manifest_as_activity, "
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:52:                "add_field", "add_import", "add_method", "create_file", "delete_file",
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:105:        replaceText.addProperty("description", "(search_replace) Replacement text. Empty string deletes the search text.");
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:124:            case "delete_file": return doDeleteFile(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/java/JavaModifyClassTool.java:221:    //  delete_file
app/src/main/java/com/sketchware/ai/tools/java/JavaReadFileTool.java:17:public final class JavaReadFileTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/java/JavaReadFileTool.java:19:    @Override public String name() { return "java_read_file"; }
app/src/main/java/com/sketchware/ai/tools/library/LibraryConfigureTool.java:103:        p.addProperty("description", "(firebase_import_from_project / admob_import_from_project / googlemap_import_from_project) Source project sc_id to copy settings from.");
app/src/main/java/com/sketchware/ai/tools/library/LibraryConfigureTool.java:360:        // Avoid duplicates.
app/src/main/java/com/sketchware/ai/tools/library/LibraryConfigureTool.java:462:        destBean.copy(sourceBean);
app/src/main/java/com/sketchware/ai/tools/library/LibraryManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:24: * operations (create_folder, import_so, rename, delete, list) but
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:58:                        + "one or more .so files into an ABI folder), rename, delete, "
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:65:                "delete",
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:80:        p.addProperty("description", "(import_so) Array of absolute paths to .so files to copy into the target ABI folder.");
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:88:        p.addProperty("description", "(rename / delete) Absolute path of the file or folder to rename/delete. Use list first to discover paths.");
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:106:            case "delete":        return doDelete(scId, args);
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:199:                FileUtil.copyDirectory(new File(src), new File(dest));
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:202:                errors.append("Failed to copy '").append(src).append("': ")
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:245:    //  delete
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:256:            return err("Refusing to delete the native_libs root directory. "
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:257:                    + "Use list to inspect contents and delete specific ABI folders or .so files.");
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:260:            return ok("Path does not exist (already deleted?): " + path);
app/src/main/java/com/sketchware/ai/tools/library/NativeLibManageTool.java:263:            FileUtil.deleteFile(path);
app/src/main/java/com/sketchware/ai/tools/library/PermissionManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java:12: * <p>Replaces 3 stubs: appcompat_manage:add_attribute, appcompat_manage:delete_attribute, appcompat_manage:reset_to_defaults
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java:21:                "Manage AppCompat attributes: add_attribute, delete_attribute, reset_to_defaults.",
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java:24:                "delete_attribute",
app/src/main/java/com/sketchware/ai/tools/manifest/AppcompatManageTool.java:52:            case "delete_attribute": {
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:24: * <p><b>FIX-D-PROJECT (Task D3):</b> removed the duplicate {@code add_permission}
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:25: * action (it duplicated {@code permission_manage.add}). Use
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:32: *   <li>{@code edit_app_components} — replaces the raw XML injected into the
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:34: *   <li>{@code edit_activity_components} — replaces the intent-filter XML
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:37: *   <li>{@code edit_all_activities_attrs} — adds an attribute that applies
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:51: *   <li><b>delete_activity</b>: removes the activity entry.</li>
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:58: *   <li><b>edit_app_components</b>: replaces the raw XML injected into
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:60: *   <li><b>edit_activity_components</b>: replaces the intent-filter XML
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:62: *   <li><b>edit_all_activities_attrs</b>: adds an attribute applied to
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:91:                "Manage AndroidManifest.xml entries: add_activity, delete_activity, "
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:93:                        + "set_launcher_activity, edit_app_components, edit_activity_components, "
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:94:                        + "edit_all_activities_attrs, show_source. "
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:97:                "add_activity", "delete_activity",
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:99:                "set_launcher_activity", "edit_app_components",
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:100:                "edit_activity_components", "edit_all_activities_attrs",
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:107:        activityName.addProperty("description", "(add/delete_activity, set_activity_attribute, edit_activity_components, set_launcher_activity) Activity class name. Must be a valid Java class name (e.g. 'MainActivity' or 'com.example.MyActivity').");
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:112:        attrName.addProperty("description", "(set_activity_attribute/set_application_attribute/edit_all_activities_attrs) Attribute name WITHOUT the 'android:' prefix (e.g. 'exported', 'theme').");
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:117:        attrValue.addProperty("description", "(set_activity_attribute/set_application_attribute/edit_all_activities_attrs) Attribute value. For booleans, use 'true' or 'false'. For resources, use '@string/foo' or '@drawable/foo'.");
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:122:        componentsXml.addProperty("description", "(set_components / edit_app_components / edit_activity_components) Raw XML. For set_components: <components> section XML. For edit_app_components: XML to inject into <application>. For edit_activity_components: intent-filter XML to inject into a specific <activity>.");
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:133:            case "delete_activity": return doDeleteActivity(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:138:            case "edit_app_components": return doEditAppComponents(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:139:            case "edit_activity_components": return doEditActivityComponents(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:140:            case "edit_all_activities_attrs": return doEditAllActivitiesAttrs(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:167:    //  delete_activity
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:276:    //  edit_app_components — FIX-D-PROJECT Task D4
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:293:    //  edit_activity_components — FIX-D-PROJECT Task D4
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:337:    //  edit_all_activities_attrs — FIX-D-PROJECT Task D4
app/src/main/java/com/sketchware/ai/tools/manifest/ManifestManageTool.java:368:            // android:attr="..." string to avoid duplicates.
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:12: * <p>Replaces 3 stubs: xml_command_manage:add, xml_command_manage:delete, xml_command_manage:edit
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:21:                "Manage custom XML commands in the project: add, delete, edit.",
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:24:                "delete",
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:25:                "edit");
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:35:        p_command_xml.addProperty("description", "(add/edit) XML content of the command.");
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:52:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/manifest/XmlCommandManageTool.java:61:            case "edit": {
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:62: *   <li><b>remove</b> - delete a single item by index.</li>
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:186:                    name(), "content", "describe the TODO item"));
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:204:                        name(), "active_form", "missing",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:219:                    name(), "index", "out of range",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:232:                        name(), "status", "unknown value '" + newStatus + "'",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:246:                            name(), "active_form", "missing",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:259:                        name(), "priority", "unknown value '" + newPriority + "'",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:271:                    name(), "index", "out of range",
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:289:                    name(), "todos", "an array of todo items"));
app/src/main/java/com/sketchware/ai/tools/meta/TodoListTool.java:354:                    name(), "index", "out of range",
app/src/main/java/com/sketchware/ai/tools/project/ProjectEnableFeatureTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:12: * <p>Replaces 7 stubs: project_manage:create, project_manage:open, project_manage:close, project_manage:save, project_manage:delete, project_manage:export, project_manage:import
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:21:                "Manage whole projects: create, open, close, save, delete, export, or import.",
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:27:                "delete",
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:39:        p_project_id.addProperty("description", "Project sc_id (open/close/save/delete).");
app/src/main/java/com/sketchware/ai/tools/project/ProjectManageTool.java:85:            case "delete": {
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetAppNameTool.java:13:public final class ProjectSetAppNameTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetAppNameTool.java:15:    @Override public String name() { return "project_set_app_name"; }
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetPackageNameTool.java:13:public final class ProjectSetPackageNameTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetPackageNameTool.java:15:    @Override public String name() { return "project_set_package_name"; }
app/src/main/java/com/sketchware/ai/tools/project/ProjectSetPropertyTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/project/ThemeManageTool.java:161:     * colors.xml so the change is visible in the editor and survives a
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:36: *   <li><b>edit</b> — overwrite an existing text file (params:
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:40: *   <li><b>delete</b> — delete a file or folder (params: {@code path}).</li>
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:57:                        + "create_folder, import, edit, rename, delete, list. "
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:60:                "create_file", "create_folder", "import", "edit",
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:61:                "rename", "delete", "list");
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:68:        addStringProp(props, "dest_dir", "(import) Destination directory (relative to assets root) to copy into.");
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:69:        addStringProp(props, "content", "(create_file/edit) File content (text). Defaults to empty string.");
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:99:            case "edit":          return doEdit(args, root);
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:101:            case "delete":        return doDelete(args, root);
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:117:        if (FileUtil.isExistFile(abs)) return err("File already exists: " + rel + ". Use 'edit' to overwrite.");
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:166:                FileUtil.copyDirectory(srcFile, new File(destPath));
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:169:                missing.add(src + " (copy failed: " + t.getMessage() + ")");
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:184:    //  edit
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:220:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/AssetsManageTool.java:229:        FileUtil.deleteFile(abs);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:22: * {@link com.besome.sketch.editor.manage.font.ManageFontActivity} +
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:23: * {@link com.besome.sketch.editor.manage.font.AddFontActivity} +
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:24: * {@link com.besome.sketch.editor.manage.font.ImportFontFragment}.
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:34: *       {@link com.besome.sketch.editor.manage.font.ImportFontFragment#processResources()}).</li>
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:44: *   <li><b>edit</b> — rename an existing font (params: {@code name},
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:46: *   <li><b>delete</b> — delete one or more fonts (params: {@code name}
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:54: * preserves the source file extension when copying into the project
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:67:                "Manage project font resources: add, edit, delete, list, "
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:70:                "add", "edit", "delete", "list", "import_from_collection");
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:75:        addStringProp(props, "new_name", "(edit) New name to rename the font to.");
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:79:        addArrayProp(props, "names", "(delete) Array of font names to delete.");
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:112:            case "edit":                  return doEdit(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:113:            case "delete":                return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:165:            pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:196:    //  edit (rename only — fonts can't have their content "edited")
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:202:        if (newName == null || newName.isEmpty()) return err("new_name is required for font edit.");
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:230:            pro.sketchware.utility.FileUtil.copyFile(oldPath, newPath);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:231:            pro.sketchware.utility.FileUtil.deleteFile(oldPath);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:247:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:272:        List<String> deleted = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:277:            // Best-effort: delete all known font extensions.
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:279:                try { new File(fontDir + File.separator + n + ext).delete(); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:282:            deleted.add(n);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:284:        if (deleted.isEmpty()) {
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:285:            return err("No fonts deleted. Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:291:        sb.append("Deleted ").append(deleted.size()).append(" font(s): ").append(deleted);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:365:        List<String> duplicates = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:368:                duplicates.add(src.resName);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:392:                pro.sketchware.utility.FileUtil.copyFile(chosen, dest);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:394:                missing.add(src.resName + " (copy failed: " + t.getMessage() + ")");
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:405:            return err("Nothing imported. Duplicates: " + duplicates + ". Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:412:        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:429:                ArrayList<ProjectResourceBean> copy = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:431:                    if (o instanceof ProjectResourceBean) copy.add((ProjectResourceBean) o);
app/src/main/java/com/sketchware/ai/tools/resource/FontManageTool.java:433:                return copy;
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:23: * icon_creator - universal tool for creating, editing, and deleting
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:60: * the {@code mipmap-anydpi-v26/{name}.xml} file at edit time. Instead, it
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:92: *   <li><b>delete</b> - delete an icon (adaptive or legacy). Removes all
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:128:                        + "set_background, delete, list. Icon names must match "
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:134:                "delete", "list");
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:153:        addArrayProp(props, "names", "(delete) Array of icon names to delete.");
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:182:            case "delete":          return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:208:        // Reject duplicates (adaptive XML or legacy PNG already present).
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:211:                    + "or 'set_background' to modify, or 'delete' first.");
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:317:            return err("Icon '" + name + "' already exists. Use 'delete' first.");
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:417:            // Best-effort: delete any stale _background.png files from a
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:423:                    try { FileUtil.deleteFile(png); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:465:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:491:        List<String> deleted = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:503:                try { FileUtil.deleteFile(xmlPath); any = true; } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:511:                        try { FileUtil.deleteFile(png); any = true; } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:524:                deleted.add(name);
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:530:        if (deleted.isEmpty()) {
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:531:            return err("No icons deleted. Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java:534:        sb.append("Deleted ").append(deleted.size()).append(" icon(s): ").append(deleted);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:22: * {@link com.besome.sketch.editor.manage.image.ManageImageActivity} +
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:23: * {@link com.besome.sketch.editor.manage.image.AddImageActivity}.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:43: *   <li><b>edit</b> — rename and/or replace content of an existing image
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:46: *   <li><b>delete</b> — delete one or more images (params: {@code name}
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:61: * so the change is immediately visible in the editor.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:73:                "Manage project image resources: add, edit, delete, list, rotate, "
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:77:                "add", "edit", "delete", "list",
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:84:        addStringProp(props, "new_name", "(edit) New name to rename the image to.");
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:91:        addArrayProp(props, "names", "(delete) Array of image names to delete.");
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:130:            case "edit":                  return doEdit(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:131:            case "delete":                return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:201:        // Reject duplicates.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:203:            return err("Image '" + name + "' already exists in project. Use 'edit' or pick a different name.");
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:206:        // Physically copy/transform source into the project image dir.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:212:            // Fallback: raw copy (no transformation).
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:214:                pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:256:    //  edit
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:315:                // If extension changed, attempt to delete the old file (best-effort).
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:317:                    try { new File(oldPath).delete(); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:321:                    pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:323:                    replacedNote = " Replaced image content (raw copy, " + new File(destPath).length() + " bytes).";
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:325:                        try { new File(oldPath).delete(); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:343:                pro.sketchware.utility.FileUtil.copyFile(oldPath2, newPath2);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:344:                pro.sketchware.utility.FileUtil.deleteFile(oldPath2);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:364:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:389:        List<String> deleted = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:394:            // Try to delete the on-disk file (best-effort, multiple extensions).
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:396:                try { new File(imgDir + File.separator + n + ext).delete(); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:399:            deleted.add(n);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:401:        if (deleted.isEmpty()) {
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:402:            return err("No images deleted. Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:408:        sb.append("Deleted ").append(deleted.size()).append(" image(s): ").append(deleted);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:499:            pro.sketchware.utility.FileUtil.deleteFile(srcPath);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:500:            pro.sketchware.utility.FileUtil.copyFile(tmpPath, srcPath);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:501:            pro.sketchware.utility.FileUtil.deleteFile(tmpPath);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:566:        // Add to project list — copy file into image dir for each.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:578:        List<String> duplicates = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:581:                duplicates.add(src.resName);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:613:                    pro.sketchware.utility.FileUtil.copyFile(chosen, dest);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:615:                    missing.add(src.resName + " (copy failed: " + t2.getMessage() + ")");
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:630:            return err("Nothing imported. Duplicates: " + duplicates + ". Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:637:        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:702:                // Defensive copy so we can mutate freely.
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:703:                ArrayList<ProjectResourceBean> copy = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:705:                    if (o instanceof ProjectResourceBean) copy.add((ProjectResourceBean) o);
app/src/main/java/com/sketchware/ai/tools/resource/ImageManageTool.java:707:                return copy;
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:24: * ({@link mod.agus.jcoderz.editor.manage.resource.ManageResourceActivity}).
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:39: *   <li><b>edit</b> — overwrite an existing XML/text file (params:
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:43: *   <li><b>delete</b> — delete a file or folder (params: {@code path}).</li>
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:61:                        + "import, edit, rename, delete, list. Paths are relative "
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:64:                "create_folder", "create_xml", "import", "edit",
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:65:                "rename", "delete", "list");
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:72:        addStringProp(props, "dest_dir", "(import) Destination directory (relative to resource root) to copy into.");
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:73:        addStringProp(props, "content", "(create_xml/edit) File content (text/XML).");
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:103:            case "edit":          return doEdit(args, root);
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:105:            case "delete":        return doDelete(args, root);
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:135:        if (FileUtil.isExistFile(abs)) return err("File already exists: " + rel + ". Use 'edit' to overwrite.");
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:171:                FileUtil.copyDirectory(srcFile, new File(destPath));
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:174:                missing.add(src + " (copy failed: " + t.getMessage() + ")");
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:189:    //  edit
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:225:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/ResourceFileManageTool.java:234:        FileUtil.deleteFile(abs);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:22: * {@link com.besome.sketch.editor.manage.sound.ManageSoundActivity} +
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:23: * {@link com.besome.sketch.editor.manage.sound.AddSoundActivity} +
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:43: *   <li><b>edit</b> — rename an existing sound (params: {@code name},
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:45: *   <li><b>delete</b> — delete one or more sounds (params: {@code name}
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:53: * The tool preserves the source file extension when copying into the
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:66:                "Manage project sound resources: add, edit, delete, list, "
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:69:                "add", "edit", "delete", "list", "import_from_collection");
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:74:        addStringProp(props, "new_name", "(edit) New name to rename the sound to.");
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:78:        addArrayProp(props, "names", "(delete) Array of sound names to delete.");
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:111:            case "edit":                  return doEdit(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:112:            case "delete":                return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:164:            pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:195:    //  edit (rename only — sounds can't have their content "edited")
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:201:        if (newName == null || newName.isEmpty()) return err("new_name is required for sound edit.");
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:229:            pro.sketchware.utility.FileUtil.copyFile(oldPath, newPath);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:230:            pro.sketchware.utility.FileUtil.deleteFile(oldPath);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:246:    //  delete
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:271:        List<String> deleted = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:277:                try { new File(soundDir + File.separator + n + ext).delete(); } catch (Throwable ignored) {}
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:280:            deleted.add(n);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:282:        if (deleted.isEmpty()) {
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:283:            return err("No sounds deleted. Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:289:        sb.append("Deleted ").append(deleted.size()).append(" sound(s): ").append(deleted);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:364:        List<String> duplicates = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:367:                duplicates.add(src.resName);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:391:                pro.sketchware.utility.FileUtil.copyFile(chosen, dest);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:393:                missing.add(src.resName + " (copy failed: " + t.getMessage() + ")");
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:404:            return err("Nothing imported. Duplicates: " + duplicates + ". Missing: " + missing);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:411:        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:428:                ArrayList<ProjectResourceBean> copy = new ArrayList<>();
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:430:                    if (o instanceof ProjectResourceBean) copy.add((ProjectResourceBean) o);
app/src/main/java/com/sketchware/ai/tools/resource/SoundManageTool.java:432:                return copy;
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java:12: * <p>Replaces 12 stubs: values_xml_manage:add_array, values_xml_manage:add_bool, values_xml_manage:add_color, values_xml_manage:add_dimen, values_xml_manage:add_id, values_xml_manage:add_integer, values_xml_manage:add_string, values_xml_manage:add_style, values_xml_manage:delete_entry, values_xml_manage:import_from_default, values_xml_manage:list_entries, values_xml_manage:switch_variant
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java:15: * Hand-edit is allowed; re-running the generator will overwrite this file.
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java:21:                "Manage values XML resources: add_array, add_bool, add_color, add_dimen, add_id, add_integer, add_string, add_style, delete_entry, import_from_default, list_entries, switch_variant.",
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java:31:                "delete_entry",
app/src/main/java/com/sketchware/ai/tools/resource/ValuesXmlManageTool.java:87:            case "delete_entry": {
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:21: * ordering of palette categories in the Sketchware View editor.
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:23: * <p>Sketchware's View editor palette has multiple categories
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:119:                        + "editor: show/hide a category, get visibility, list "
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:192:            prefs.edit().putBoolean(String.format(KEY_CAT_VISIBLE, category), visible).apply();
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:300:            SharedPreferences.Editor ed = prefs.edit();
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:333:            prefs.edit().putBoolean(String.format(KEY_WIDGET_VISIBLE, widgetType), visible).apply();
app/src/main/java/com/sketchware/ai/tools/view/PaletteVisibilityManageTool.java:351:            SharedPreferences.Editor ed = prefs.edit();
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:30:public final class ViewAddWidgetTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:32:    @Override public String name() { return "view_add_widget"; }
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:87:        // "main.xml", causing the widget to be invisible in the editor.
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:162:            //       rendered in the editor, because ViewPane uses
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:188:            // when the editor reloads from disk (e.g. on layout switch or
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:191:            // cache (20 widgets) while the editor reloaded from disk (0).
app/src/main/java/com/sketchware/ai/tools/view/ViewAddWidgetTool.java:194:            // Refresh the editor so the new widget is visible.
app/src/main/java/com/sketchware/ai/tools/view/ViewDeleteWidgetTool.java:13: * view_delete_widget - delete a widget from the canvas via reflection.
app/src/main/java/com/sketchware/ai/tools/view/ViewDeleteWidgetTool.java:15:public final class ViewDeleteWidgetTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/view/ViewDeleteWidgetTool.java:17:    @Override public String name() { return "view_delete_widget"; }
app/src/main/java/com/sketchware/ai/tools/view/ViewDeleteWidgetTool.java:57:            // Persist the deletion to disk so the editor and tool stay in sync.
app/src/main/java/com/sketchware/ai/tools/view/ViewListWidgetsTool.java:22:public final class ViewListWidgetsTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/view/ViewListWidgetsTool.java:24:    @Override public String name() { return "view_list_widgets"; }
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:24: * that appear in the editor palette), not "custom view components" which
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:30: * <p>Replaces 5 stubs: palette_widget_manage:{create, edit, delete,
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:39: *   <li>For {@code edit}: verifies the widget exists first (returns a
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:41: *   <li>For {@code delete}: warns but allows if other layouts reference
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:59:                "Manage palette (custom) widget definitions: create, edit, delete, export, or import. "
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:63:                "create", "edit", "delete", "export", "import");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:74:        pDef.addProperty("description", "(create/edit) JSON definition of the widget structure. Must be a valid JSON object.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:90:            case "edit":    return doEdit(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:91:            case "delete":  return doDelete(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:113:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:115:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:119:        if (customWidgetExists(editor, name)) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:121:                    + "'. Use palette_widget_manage:edit to modify it, "
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:122:                    + "or delete it first.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:125:            SketchwareApi.invoke(editor, "a", PREFIX + name, definition);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:135:    //  edit
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:148:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:150:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:154:        if (!customWidgetExists(editor, name)) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:157:                    + "Existing custom widgets: " + listCustomWidgets(editor));
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:160:            SketchwareApi.invoke(editor, "b", PREFIX + name, definition);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:170:    //  delete
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:178:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:180:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:184:        if (!customWidgetExists(editor, name)) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:186:                    + "'. Existing custom widgets: " + listCustomWidgets(editor));
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:191:            SketchwareApi.invoke(editor, "c", PREFIX + name);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:221:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:223:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:227:        if (!customWidgetExists(editor, name)) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:229:                    + "'. Existing custom widgets: " + listCustomWidgets(editor));
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:232:            Object def = SketchwareApi.invoke(editor, "d", PREFIX + name);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:296:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:297:            if (customWidgetExists(editor, name)) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:301:            SketchwareApi.invoke(editor, "a", PREFIX + name, content);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:319:    private static boolean customWidgetExists(Object editor, String name) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:320:        if (editor == null) return false;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:322:            Object def = SketchwareApi.invoke(editor, "d", PREFIX + name);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:328:    private static List<String> listCustomWidgets(Object editor) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:330:        if (editor == null) return names;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:332:            Object all = SketchwareApi.invoke(editor, "e");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:348:     * the delete on a reflection failure).
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:353:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:354:            Object layouts = SketchwareApi.invoke(editor, "e");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageCustomWidgetTool.java:361:                        Object widgets = SketchwareApi.invoke(editor, "d", key);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:22: * delete_collection, add_image_resource_inline}.
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:31: *   <li>For {@code delete_collection}: warns but allows if the collection
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:55:                        + "delete a collection, or add an inline image resource. "
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:58:                "save_widget", "add_collection", "delete_collection", "add_image_resource_inline");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:91:            case "delete_collection":          return doDeleteCollection(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:107:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:109:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:116:            return err("No active layout. Open a layout in the View editor first.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:118:        List<String> available = listWidgetIds(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:124:            SketchwareApi.invoke(editor, "e", widgetId, collection);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:142:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:143:            SketchwareApi.invoke(editor, "f", collection);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:151:    //  delete_collection
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:159:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:161:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:166:        List<String> savedWidgets = listWidgetsInCollection(editor, collection);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:168:            SketchwareApi.invoke(editor, "g", collection);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:234:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:235:            SketchwareApi.invoke(editor, "h", name, base64ToStore);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:263:    private static List<String> listWidgetIds(Object editor, String javaName) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:265:        if (editor == null) return ids;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:267:            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:278:    private static List<String> listWidgetsInCollection(Object editor, String collection) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:280:        if (editor == null) return names;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageFavoritesTool.java:282:            Object saved = SketchwareApi.invoke(editor, "i", collection);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:21: * <p>Replaces 4 stubs: view_create_layout, view_delete_layout,
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:51: *   <li><b>delete</b>: removes the layout file and unregisters it from
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:57: *       and signals the View editor to refresh.</li>
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:104:                "Manage layout XML files in the current project: create, delete, "
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:117:                "create", "delete", "rename", "switch_active", "list");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:194:            case "delete": return doDelete(ctx, scId, name);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:237:                    + "Use switch_active to switch to it, or delete first.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:305:                // CRITICAL: save to disk so the layout survives an editor
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:357:                        Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:358:                        SketchwareApi.invoke(editor, "a", name, fabBean);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:382:        // survives an editor reload. Without this, the layout was registered
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:386:        // 8. Refresh the View editor so the new layout appears in the palette list.
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:396:        // under 'main' but the editor reads from 'main.xml' → 0 widgets.
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:403:    //  delete
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:410:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:411:            SketchwareApi.invoke(editor, "b", name);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:435:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:436:            SketchwareApi.invoke(editor, "a", oldName, newName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:473:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageLayoutTool.java:478:            Object beans = SketchwareApi.invoke(editor, "d", xmlName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:14: * add/delete/list/set_property (which have their own dedicated tools).
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:24: *   <li>For {@code clone}, auto-generates a unique {@code _copy},
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:25: *       {@code _copy_2}, {@code _copy_3}, ... suffix if {@code new_id} is
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:68:        newName.addProperty("description", "(clone) New widget ID for the clone. If omitted, a unique _copy, _copy_2, _copy_3 suffix is generated.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:95:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:97:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:101:        List<?> widgets = listWidgets(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:115:            SketchwareApi.invoke(editor, "b", javaName, widgets);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:130:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:132:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:136:        List<?> widgets = listWidgets(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:157:            SketchwareApi.invoke(editor, "a", javaName, beanClone);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:172:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:174:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:178:        List<?> widgets = listWidgets(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:186:            SketchwareApi.invoke(editor, "b", javaName, widgets);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:190:                    + "'. The View editor canvas has been refreshed.");
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:201:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:203:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:207:        List<?> widgets = listWidgets(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:247:    private static List<?> listWidgets(Object editor, String javaName) {
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:249:            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:265:        // Try _copy, then _copy_2, _copy_3, ...
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:266:        String suffix = "_copy";
app/src/main/java/com/sketchware/ai/tools/view/ViewManageWidgetTool.java:270:            suffix = "_copy_" + n;
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:17: * the View editor palette/UI: switch the palette group, switch the
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:18: * property group, or open the property editor for a widget.
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:21: * switch_property_group, open_property_editor}.
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:37: *   <li>For {@code open_property_editor}: verifies the widget exists in
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:51:                "Read-only operations on the View editor palette/UI: switch palette group, "
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:52:                        + "switch property group, or open the property editor for a widget. "
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:57:                "switch_palette_group", "switch_property_group", "open_property_editor");
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:72:                "(open_property_editor) ID of widget whose properties to edit. "
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:85:            case "open_property_editor":    return doOpenPropertyEditor(ctx, scId, args);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:99:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:100:            SketchwareApi.invoke(editor, "i", group);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:103:                    + "The View editor palette now displays the '" + group + "' category.");
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:118:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:119:            SketchwareApi.invoke(editor, "j", group);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:129:    //  open_property_editor
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:134:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:136:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:143:            return err("No active layout. Open a layout in the View editor first.");
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:145:        List<String> available = listWidgetIds(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:151:            SketchwareApi.invoke(editor, "k", widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:153:            return ok("Opened property editor for widget '" + widgetId + "' in layout '"
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:163:    private static List<String> listWidgetIds(Object editor, String javaName) {
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:165:        if (editor == null) return ids;
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteActionTool.java:167:            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:13: * the View editor. Mutating operation; NOT auto-approved.
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:18: * {@code commit_property_changes} action mutates pending property edits —
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:31:                "Commit pending property changes in the View editor. "
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:33:                        + "edits to the project; it requires user approval in ACT mode. "
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:58:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:59:            Object result = SketchwareApi.invoke(editor, "l");
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:68:                        + scId + "'. The View editor has been refreshed.");
app/src/main/java/com/sketchware/ai/tools/view/ViewPaletteCommitTool.java:71:                    + "The View editor has been refreshed and changes are now persisted.");
app/src/main/java/com/sketchware/ai/tools/view/ViewSetPropertyTool.java:55:public final class ViewSetPropertyTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/view/ViewSetPropertyTool.java:57:    @Override public String name() { return "view_set_property"; }
app/src/main/java/com/sketchware/ai/tools/view/ViewSetPropertyTool.java:178:            // For the root widget, eC.a(xmlName, viewBean) might duplicate it,
app/src/main/java/com/sketchware/ai/tools/view/ViewSetPropertyTool.java:183:            // Persist the property change to disk so the editor and tool stay in sync.
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:10: * view_undo / view_redo - undo or redo the last action in the View editor.
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:27: * — the active layout/file being edited.
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:29:public final class ViewUndoRedoTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:35:    @Override public String name() { return undo ? "view_undo" : "view_redo"; }
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:41:                ? "Undo the last action in the View editor (add/delete/move/property change). "
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:43:                : "Redo the previously undone action in the View editor. "
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:57:            return ToolResult.error("[" + name() + "] No active project (sc_id is null).");
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:61:            return ToolResult.error("[" + name() + "] No active layout. "
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:62:                    + "Open a layout in the View editor before calling "
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:63:                    + name() + ".");
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:70:            return ToolResult.error("[" + name() + "] Failed to obtain cC history singleton: "
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:74:            return ToolResult.error("[" + name() + "] cC.c(scId) returned null.");
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:87:                return ToolResult.error("[" + name() + "] No " + opLabel
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:97:                return ToolResult.error("[" + name() + "] " + opLabel
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:101:            // Refresh the View editor so the change is visible.
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:104:            return ToolResult.success("[" + name() + "] " + opLabel
app/src/main/java/com/sketchware/ai/tools/view/ViewUndoRedoTool.java:107:                    + ". The View editor has been refreshed.");
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:29: * equivalents don't exist on the editor singleton, the reflection call will
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:367:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:368:            SketchwareApi.invoke(editor, "setText", widgetId, value);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:393:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:394:            SketchwareApi.invoke(editor, "setImage", widgetId, value);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:423:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:424:            SketchwareApi.invoke(editor, nativeMethod, widgetId, normalized);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:457:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:458:            SketchwareApi.invoke(editor, "setVisibility", widgetId, androidVisibility);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:476:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:477:            Object result = SketchwareApi.invoke(editor, "getText", widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:518:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:519:            SketchwareApi.invoke(editor, "m", widgetId, anim, duration);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:548:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:549:            SketchwareApi.invoke(editor, "setTypeface", widgetId, fontName, textStyle);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:563:     * Validate widget exists, then call {@code editor.<opCode>(widgetId, value)}
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:582:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:583:            SketchwareApi.invoke(editor, opCode, widgetId, value);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:608:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:609:            SketchwareApi.invoke(editor, opCode, widgetId, normalized);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:617:    /** Validate widget exists, then call {@code editor.<opCode>(widgetId)} with no extra args. */
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:624:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:625:            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:634:    /** Getter: call {@code editor.<opCode>(widgetId)} and return the value as a string. */
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:641:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:642:            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:650:    /** Coerce value to int and call {@code editor.<opCode>(widgetId, intValue)}. */
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:666:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:667:            SketchwareApi.invoke(editor, opCode, widgetId, intValue);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:675:    /** Coerce value to long and call {@code editor.<opCode>(widgetId, longValue)}. */
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:691:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:692:            SketchwareApi.invoke(editor, opCode, widgetId, longValue);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:703:     * {@code editor.<opCode>(widgetId, arg1, arg2, ...)}.
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:724:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:728:            SketchwareApi.invoke(editor, opCode, callArgs);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:753:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:754:            SketchwareApi.invoke(editor, opCode, widgetId, argList.get(0));
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:771:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:772:            SketchwareApi.invoke(editor, opCode, widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:784:            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:785:            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:802:            return err("No active layout. Open a layout in the View editor first.");
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:804:        Object editor;
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:806:            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:810:        List<String> available = listWidgetIds(editor, javaName);
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:818:    private static List<String> listWidgetIds(Object editor, String javaName) {
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:820:        if (editor == null) return ids;
app/src/main/java/com/sketchware/ai/tools/view/ViewfuncInvokeTool.java:822:            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
app/src/main/java/com/sketchware/ai/tools/web/WebFetchTool.java:33:public final class WebFetchTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/web/WebFetchTool.java:44:    @Override public String name() { return "web_fetch"; }
app/src/main/java/com/sketchware/ai/tools/web/WebFetchTool.java:145:                body = baos.toString(java.nio.charset.StandardCharsets.UTF_8.name());
app/src/main/java/com/sketchware/ai/tools/web/WebSearchTool.java:40:public final class WebSearchTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/web/WebSearchTool.java:51:    @Override public String name() { return "web_search"; }
app/src/main/java/com/sketchware/ai/tools/web/WebSearchTool.java:96:        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
app/src/main/java/com/sketchware/ai/tools/web/WebSearchTool.java:201:                return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:16:            "view_add_widget", "view_set_property", "view_delete_widget", "view_list_widgets",
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:27:        return tool != null && !HIDDEN_AGENT_ALIASES.contains(tool.name());
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:37:        String name = tool.name();
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:48:    public static List<SketchwareTool> canonicalTools(ToolRegistry registry) {
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:59:    public static List<SketchwareTool> catalogTools(ToolRegistry registry) {
app/src/main/java/com/sketchware/ai/tools/ToolVisibilityPolicy.java:62:        for (SketchwareTool tool : canonicalTools(registry)) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:66:import com.sketchware.ai.ui.chat.sheet.AiToolsBottomSheet;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:92:    private View btnTools;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:216:     * duplicate delayed refreshes.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:227:     * rejected the duplicate run, silently losing the user's text.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:273:        btnTools = root.findViewById(R.id.btn_tools);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:299:            @Override public void onCopy(ChatMessage message) { copyMessage(message); }
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:300:            @Override public void onRetry(ChatMessage message) { retryMessage(message); }
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:301:            @Override public void onEdit(ChatMessage message) { editMessage(message); }
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:302:            @Override public void onDelete(ChatMessage message) { deleteMessage(message); }
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:390:        // (history = just refreshed the list; settings = duplicated the
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:412:        if (btnTools != null) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:413:            btnTools.setOnClickListener(v -> AiToolCatalogSheet.show(requireContext(), toolRegistry));
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:479:            aaPrefs.edit().putBoolean(AutoApproveFragment.KEY_YOLO, checked).apply();
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:748:    private void copyMessage(ChatMessage message) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:757:                showActionFeedback(R.string.ai_chat_action_copy_done);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:764:    private void retryMessage(ChatMessage message) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:779:        showActionFeedback(R.string.ai_chat_action_retry_done);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:783:    private void editMessage(ChatMessage message) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:809:    private void deleteMessage(ChatMessage message) {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:815:                .setTitle(R.string.ai_chat_action_delete)
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:816:                .setMessage(R.string.ai_chat_action_delete_confirm)
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:818:                .setPositiveButton(R.string.ai_chat_action_delete, (dialog, which) -> {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:826:                        showActionFeedback(R.string.ai_chat_action_deleted);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:835:        final int copyId = 1;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:836:        final int retryId = 2;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:837:        final int editId = 3;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:838:        final int deleteId = 4;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:839:        popup.getMenu().add(0, copyId, 0, R.string.ai_chat_action_copy);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:840:        popup.getMenu().add(0, retryId, 1, R.string.ai_chat_action_refresh);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:841:        popup.getMenu().add(0, editId, 2, R.string.ai_chat_action_edit);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:842:        popup.getMenu().add(0, deleteId, 3, R.string.ai_chat_action_delete);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:845:                case copyId: copyMessage(message); return true;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:846:                case retryId: retryMessage(message); return true;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:847:                case editId: editMessage(message); return true;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:848:                case deleteId: deleteMessage(message); return true;
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:943:        // duplicate submissions.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:990:        // real editor refresh methods. Previously these were no-op lambdas,
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:991:        // which meant every tool call reported success but the editor canvas
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:996:        // modified, so DesignActivity can SWITCH the editor to that layout if
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:999:        // and added widgets to it, but the editor was still showing 'main',
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1099:                    // completion row duplicated the whole message.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1124:                    // Same duplicate-avoidance as onComplete: only emit a
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1527:            out.add(new SketchwareToolInterface(t.name(), t.category()));
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1623:     * conversation into the current agent; long-pressing deletes it.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1655:                            .setMessage("This will permanently delete all " + tasks.size() + " saved tasks.")
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1657:                                for (TaskHistoryStore.TaskMetadata t : tasks) store.delete(t.id);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1658:                                Snackbar.make(getView(), "All tasks deleted", Snackbar.LENGTH_SHORT).show();
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1743:     * the user can't retry from the drawer; they have to retype the prompt.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1768:            // the user had no way to retry them from the drawer.
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:1789:                    // The task file may have been deleted from disk (e.g. the
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2060:        sheet.findViewById(R.id.sheet_action_delete).setOnClickListener(v -> {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2106:        prefs.edit().putStringSet(PINNED_THREADS_KEY, current).apply();
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2216:                .setTitle(R.string.ai_thread_action_delete)
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2217:                .setMessage(R.string.ai_thread_delete_confirm)
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2218:                .setPositiveButton(R.string.ai_thread_action_delete, (d, w) -> {
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2221:                        store.delete(thread.id);
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2223:                    }, "ai-thread-delete").start();
app/src/main/java/com/sketchware/ai/ui/chat/ChatFragment.java:2273:        AiToolsBottomSheet.show(a, new AiToolsBottomSheet.Callback() {
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:24:     * Return a <b>snapshot copy</b> of the current message list.
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:30:     * streaming. Returning a shallow copy guarantees a fresh wrapper while
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:82:    public synchronized boolean deleteMessage(long ts) {
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:93:    public synchronized boolean editMessage(long ts, String newText) {
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:105:        return message != null && deleteMessage(message.ts);
app/src/main/java/com/sketchware/ai/ui/chat/MessageReducer.java:191:     * on screen, appending the same text again would duplicate it.
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:33: *       with copy/refresh/edit/speak/more, token count).</li>
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:145:    private static void copyToClipboard(Context context, String value, String label) {
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:151:                Toast.makeText(context, R.string.ai_chat_action_copy_done, Toast.LENGTH_SHORT).show();
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:174:            actionCopy = v.findViewById(R.id.action_copy);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:176:            actionEdit = v.findViewById(R.id.action_edit);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:177:            actionDelete = v.findViewById(R.id.action_delete);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:185:            // Show copy action on user messages too (handy for re-running prompts)
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:192:                    else copyToClipboard(v.getContext(), m.text, "AI prompt");
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:236:            actionCopy = v.findViewById(R.id.action_copy);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:238:            actionEdit = v.findViewById(R.id.action_edit);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:239:            actionDelete = v.findViewById(R.id.action_delete);
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:311:            // Show the copy action on non-streaming assistant messages.
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatAdapter.java:322:                    else copyToClipboard(v.getContext(), m.text, "AI response");
app/src/main/java/com/sketchware/ai/ui/chat/adapter/ChatThreadsAdapter.java:35: *   <li>{@code btn_thread_more} — overflow icon (rename / delete).</li>
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiModelPickerSheet.java:517:        prefs.edit()
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheet.java:70:                TextView row = text(context, displayToolName(tool.name()) + "\n" + tool.description(), 14, Typeface.NORMAL);
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheet.java:101:        summary.append("\nTap the tools button beside Attach to browse the capabilities available in the Sketchware editor. ")
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheet.java:109:        for (SketchwareTool tool : ToolVisibilityPolicy.catalogTools(registry)) {
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheet.java:111:            // editor capabilities. They remain executable through registry
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolsBottomSheet.java:12: * {@code KelivoToolsBottomSheet}. Opens when the user taps the paperclip
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolsBottomSheet.java:23:public final class AiToolsBottomSheet {
app/src/main/java/com/sketchware/ai/ui/chat/sheet/AiToolsBottomSheet.java:32:    private AiToolsBottomSheet() {
app/src/main/java/com/sketchware/ai/ui/settings/AutoApproveFragment.java:74:            prefs.edit().putBoolean(KEY_YOLO, checked).apply();
app/src/main/java/com/sketchware/ai/ui/settings/AutoApproveFragment.java:85:                prefs.edit().putBoolean(key, checked).apply();
app/src/main/java/com/sketchware/ai/ui/settings/AutoApproveFragment.java:107:            prefs.edit().putInt(KEY_MAX_ITERATIONS, val).apply();
app/src/main/java/com/sketchware/ai/ui/settings/ExperimentalFragment.java:19:/** Experimental settings: background editing, AI image generation. */
app/src/main/java/com/sketchware/ai/ui/settings/ExperimentalFragment.java:41:        swBackgroundEditing = root.findViewById(R.id.sw_background_editing);
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java:45: * Detail editor for a single AI provider.
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java:363:                .edit()
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java:686:            final ImageView deleteBtn;
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java:694:                // Reuse the check ImageView as a delete button by toggling
app/src/main/java/com/sketchware/ai/ui/settings/ProviderDetailActivity.java:697:                deleteBtn = check;
app/src/main/java/com/sketchware/ai/ui/settings/ProvidersListFragment.java:33: * {@link ProviderDetailActivity} for that provider, where the user edits
app/src/main/java/com/sketchware/ai/ui/settings/ProvidersListFragment.java:47: * it never edits storage directly. The active provider/model used by the
app/src/main/java/com/sketchware/ai/ui/settings/ProvidersListFragment.java:98:        // Reload in case the user edited a provider detail and came back.
app/src/main/java/com/sketchware/ai/util/PathSafety.java:17: * {@code ResourceFileManageTool}; the original private copy remains there for
=== creator tool references ===
app/src/main/java/a/a/a/Fw.java:134:        creatorRuntimeProjectId = getActivity().getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/a/a/a/Fw.java:330:                                intent.putExtra("creator_runtime_project_id", creatorRuntimeProjectId);
app/src/main/java/a/a/a/ViewEditorFragment.java:171:                .getStringExtra("creator_runtime_project_id");
app/src/main/java/a/a/a/ViewEditorFragment.java:173:            intent.putExtra("creator_runtime_project_id", runtimeProjectId);
app/src/main/java/a/a/a/br.java:112:                .getStringExtra("creator_runtime_project_id");
app/src/main/java/a/a/a/br.java:114:            intent.putExtra("creator_runtime_project_id", runtimeProjectId);
app/src/main/java/a/a/a/rs.java:403:                .getStringExtra("creator_runtime_project_id");
app/src/main/java/a/a/a/rs.java:405:            intent.putExtra("creator_runtime_project_id", runtimeProjectId);
app/src/main/java/com/besome/sketch/MainDrawer.java:114:        } else if (id == R.id.creator_runtime) {
app/src/main/java/com/besome/sketch/MainDrawer.java:119:                    .putExtra("creator_runtime_project_id", document.getProjectId())
app/src/main/java/com/besome/sketch/design/DesignActivity.java:179:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:223:                ? null : getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:476:                || launchIntent.hasExtra("creator_runtime_project_id")) return;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:480:        launchIntent.putExtra("creator_runtime_project_id", document.getProjectId());
app/src/main/java/com/besome/sketch/design/DesignActivity.java:845:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:855:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1035:            intent.putExtra("creator_runtime_project_id",
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1036:                    getIntent().getStringExtra("creator_runtime_project_id"));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1213:            intent.putExtra("creator_runtime_project_id",
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1214:                    getIntent().getStringExtra("creator_runtime_project_id"));
app/src/main/java/com/besome/sketch/editor/LogicEditorActivity.java:176:                ? null : getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:182:            creatorRuntimeProjectId = intent.getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:187:            creatorRuntimeProjectId = savedInstanceState.getString("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:250:        outState.putString("creator_runtime_project_id", creatorRuntimeProjectId);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:256:            intent.putExtra("creator_runtime_project_id", creatorRuntimeProjectId);
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:314:        return getIntent().hasExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:275:            creatorRuntimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:277:            creatorRuntimeProjectId = savedInstanceState.getString("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:299:            creatorRuntimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:303:            creatorRuntimeProjectId = savedInstanceState.getString("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:341:            intent.putExtra("creator_runtime_project_id", creatorRuntimeProjectId);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:349:        newState.putString("creator_runtime_project_id", creatorRuntimeProjectId);
app/src/main/java/com/besome/sketch/lib/base/BaseAppCompatActivity.java:105:                : getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/lib/base/BaseAppCompatActivity.java:111:                && !intent.hasExtra("creator_runtime_project_id")) {
app/src/main/java/com/besome/sketch/lib/base/BaseAppCompatActivity.java:112:            intent.putExtra("creator_runtime_project_id",
app/src/main/java/com/besome/sketch/lib/base/BaseAppCompatActivity.java:113:                    getIntent().getStringExtra("creator_runtime_project_id"));
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:19:import com.sketchware.ai.tools.creator.ActivityListTool;
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:20:import com.sketchware.ai.tools.creator.CreatorRuntimeTool;
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:220:        r.register(new ActivityListTool());
app/src/main/java/com/sketchware/ai/tools/ToolRegistryInitializer.java:223:        r.register(new CreatorRuntimeTool());
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:20:public final class CreatorRuntimeTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java:21:    @Override public String name() { return "creator_runtime"; }
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java:15:public final class ActivityListTool implements SketchwareTool {
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java:16:    @Override public String name() { return "activity_list"; }
app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java:44:                result.append("- No screens exist yet. Use creator_runtime action=create_screen.");
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:230:                    .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1182:        if ("creator_runtime".equals(serviceId) && "open_editor".equals(eventName)) {
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:29:                .putExtra("creator_runtime_project_id", document.getProjectId()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:56:                environment.publish("creator_runtime", "open_editor",
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:63:                    environment.publish("creator_runtime", "open_editor",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:30:    public static final String ACTIVITY_EVENT_TARGET = "__creator_runtime_activity__";
app/src/main/java/pro/sketchware/creator/runtime/CreatorNotificationService.java:15:    private static final String DEFAULT_CHANNEL = "creator_runtime";
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:21:        preferences = this.context.getSharedPreferences("creator_runtime_" + projectId, Context.MODE_PRIVATE);
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:72:        return context.getSharedPreferences("creator_runtime_" + projectId + "_" + sanitize(storeName), Context.MODE_PRIVATE);
app/src/main/java/pro/sketchware/creator/runtime/CreatorUiService.java:29:            clipboard.setPrimaryClip(ClipData.newPlainText("creator_runtime", text));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeProjectStore.java:12:    private static final String PREFERENCES = "creator_runtime";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:47:    private static final String PREFS = "creator_runtime_legacy_bridge";
app/src/test/java/com/sketchware/ai/tools/ToolRegistryInitializerTest.java:31:        assertThat(r.has("creator_runtime")).isTrue();
app/src/test/java/com/sketchware/ai/tools/ToolRegistryInitializerTest.java:99:            "library_enable", "activity_list",
app/src/test/java/com/sketchware/ai/tools/ToolRegistryInitializerTest.java:123:        assertThat(payload).contains("\"name\":\"activity_list\"");
app/src/test/java/com/sketchware/ai/tools/ToolRegistryInitializerTest.java:130:        SketchwareTool tool = ToolRegistryInitializer.createDefault().get("activity_list");
app/src/test/java/com/sketchware/ai/tools/creator/CreatorRuntimeToolSchemaTest.java:20:public final class CreatorRuntimeToolSchemaTest {
app/src/test/java/com/sketchware/ai/tools/creator/CreatorRuntimeToolSchemaTest.java:22:        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
app/src/test/java/com/sketchware/ai/tools/creator/CreatorRuntimeToolSchemaTest.java:31:        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
app/src/test/java/com/sketchware/ai/tools/creator/CreatorRuntimeToolSchemaTest.java:99:        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
app/src/test/java/com/sketchware/ai/ui/chat/sheet/AiToolCatalogSheetTest.java:32:        assertThat(visibleNames).contains("activity_list");
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNativeWidgetTest.java:117:        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE)
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNativeWidgetTest.java:626:        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit()
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNativeWidgetTest.java:1029:        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit()
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:57:        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit().clear().commit();
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:86:                assertThat(activity.getIntent().getStringExtra("creator_runtime_project_id"))
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:185:                .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:206:                .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:230:                .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:246:                .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:286:                assertThat(activity.getIntent().getStringExtra("creator_runtime_project_id")).isNotEmpty();
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java:388:                .putExtra("creator_runtime_project_id", document.getProjectId());
