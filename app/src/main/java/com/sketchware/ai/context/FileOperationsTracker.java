package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Track file operations (read / written / edited) across a conversation so
 * the compaction summary can carry them forward as a {@code <files>} tag.
 * Ported from oh-my-pi's
 * {@code packages/agent/src/compaction/utils.ts} (extractFileOpsFromMessage,
 * computeFileLists, formatFileOperations, upsertFileOperations).
 *
 * <p>Tool name conventions:
 * <ul>
 *   <li>Reads: {@code read_file}, {@code list_files}, {@code search_files},
 *       {@code view_list_widgets}, {@code view_list_layouts},
 *       {@code java_read_file}, {@code read}</li>
 *   <li>Writes: {@code write_file}, {@code create_file},
 *       {@code view_manage_layout} with action {@code create}</li>
 *   <li>Edits: {@code edit_file}, {@code apply_patch}, {@code diff_edit_file},
 *       {@code java_edit_file}, {@code java_modify_class},
 *       {@code view_add_widget}, {@code view_set_property},
 *       {@code view_delete_widget}, {@code view_manage_widget}</li>
 * </ul>
 *
 * <p>The result is rendered as a single {@code <files>} tag containing a
 * grouped, prefix-folded directory tree with {@code (Read)}, {@code (Write)},
 * or {@code (RW)} markers per file. Mirrors the TypeScript
 * {@code formatFileOperations} shape, capped at 20 files.
 */
public final class FileOperationsTracker {

    /** Maximum number of files to render in the {@code <files>} tag. */
    private static final int FILE_OPERATION_SUMMARY_LIMIT = 20;

    private final Set<String> read = new LinkedHashSet<>();
    private final Set<String> written = new LinkedHashSet<>();
    private final Set<String> edited = new LinkedHashSet<>();

    public FileOperationsTracker() {}

    /**
     * Extract file operations from a single assistant message's tool calls.
     */
    public void extractFrom(AgentMessage message) {
        if (message == null) return;
        if (!AgentMessage.ROLE_ASSISTANT.equals(message.role)) return;
        if (message.toolCalls == null) return;

        for (AgentMessage.ToolCall tc : message.toolCalls) {
            String path = extractPath(tc);
            if (path == null) continue;
            // Skip internal URIs and web URLs — they aren't re-groundable files.
            if (isUrlScheme(path)) continue;

            String tool = tc.name == null ? "" : tc.name.toLowerCase();
            if (isReadTool(tool)) {
                read.add(stripReadSelector(path));
            } else if (isWriteTool(tool)) {
                written.add(path);
            } else if (isEditTool(tool)) {
                edited.add(path);
            }
        }
    }

    /**
     * Merge another tracker's sets into this one. Used when carrying
     * forward cumulative file ops from a previous compaction's details.
     */
    public void mergeFrom(FileOperationsTracker other) {
        if (other == null) return;
        read.addAll(other.read);
        written.addAll(other.written);
        edited.addAll(other.edited);
    }

    /**
     * Compute the final file lists: readFiles (read-only, not modified)
     * and modifiedFiles (written or edited).
     */
    public ComputedFileLists computeLists() {
        Set<String> modified = new LinkedHashSet<>();
        for (String f : edited) if (!isUrlScheme(f)) modified.add(f);
        for (String f : written) if (!isUrlScheme(f)) modified.add(f);

        List<String> readOnly = new ArrayList<>();
        for (String f : read) {
            if (isUrlScheme(f)) continue;
            if (modified.contains(f)) continue;
            readOnly.add(f);
        }
        Collections.sort(readOnly);

        List<String> modifiedSorted = new ArrayList<>(modified);
        Collections.sort(modifiedSorted);

        return new ComputedFileLists(readOnly, modifiedSorted);
    }

    /**
     * Format the file operations as a single {@code <files>} tag. Returns
     * an empty string if no files were tracked.
     */
    public String format() {
        ComputedFileLists lists = computeLists();
        if (lists.readFiles.isEmpty() && lists.modifiedFiles.isEmpty()) {
            return "";
        }

        // Build a mode map: Read / Write / RW.
        Map<String, String> mode = new LinkedHashMap<>();
        for (String f : lists.readFiles) mode.put(f, "Read");
        for (String f : lists.modifiedFiles) {
            mode.put(f, read.contains(f) ? "RW" : "Write");
        }

        List<String> all = new ArrayList<>(mode.keySet());
        Collections.sort(all);

        int limit = Math.min(all.size(), FILE_OPERATION_SUMMARY_LIMIT);
        StringBuilder sb = new StringBuilder();
        sb.append("<files>\n");
        // Group by parent directory (prefix-folded tree shape).
        Map<String, List<String>> byParent = groupByParent(all.subList(0, limit));
        boolean first = true;
        for (Map.Entry<String, List<String>> e : byParent.entrySet()) {
            if (!first) sb.append('\n');
            first = false;
            String parent = e.getKey();
            if (!parent.isEmpty()) {
                sb.append("# ").append(parent).append('\n');
            }
            for (String f : e.getValue()) {
                sb.append(f).append(" (").append(mode.get(f)).append(")\n");
            }
        }
        if (all.size() > FILE_OPERATION_SUMMARY_LIMIT) {
            sb.append("[\u2026").append(all.size() - FILE_OPERATION_SUMMARY_LIMIT)
              .append(" files elided\u2026]\n");
        }
        sb.append("</files>");
        return sb.toString();
    }

