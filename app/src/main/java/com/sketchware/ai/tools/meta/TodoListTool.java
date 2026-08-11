package com.sketchware.ai.tools.meta;

import com.google.gson.JsonArray;
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
 * Port of Cline's {@code task_progress} parameter + FocusChainManager.
 *
 * <p>The LLM uses this tool to track multi-step tasks: add items, mark them
 * in_progress / completed / blocked, list current state, clear when done.
 * The list is shared across all tool calls in a session, so the LLM can
 * check progress before deciding the next step.
 *
 * <h2>Actions</h2>
 * <ul>
 *   <li><b>add</b> - add a new item with content (and optional priority).</li>
 *   <li><b>update</b> - update an item's status or content.</li>
 *   <li><b>complete</b> - mark an item as completed (shortcut for update status=completed).</li>
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
              + "list current state, clear when done. Always check the list before deciding the next step.",
              "meta",
              true,   // read-only (the list is in-memory, not project state)
              true,   // auto-approved
              "add", "update", "complete", "list", "clear", "remove");
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
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add":      return doAdd(args);
            case "update":   return doUpdate(args);
            case "complete": return doComplete(args);
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
        TodoItem item = new TodoItem(content, TodoItem.Status.PENDING, TodoItem.Priority.parse(priority));
        SessionState.INSTANCE.items.add(item);
        return ok("Added TODO #" + SessionState.INSTANCE.items.size() + ": " + content
                + " [priority=" + priority + "]");
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
        String newStatus = optString(args, "status");
        if (newStatus != null) {
            TodoItem.Status s = TodoItem.Status.parse(newStatus);
            if (s == null) {
                return ToolResult.error(ToolResultFormatter.invalidArgument(
                        name(), "status", "unknown value '" + newStatus + "'",
                        "one of: pending, in_progress, completed, blocked"));
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
        return ok("Marked TODO #" + idx + " as completed: " + item.content);
    }

    private ToolResult doList() {
        if (SessionState.INSTANCE.items.isEmpty()) {
            return ok("TODO list is empty. Use action=add to add items.");
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
              .append(item.content).append(" [").append(item.priority).append("]\n");
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
        public final long createdAt;
        public TodoItem(String content, Status status, Priority priority) {
            this.content = content;
            this.status = status;
            this.priority = priority;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
