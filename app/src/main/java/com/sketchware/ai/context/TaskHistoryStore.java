package com.sketchware.ai.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.agent.AgentMessage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Persists past conversation tasks to JSON files for resume / branch.
 * Mirrors Cline's {@code shared/HistoryItem.ts} + task-history controller.
 *
 * <p>Each saved task is stored as a single JSON file in the app's private
 * storage: {@code <filesDir>/ai_task_history/<taskId>.json}.
 *
 * <p>The JSON contains:
 * <ul>
 *   <li>{@code id} - unique task ID (ULID-like timestamp + random).</li>
 *   <li>{@code createdAt} - epoch millis.</li>
 *   <li>{@code updatedAt} - epoch millis (updated on every save).</li>
 *   <li>{@code projectScId} - the Sketchware project ID this task belongs to.</li>
 *   <li>{@code projectName} - human-readable project name.</li>
 *   <li>{@code firstUserMessage} - the first user message (used as task title).</li>
 *   <li>{@code lastUserMessage} - the most recent user message.</li>
 *   <li>{@code messageCount} - total messages in the conversation.</li>
 *   <li>{@code conversation} - a JSON array of serialized {@link AgentMessage}s.</li>
 * </ul>
 *
 * <p>Thread-safety: instances are safe for use from a single thread. The
 * caller (typically the UI thread) should serialize access.
 */
public final class TaskHistoryStore {

    private final File historyDir;
    private final Gson gson;