    /** Read-only access to the read set (for RW detection). */
    public Set<String> readSet() { return Collections.unmodifiableSet(read); }

    /**
     * Strip a trailing read-tool selector ({@code :50-200}, {@code :raw},
     * {@code :1-50:raw}) so the same file read with different ranges
     * dedupes to one entry.
     */
    static String stripReadSelector(String path) {
        if (path == null) return path;
        int colon = path.lastIndexOf(':');
        if (colon <= 0) return path;
        String candidate = path.substring(colon + 1);
        // Selector grammar: line ranges (L?digits with - + ..), 'raw', 'conflicts'.
        if (candidate.matches("(?i)L?\\d+(?:[-+..]|-|\\.\\.)?L?\\d*(?:,L?\\d+(?:[-+..]|-|\\.\\.)?L?\\d*)*")
                || candidate.equalsIgnoreCase("raw")
                || candidate.equalsIgnoreCase("conflicts")) {
            return path.substring(0, colon);
        }
        return path;
    }

    /** A {@code scheme://} URL is not a re-groundable file. */
    static boolean isUrlScheme(String path) {
        if (path == null) return false;
        return path.matches("^[a-z][a-z0-9+.-]*://.*$");
    }

    private static String extractPath(AgentMessage.ToolCall tc) {
        if (tc.argumentsJson == null || tc.argumentsJson.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(tc.argumentsJson);
            // Common arg names across our tool registry.
            String[] keys = {"path", "file_path", "file", "filename", "name",
                             "java_name", "xml_name", "layout", "widget_id"};
            for (String k : keys) {
                if (obj.has(k)) {
                    Object v = obj.get(k);
                    if (v instanceof String) return (String) v;
                }
            }
        } catch (JSONException ignored) {}
        return null;
    }

    private static boolean isReadTool(String name) {
        return name.equals("read_file") || name.equals("read")
            || name.equals("list_files") || name.equals("search_files")
            || name.equals("view_list_widgets") || name.equals("view_list_layouts")
            || name.equals("java_read_file") || name.equals("view_list_favorites")
            || name.equals("list_blocks") || name.equals("event_list");
    }

    private static boolean isWriteTool(String name) {
        return name.equals("write_file") || name.equals("create_file")
            || name.equals("write") || name.equals("create");
    }

    private static boolean isEditTool(String name) {
        return name.equals("edit_file") || name.equals("apply_patch")
            || name.equals("diff_edit_file") || name.equals("java_edit_file")
            || name.equals("java_modify_class") || name.equals("view_add_widget")
            || name.equals("view_set_property") || name.equals("view_delete_widget")
            || name.equals("view_manage_widget") || name.equals("view_manage_layout")
            || name.equals("view_manage_custom_widget") || name.equals("view_undo_redo")
            || name.equals("component_add") || name.equals("component_set_property")
            || name.equals("component_manage") || name.equals("custom_component_manage")
            || name.equals("block_add") || name.equals("block_manage")
            || name.equals("event_attach") || name.equals("event_manage")
            || name.equals("manifest_manage") || name.equals("resource_file_manage")
            || name.equals("values_xml_manage") || name.equals("project_set_property")
            || name.equals("project_manage") || name.equals("theme_manage");
    }

    private static Map<String, List<String>> groupByParent(List<String> files) {
        // Sort into a tree by directory prefix, then render.
        TreeMap<String, List<String>> result = new TreeMap<>();
        for (String f : files) {
            String parent;
            int slash = f.lastIndexOf('/');
            parent = slash >= 0 ? f.substring(0, slash) : "";
            String base = slash >= 0 ? f.substring(slash + 1) : f;
            result.computeIfAbsent(parent, k -> new ArrayList<>()).add(base);
        }
        return result;
    }

    /** Result of {@link #computeLists()}. */
    public static final class ComputedFileLists {
        public final List<String> readFiles;
        public final List<String> modifiedFiles;
        public ComputedFileLists(List<String> readFiles, List<String> modifiedFiles) {
            this.readFiles = readFiles;
            this.modifiedFiles = modifiedFiles;
        }
    }
}
