package com.sketchware.ai.tools.meta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.ToolResultFormatter;
import com.sketchware.ai.tools.UniversalTool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * todo_list - AI-managed TODO list that persists across the conversation.
 * Port of Cline's {@code TodoWriteTool} / {@code task_progress} parameter
 * plus the FocusChainManager state machine.
 *
 * <p>The LLM uses this tool to track multi-step tasks: add items, mark them
 * in_progress / completed / blocked, list current state, clear when done.
 * The list is shared across all tool calls in a session, so the LLM can
 * check progress before deciding the next step.
 *
 * <h2>Cline 3.x enhancements (this revision)</h2>
 * <ul>
 *   <li><b>{@code active_form}</b> field on every item — the active-voice
 *       form displayed to the user while the item is in progress
 *       ("Adding button to main layout" rather than "Add button to main
 *       layout"). Mirrors Cline 3.x's {@code activeForm} field on
 *       {@code TodoWriteItem}. When the LLM marks an item in_progress it
 *       MUST also set {@code active_form}; the chat UI surfaces this string
 *       in the status bar so the user sees what the agent is doing right
 *       now without scrolling through tool calls.</li>
 *   <li><b>{@code write} action</b> — state-replacement semantics: takes a
 *       complete {@code todos} array and replaces the entire list in one
 *       call. Mirrors Cline 3.x's {@code TodoWriteTool}, which always
 *       takes the full list (no incremental add/update). The LLM prefers
 *       {@code write} for multi-item rewrites because it's atomic — no
 *       intermediate states visible to the user.</li>
 *   <li><b>Single-in-progress enforcement</b> — at most one item can be
 *       in_progress at a time. The {@code write} and {@code update}
 *       actions reject payloads with multiple in_progress items. Mirrors
 *       Cline 3.x's {@code validateTodoList} invariant.</li>
 *   <li><b>{@code requireActiveTodo()} static guard</b> — returns true if
 *       at least one item is in_progress. The {@link
 *       com.sketchware.ai.agent.AgentRuntime} consults this guard before
 *       executing any non-meta tool; if no item is in_progress, the
 *       runtime injects a warning telling the LLM to set one. Mirrors
 *       Cline 3.x's {@code requireTodosInRange} hook.</li>
 * </ul>
 *
 * <h2>Actions</h2>
 * <ul>
 *   <li><b>add</b> - add a new item with content (and optional priority/active_form).</li>
 *   <li><b>update</b> - update an item's status/content/active_form.</li>
 *   <li><b>complete</b> - mark an item as completed (shortcut for update status=completed).</li>
 *   <li><b>write</b> - replace the entire list with a new todos array (atomic).</li>
 *   <li><b>list</b> - return the current TODO list as markdown.</li>
 *   <li><b>clear</b> - remove all items.</li>
 *   <li><b>remove</b> - delete a single item by index.</li>
 * </ul>
 *
 * <p>The list is stored in a static singleton (one per process). This is
 * intentional — the LLM may run multiple tools in sequence and the list
 * should persist across them. Use {@link #resetSession()} to clear between
 * unrelated conversations.
 */
public final class TodoListTool extends UniversalTool {

    public TodoListTool() {
        super("todo_list",
              "Manage a TODO list that persists across tool calls in this conversation. "
              + "Use it to track multi-step tasks: add items, mark them in_progress / completed / blocked, "
              + "list current state, clear when done. Always check the list before deciding the next step. "
              + "EXACTLY ONE item must be in_progress at any time — before calling any other tool, ensure "
              + "one todo is marked in_progress. Use action=write to atomically replace the entire list "
              + "with a new todos array (preferred for multi-item rewrites).",
              "meta",
              true,   // read-only (the list is in-memory, not project state)
              true,   // auto-approved
              "add", "update", "complete", "write", "list", "clear", "remove");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        JsonObject content = new JsonObject();
        content.addProperty("type", "string");
        content.addProperty("description", "TODO item content (required for add, optional for update).");
        props.add("content", content);

        JsonObject index = new JsonObject();
        index.addProperty("type", "integer");
        index.addProperty("description", "1-based index of the item to update/complete/remove.");
        props.add("index", index);

        JsonObject status = new JsonObject();
        status.addProperty("type", "string");
        status.addProperty("description", "New status for the item. One of: pending, in_progress, completed, blocked.");
        JsonArray statusEnum = new JsonArray();
        statusEnum.add("pending");
        statusEnum.add("in_progress");
        statusEnum.add("completed");
        statusEnum.add("blocked");
        status.add("enum", statusEnum);
        props.add("status", status);

        JsonObject priority = new JsonObject();
        priority.addProperty("type", "string");
        priority.addProperty("description", "Priority hint for the item. One of: low, medium, high.");
        JsonArray priorityEnum = new JsonArray();
        priorityEnum.add("low");
        priorityEnum.add("medium");
        priorityEnum.add("high");
        priority.add("enum", priorityEnum);
        props.add("priority", priority);

        // active_form — Cline 3.x active-voice display string.
        JsonObject activeForm = new JsonObject();
        activeForm.addProperty("type", "string");
        activeForm.addProperty("description",
                "Active-voice form of the item, displayed to the user while the item is in progress "
                + "(e.g. 'Adding button to main layout'). Required when status=in_progress. "
                + "Should be short (<= 80 chars), present-progressive, action-focused.");
        props.add("active_form", activeForm);

        // todos — array form for the write action (state-replacement).
        JsonObject todos = new JsonObject();
        todos.addProperty("type", "array");
        todos.addProperty("description",
                "Complete TODO list for the write action. Replaces the entire list atomically. "
                + "Each element is an object with: content (string, required), status (enum), "
                + "priority (enum, optional), active_form (string, optional, required when status=in_progress). "
                + "EXACTLY ONE item in the array may have status=in_progress.");
        JsonObject itemSchema = new JsonObject();
        itemSchema.addProperty("type", "object");
        JsonObject itemProps = new JsonObject();
        JsonObject itemContent = new JsonObject();
        itemContent.addProperty("type", "string");
        itemProps.add("content", itemContent);
        JsonObject itemStatus = new JsonObject();
        itemStatus.addProperty("type", "string");
        JsonArray itemStatusEnum = new JsonArray();
        itemStatusEnum.add("pending");
        itemStatusEnum.add("in_progress");
        itemStatusEnum.add("completed");
        itemStatusEnum.add("blocked");
        itemStatus.add("enum", itemStatusEnum);
        itemProps.add("status", itemStatus);
        JsonObject itemPriority = new JsonObject();
        itemPriority.addProperty("type", "string");
        JsonArray itemPriorityEnum = new JsonArray();
        itemPriorityEnum.add("low");
        itemPriorityEnum.add("medium");
        itemPriorityEnum.add("high");
        itemPriority.add("enum", itemPriorityEnum);
        itemProps.add("priority", itemPriority);
        JsonObject itemActiveForm = new JsonObject();
        itemActiveForm.addProperty("type", "string");
        itemProps.add("active_form", itemActiveForm);
        itemSchema.add("properties", itemProps);
        itemSchema.addProperty("additionalProperties", false);
        todos.add("items", itemSchema);
        props.add("todos", todos);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add":      return doAdd(args);
            case "update":   return doUpdate(args);
            case "complete": return doComplete(args);
            case "write":    return doWrite(args);
            case "list":     return doList();
            case "clear":    return doClear();
            case "remove":   return doRemove(args);
            default:         return err("Unknown action: " + action);
        }
    }

    private ToolResult doAdd(JsonObject args) {
        String content = optString(args, "content");
        if (content == null || content.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.missingArgument(
                    name(), "content", "describe the TODO item"));
        }
        String priority = optString(args, "priority", "medium");
        String activeForm = optString(args, "active_form");
        String statusStr = optString(args, "status", "pending");
        TodoItem.Status status = TodoItem.Status.parse(statusStr);
        if (status == null) status = TodoItem.Status.PENDING;

        // Single-in-progress enforcement.
        if (status == TodoItem.Status.IN_PROGRESS) {
            String conflict = findInProgressContent();
            if (conflict != null) {
                return ToolResult.error("Cannot add a new in_progress item: '" + conflict
                        + "' is already in_progress. Mark it completed/blocked first, "
                        + "or use action=write to replace the whole list.");
            }
            if (activeForm == null || activeForm.isEmpty()) {
                return ToolResult.error(ToolResultFormatter.invalidArgument(
                        name(), "active_form", "missing",
                        "active_form is required when status=in_progress"));
            }
        }

        TodoItem item = new TodoItem(content, status, TodoItem.Priority.parse(priority), activeForm);
        SessionState.INSTANCE.items.add(item);
        return ok("Added TODO #" + SessionState.INSTANCE.items.size() + ": " + content
                + " [status=" + status + ", priority=" + priority + "]");
    }

    private ToolResult doUpdate(JsonObject args) {
        int idx = optInt(args, "index", -1);
        if (idx < 1 || idx > SessionState.INSTANCE.items.size()) {
            return ToolResult.error(ToolResultFormatter.invalidArgument(
                    name(), "index", "out of range",
                    "1 to " + SessionState.INSTANCE.items.size()));
        }
        TodoItem item = SessionState.INSTANCE.items.get(idx - 1);
        String newContent = optString(args, "content");
        if (newContent != null && !newContent.isEmpty()) item.content = newContent;
        String newActiveForm = optString(args, "active_form");
        if (newActiveForm != null) item.activeForm = newActiveForm;
        String newStatus = optString(args, "status");
        if (newStatus != null) {
            TodoItem.Status s = TodoItem.Status.parse(newStatus);
            if (s == null) {
                return ToolResult.error(ToolResultFormatter.invalidArgument(
                        name(), "status", "unknown value '" + newStatus + "'",
                        "one of: pending, in_progress, completed, blocked"));
            }
            // Single-in-progress enforcement.
            if (s == TodoItem.Status.IN_PROGRESS) {
                String conflict = findInProgressContent();
                if (conflict != null && !conflict.equals(item.content)) {
                    return ToolResult.error("Cannot mark this item in_progress: '" + conflict
                            + "' is already in_progress. Mark it completed/blocked first.");
                }
                // Require active_form when transitioning to in_progress.
                String af = item.activeForm != null ? item.activeForm : newActiveForm;
                if ((af == null || af.isEmpty())) {
                    return ToolResult.error(ToolResultFormatter.invalidArgument(
                            name(), "active_form", "missing",
                            "active_form is required when status=in_progress. "
                            + "Set active_form to a present-progressive description "
                            + "of what you are doing (e.g. 'Adding button to main layout')."));
                }
            }
            item.status = s;
        }
        String newPriority = optString(args, "priority");
        if (newPriority != null) {
            TodoItem.Priority p = TodoItem.Priority.parse(newPriority);
            if (p == null) {
                return ToolResult.error(ToolResultFormatter.invalidArgument(
                        name(), "priority", "unknown value '" + newPriority + "'",
                        "one of: low, medium, high"));
            }
            item.priority = p;
        }
        return ok("Updated TODO #" + idx + ": " + item.content + " [status=" + item.status + "]");
    }

    private ToolResult doComplete(JsonObject args) {
        int idx = optInt(args, "index", -1);
        if (idx < 1 || idx > SessionState.INSTANCE.items.size()) {
            return ToolResult.error(ToolResultFormatter.invalidArgument(
                    name(), "index", "out of range",
                    "1 to " + SessionState.INSTANCE.items.size()));
        }
        TodoItem item = SessionState.INSTANCE.items.get(idx - 1);
        item.status = TodoItem.Status.COMPLETED;
        // Clear active_form — no longer in progress.
        item.activeForm = null;
        return ok("Marked TODO #" + idx + " as completed: " + item.content);
    }

    /**
     * Atomic state-replacement: replace the entire list with a new todos
     * array. Mirrors Cline 3.x's TodoWriteTool semantics — the LLM
     * always sends the full list, never incremental updates.
     */
    private ToolResult doWrite(JsonObject args) {
        if (!args.has("todos") || !args.get("todos").isJsonArray()) {
            return ToolResult.error(ToolResultFormatter.missingArgument(
                    name(), "todos", "an array of todo items"));
        }
        JsonArray arr = args.getAsJsonArray("todos");
        List<TodoItem> newItems = new ArrayList<>(arr.size());
        int inProgressCount = 0;
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                return ToolResult.error("Each todos element must be an object; got: " + el);
            }
            JsonObject o = el.getAsJsonObject();
            String content = o.has("content") && !o.get("content").isJsonNull()
                    ? o.get("content").getAsString() : null;
            if (content == null || content.isEmpty()) {
                return ToolResult.error("Each todos element requires a non-empty 'content' field.");
            }
            String statusStr = o.has("status") && !o.get("status").isJsonNull()
                    ? o.get("status").getAsString() : "pending";
            TodoItem.Status status = TodoItem.Status.parse(statusStr);
            if (status == null) {
                return ToolResult.error("Unknown status '" + statusStr + "' for item: " + content);
            }
            String priorityStr = o.has("priority") && !o.get("priority").isJsonNull()
                    ? o.get("priority").getAsString() : "medium";
            String activeForm = o.has("active_form") && !o.get("active_form").isJsonNull()
                    ? o.get("active_form").getAsString() : null;

            if (status == TodoItem.Status.IN_PROGRESS) {
                inProgressCount++;
                if (activeForm == null || activeForm.isEmpty()) {
                    return ToolResult.error("Item '" + content
                            + "' is marked in_progress but has no active_form. "
                            + "active_form is required for in_progress items.");
                }
            }
            newItems.add(new TodoItem(content, status,
                    TodoItem.Priority.parse(priorityStr), activeForm));
        }
        if (inProgressCount > 1) {
            return ToolResult.error("EXACTLY ONE item may be in_progress at a time; "
                    + "the provided todos array has " + inProgressCount
                    + " in_progress items. Mark all but one as pending/completed/blocked.");
        }
        SessionState.INSTANCE.items.clear();
        SessionState.INSTANCE.items.addAll(newItems);
        return ok("Replaced TODO list with " + newItems.size() + " item(s).\n" + renderList());
    }

    private ToolResult doList() {
        if (SessionState.INSTANCE.items.isEmpty()) {
            return ok("TODO list is empty. Use action=add to add items, "
                    + "or action=write with a todos array to set the full list.");
        }
        return ToolResult.success(renderList());
    }

    private ToolResult doClear() {
        int n = SessionState.INSTANCE.items.size();
        SessionState.INSTANCE.items.clear();
        return ok("Cleared " + n + " TODO item(s).");
    }

    private ToolResult doRemove(JsonObject args) {
        int idx = optInt(args, "index", -1);
        if (idx < 1 || idx > SessionState.INSTANCE.items.size()) {
            return ToolResult.error(ToolResultFormatter.invalidArgument(
                    name(), "index", "out of range",
                    "1 to " + SessionState.INSTANCE.items.size()));
        }
        TodoItem removed = SessionState.INSTANCE.items.remove(idx - 1);
        return ok("Removed TODO #" + idx + ": " + removed.content);
    }

    /** Render the TODO list as a markdown checklist. */
    public static String renderList() {
        if (SessionState.INSTANCE.items.isEmpty()) return "(empty)";
        StringBuilder sb = new StringBuilder();
        sb.append("TODO list (").append(SessionState.INSTANCE.items.size()).append(" items):\n");
        int completed = 0, inProgress = 0, blocked = 0;
        for (int i = 0; i < SessionState.INSTANCE.items.size(); i++) {
            TodoItem item = SessionState.INSTANCE.items.get(i);
            String checkbox;
            switch (item.status) {
                case COMPLETED:   checkbox = "[x]"; completed++; break;
                case IN_PROGRESS: checkbox = "[~]"; inProgress++; break;
                case BLOCKED:     checkbox = "[!]"; blocked++; break;
                default:          checkbox = "[ ]"; break;
            }
            sb.append("  ").append(i + 1).append(". ").append(checkbox).append(" ")
              .append(item.content).append(" [").append(item.priority).append("]");
            if (item.status == TodoItem.Status.IN_PROGRESS && item.activeForm != null) {
                sb.append("  \u2192 ").append(item.activeForm);
            }
            sb.append('\n');
        }
        sb.append("\nSummary: ").append(completed).append(" completed, ")
          .append(inProgress).append(" in progress, ")
          .append(SessionState.INSTANCE.items.size() - completed - inProgress - blocked)
          .append(" pending, ").append(blocked).append(" blocked.");
        return sb.toString();
    }

    /** Reset the session TODO list (call when starting a new conversation). */
    public static void resetSession() {
        SessionState.INSTANCE.items.clear();
    }

    /** Get a snapshot of the current TODO items. */
    public static List<TodoItem> snapshot() {
        return new ArrayList<>(SessionState.INSTANCE.items);
    }

    /**
     * Return the active_form of the in_progress item, or null if no item
     * is in_progress. Used by the chat UI to display the current activity
     * in the status bar.
     */
    public static String currentActiveForm() {
        for (TodoItem item : SessionState.INSTANCE.items) {
            if (item.status == TodoItem.Status.IN_PROGRESS) {
                return item.activeForm != null ? item.activeForm : item.content;
            }
        }
        return null;
    }

    /**
     * Return true if at least one TODO item is currently in_progress.
     * The agent runtime consults this guard before executing any non-meta
     * tool; if it returns false, the runtime injects a warning telling
     * the LLM to mark an item in_progress first. Mirrors Cline 3.x's
     * {@code requireTodosInRange} hook.
     */
    public static boolean hasActiveTodo() {
        for (TodoItem item : SessionState.INSTANCE.items) {
            if (item.status == TodoItem.Status.IN_PROGRESS) return true;
        }
        return false;
    }

    /**
     * Return true if the TODO list is empty. Used by the agent runtime
     * to decide whether to nudge the LLM to create a list at the start
     * of a multi-step task.
     */
    public static boolean isEmpty() {
        return SessionState.INSTANCE.items.isEmpty();
    }

    /** Find the content of the currently in_progress item, or null. */
    private static String findInProgressContent() {
        for (TodoItem item : SessionState.INSTANCE.items) {
            if (item.status == TodoItem.Status.IN_PROGRESS) return item.content;
        }
        return null;
    }

    // ---- Inner types ----

    /** Singleton session state (process-wide). */
    private enum SessionState {
        INSTANCE;
        final CopyOnWriteArrayList<TodoItem> items = new CopyOnWriteArrayList<>();
    }

    public static final class TodoItem {
        public enum Status { PENDING, IN_PROGRESS, COMPLETED, BLOCKED;
            public static Status parse(String s) {
                if (s == null) return null;
                switch (s.toLowerCase()) {
                    case "pending":     return PENDING;
                    case "in_progress": return IN_PROGRESS;
                    case "completed":   return COMPLETED;
                    case "blocked":     return BLOCKED;
                }
                return null;
            }
        }
        public enum Priority { LOW, MEDIUM, HIGH;
            public static Priority parse(String s) {
                if (s == null) return MEDIUM;
                switch (s.toLowerCase()) {
                    case "low":    return LOW;
                    case "high":   return HIGH;
                    default:       return MEDIUM;
                }
            }
        }
        public volatile String content;
        public volatile Status status;
        public volatile Priority priority;
        /** Active-voice form displayed while in_progress (Cline 3.x). */
        public volatile String activeForm;
        public final long createdAt;
        public TodoItem(String content, Status status, Priority priority, String activeForm) {
            this.content = content;
            this.status = status;
            this.priority = priority;
            this.activeForm = activeForm;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