    public TaskHistoryStore(File filesDir) {
        this.historyDir = new File(filesDir, "ai_task_history");
        if (!historyDir.exists()) historyDir.mkdirs();
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /** Metadata for a saved task (without the full conversation). */
    public static final class TaskMetadata {
        public final String id;
        public final long createdAt;
        public final long updatedAt;
        public final String projectScId;
        public final String projectName;
        public final String firstUserMessage;
        public final String lastUserMessage;
        public final int messageCount;
        /** Provider id used in the last message of this chat (may be null for old chats). */
        public final String lastProviderId;
        /** Model id used in the last message of this chat (may be null for old chats). */
        public final String lastModelId;

        public TaskMetadata(String id, long createdAt, long updatedAt, String projectScId,
                            String projectName, String firstUserMessage, String lastUserMessage,
                            int messageCount) {
            this(id, createdAt, updatedAt, projectScId, projectName,
                    firstUserMessage, lastUserMessage, messageCount, null, null);
        }

        public TaskMetadata(String id, long createdAt, long updatedAt, String projectScId,
                            String projectName, String firstUserMessage, String lastUserMessage,
                            int messageCount, String lastProviderId, String lastModelId) {
            this.id = id;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.projectScId = projectScId;
            this.projectName = projectName;
            this.firstUserMessage = firstUserMessage;
            this.lastUserMessage = lastUserMessage;
            this.messageCount = messageCount;
            this.lastProviderId = lastProviderId;
            this.lastModelId = lastModelId;
        }
    }

    /** Save the current conversation as a new task. Returns the task ID. */
    public String save(LinkedList<AgentMessage> conversation, String projectScId, String projectName) throws IOException {
        return save(conversation, projectScId, projectName, null, null);
    }

    /**
     * Save the current conversation as a new task, recording the provider/model
     * used so the chat list can show the provider's emblem for this chat.
     */
    public String save(LinkedList<AgentMessage> conversation, String projectScId,
                       String projectName, String providerId, String modelId) throws IOException {
        String id = generateId();
        long now = System.currentTimeMillis();
        String firstUser = null, lastUser = null;
        for (AgentMessage m : conversation) {
            if (AgentMessage.ROLE_USER.equals(m.role) && m.text != null && !m.text.isEmpty()) {
                if (firstUser == null) firstUser = m.text;
                lastUser = m.text;
            }
        }
        if (firstUser == null) firstUser = "(no user message)";
        if (lastUser == null) lastUser = firstUser;

        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("createdAt", now);
        root.addProperty("updatedAt", now);
        root.addProperty("projectScId", projectScId);
        root.addProperty("projectName", projectName);
        root.addProperty("firstUserMessage", truncate(firstUser, 200));
        root.addProperty("lastUserMessage", truncate(lastUser, 200));
        root.addProperty("messageCount", conversation.size());
        if (providerId != null) root.addProperty("lastProviderId", providerId);
        if (modelId != null) root.addProperty("lastModelId", modelId);

        JsonArray conv = new JsonArray();
        for (AgentMessage m : conversation) conv.add(serialize(m));
        root.add("conversation", conv);

        File file = new File(historyDir, id + ".json");
        Files.write(file.toPath(), gson.toJson(root).getBytes(StandardCharsets.UTF_8));
        return id;
    }

    /** Update an existing task with the latest conversation state. */
    public void update(String taskId, LinkedList<AgentMessage> conversation) throws IOException {
        update(taskId, conversation, null, null);
    }

    /**
     * Update an existing task, also refreshing the provider/model fields so
     * the chat list emblem stays in sync with whatever model the user switched
     * to during this chat.
     */
    public void update(String taskId, LinkedList<AgentMessage> conversation,
                       String providerId, String modelId) throws IOException {
        File file = new File(historyDir, taskId + ".json");
        if (!file.exists()) throw new IOException("Task not found: " + taskId);
        JsonObject root = gson.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
        root.addProperty("updatedAt", System.currentTimeMillis());
        root.addProperty("messageCount", conversation.size());
        if (providerId != null) root.addProperty("lastProviderId", providerId);
        if (modelId != null) root.addProperty("lastModelId", modelId);

        String lastUser = null;
        for (AgentMessage m : conversation) {
            if (AgentMessage.ROLE_USER.equals(m.role) && m.text != null && !m.text.isEmpty()) {
                lastUser = m.text;
            }
        }
        if (lastUser != null) root.addProperty("lastUserMessage", truncate(lastUser, 200));

        JsonArray conv = new JsonArray();
        for (AgentMessage m : conversation) conv.add(serialize(m));
        root.add("conversation", conv);

        Files.write(file.toPath(), gson.toJson(root).getBytes(StandardCharsets.UTF_8));
    }

    /** Load a saved task's conversation. Returns null if not found. */
    public LinkedList<AgentMessage> load(String taskId) throws IOException {
        File file = new File(historyDir, taskId + ".json");
        if (!file.exists()) return null;
        JsonObject root = gson.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
        JsonArray conv = root.getAsJsonArray("conversation");
        LinkedList<AgentMessage> messages = new LinkedList<>();
        for (int i = 0; i < conv.size(); i++) {
            messages.add(deserialize(conv.get(i).getAsJsonObject()));
        }
        return messages;
    }

    /** List all saved tasks (most recent first). */
    public List<TaskMetadata> list() {
        List<TaskMetadata> result = new ArrayList<>();
        File[] files = historyDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return result;
        for (File f : files) {
            try {
                JsonObject root = gson.fromJson(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8), JsonObject.class);
                result.add(new TaskMetadata(
                        root.has("id") ? root.get("id").getAsString() : f.getName(),
                        root.has("createdAt") ? root.get("createdAt").getAsLong() : f.lastModified(),
                        root.has("updatedAt") ? root.get("updatedAt").getAsLong() : f.lastModified(),
                        root.has("projectScId") ? root.get("projectScId").getAsString() : null,
                        root.has("projectName") ? root.get("projectName").getAsString() : null,
                        root.has("firstUserMessage") ? root.get("firstUserMessage").getAsString() : "(no title)",
                        root.has("lastUserMessage") ? root.get("lastUserMessage").getAsString() : null,
                        root.has("messageCount") ? root.get("messageCount").getAsInt() : 0,
                        root.has("lastProviderId") && !root.get("lastProviderId").isJsonNull()
                                ? root.get("lastProviderId").getAsString() : null,
                        root.has("lastModelId") && !root.get("lastModelId").isJsonNull()
                                ? root.get("lastModelId").getAsString() : null));
            } catch (Throwable ignored) {
                // Skip malformed files.
            }
        }
        result.sort(Comparator.comparingLong((TaskMetadata m) -> m.updatedAt).reversed());
        return result;
    }

    /** Delete a saved task. Returns true if deleted. */
    public boolean delete(String taskId) {
        File file = new File(historyDir, taskId + ".json");
        return file.exists() && file.delete();
    }

