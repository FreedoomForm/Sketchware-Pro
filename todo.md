# Project TODO

- [x] Map the current AI features, dependencies, source files, and user interface flows.
- [x] Identify focused improvements to the AI-tool workflow and UI.
- [x] Add a discoverable in-chat tool catalog, grouped by the agent's registered tool categories.
- [x] Add an accessible tools action to the chat composer and improve the `/tools` summary.
- [x] Correct tool execution feedback so failures use an error state and user prompts retain copy actions.
- [x] Implement the selected improvements with Android-compatible code and resources.
- [x] Compile the Android app and run the focused AI tool-catalog unit tests successfully.
- [ ] Complete a full debug APK assembly; the sandbox terminated the long dexing stage before an APK was produced.
- [x] Trace the agent tool-call pipeline and reproduce the faulty execution behavior.
- [x] Review and correct the agent system prompt so tool contracts are precise and actionable.
- [x] Repair umbrella schemas and missing-name inference so action-specific arguments reach the intended tool.
- [x] Remove stale hidden layout-tool names from live tool descriptions injected into the system prompt.
- [x] Add regression tests for the corrected tool execution and prompt contract.
- [ ] Validate and publish the corrected AI-agent behavior.