    /** Delete all tasks older than the given cutoff (epoch millis). */
    public int deleteOlderThan(long cutoffMillis) {
        int n = 0;
        File[] files = historyDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return 0;
        for (File f : files) {
            if (f.lastModified() < cutoffMillis) {
                if (f.delete()) n++;
            }
        }
        return n;
    }

    /** Get the storage directory (for UI inspection / cleanup). */
    public File getHistoryDir() { return historyDir; }

    // ---- Serialization helpers ----

    private JsonObject serialize(AgentMessage m) {
        JsonObject o = new JsonObject();
        o.addProperty("role", m.role);
        if (m.text != null) o.addProperty("text", m.text);
        if (m.reasoning != null) o.addProperty("reasoning", m.reasoning);
        if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (AgentMessage.ToolCall tc : m.toolCalls) {
                JsonObject tco = new JsonObject();
                tco.addProperty("id", tc.id);
                tco.addProperty("name", tc.name);
                tco.addProperty("argumentsJson", tc.argumentsJson);
                arr.add(tco);
            }
            o.add("toolCalls", arr);
        }
        if (m.toolResults != null && !m.toolResults.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                JsonObject ro = new JsonObject();
                ro.addProperty("toolCallId", r.toolCallId);
                ro.addProperty("toolName", r.toolName);
                ro.addProperty("output", r.output);
                ro.addProperty("isError", r.isError);
                arr.add(ro);
            }
            o.add("toolResults", arr);
        }
        if (m.images != null && !m.images.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (String img : m.images) arr.add(img);
            o.add("images", arr);
        }
        return o;
    }

    private AgentMessage deserialize(JsonObject o) {
        String role = o.has("role") ? o.get("role").getAsString() : AgentMessage.ROLE_USER;
        String text = o.has("text") ? o.get("text").getAsString() : null;
        String reasoning = o.has("reasoning") ? o.get("reasoning").getAsString() : null;
        List<AgentMessage.ToolCall> toolCalls = null;
        if (o.has("toolCalls")) {
            toolCalls = new ArrayList<>();
            JsonArray arr = o.getAsJsonArray("toolCalls");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject tco = arr.get(i).getAsJsonObject();
                toolCalls.add(new AgentMessage.ToolCall(
                        tco.has("id") ? tco.get("id").getAsString() : null,
                        tco.has("name") ? tco.get("name").getAsString() : null,
                        tco.has("argumentsJson") ? tco.get("argumentsJson").getAsString() : null));
            }
        }
        List<AgentMessage.ToolResultContent> toolResults = null;
        if (o.has("toolResults")) {
            toolResults = new ArrayList<>();
            JsonArray arr = o.getAsJsonArray("toolResults");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject ro = arr.get(i).getAsJsonObject();
                toolResults.add(new AgentMessage.ToolResultContent(
                        ro.has("toolCallId") ? ro.get("toolCallId").getAsString() : null,
                        ro.has("toolName") ? ro.get("toolName").getAsString() : null,
                        ro.has("output") ? ro.get("output").getAsString() : null,
                        ro.has("isError") && ro.get("isError").getAsBoolean()));
            }
        }
        List<String> images = null;
        if (o.has("images")) {
            images = new ArrayList<>();
            JsonArray arr = o.getAsJsonArray("images");
            for (int i = 0; i < arr.size(); i++) images.add(arr.get(i).getAsString());
        }
        // Use reflection-free reconstruction via package-private constructor path.
        // AgentMessage's constructor is private, so we use the static factories + a wrapper.
        AgentMessage msg;
        if (AgentMessage.ROLE_SYSTEM.equals(role)) {
            msg = AgentMessage.system(text == null ? "" : text);
        } else if (AgentMessage.ROLE_ASSISTANT.equals(role)) {
            msg = AgentMessage.assistant(text, reasoning, toolCalls);
        } else if (toolResults != null) {
            msg = AgentMessage.toolResult(toolResults);
        } else if (images != null) {
            msg = AgentMessage.userWithImages(text == null ? "" : text, images);
        } else {
            msg = AgentMessage.user(text == null ? "" : text);
        }
        return msg;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Generate a task ID: timestamp-base32 + random suffix. */
    private static String generateId() {
        long now = System.currentTimeMillis();
        String base = Long.toString(now, 36);
        String rand = Integer.toHexString((int) (Math.random() * 0xFFFF));
        return base + "-" + rand;
    }
}
