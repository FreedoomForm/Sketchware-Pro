package com.sketchware.ai.tools.filesystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * search_files — search file contents in the project using regex.
 *
 * <p>Mirrors Cline's {@code search_files} tool. Walks the project directory
 * and searches each file's content for lines matching the given regex pattern.
 * Returns matching lines with file paths and line numbers.
 *
 * <p>Use to find where a particular string, class name, or pattern appears
 * in the project (e.g. "find all places that reference R.id.btn_save").
 *
 * <p>Files larger than {@link #MAX_FILE_SIZE_BYTES} are skipped. Binary files
 * (determined by file extension) are also skipped. Hidden files and
 * directories (starting with {@code .}) are excluded.
 */
public final class SearchFilesTool implements SketchwareTool {

    /** Maximum file size to search (in bytes). Larger files are skipped. */
    static final long MAX_FILE_SIZE_BYTES = 500_000L;

    /** Maximum total matching lines to return. */
    static final int MAX_MATCHES = 50;

    /** Maximum files to scan (safety cap to avoid runaway scans). */
    static final int MAX_FILES_SCANNED = 500;

    /** File extensions that are treated as text. */
    static final String[] TEXT_EXTENSIONS = {
        ".java", ".kt", ".xml", ".json", ".txt", ".md", ".gradle", ".properties",
        ".js", ".ts", ".html", ".css", ".scss", ".py", ".rb", ".go", ".rs",
        ".c", ".cpp", ".h", ".hpp", ".cs", ".php", ".swift", ".m", ".sh",
        ".yml", ".yaml", ".toml", ".cfg", ".conf", ".ini", ".sql", ".csv"
    };

    @Override public String name() { return "search_files"; }
    @Override public String category() { return "filesystem"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Search file contents in the project using regex. Returns matching lines with file paths and line numbers. "
                + "Use to find where a string/pattern appears. Max " + MAX_MATCHES + " matches returned.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject pattern = new JsonObject();
        pattern.addProperty("type", "string");
        pattern.addProperty("description", "Regex pattern to search for (case-insensitive by default)");
        props.add("pattern", pattern);
        JsonObject path = new JsonObject();
        path.addProperty("type", "string");
        path.addProperty("description", "Optional: relative path to limit search scope (e.g. 'java', 'resource')");
        props.add("path", path);
        JsonObject caseSensitive = new JsonObject();
        caseSensitive.addProperty("type", "boolean");
        caseSensitive.addProperty("description", "If true, search is case-sensitive (default false)");
        caseSensitive.addProperty("default", false);
        props.add("case_sensitive", caseSensitive);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("pattern");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (!args.has("pattern") || args.get("pattern").isJsonNull()) {
            return ToolResult.error("Missing required parameter: pattern");
        }
        String patternStr = args.get("pattern").getAsString();
        if (patternStr.isEmpty()) {
            return ToolResult.error("Search pattern is empty");
        }
        String relPath = args.has("path") && !args.get("path").isJsonNull()
                ? args.get("path").getAsString().trim() : "";
        boolean caseSensitive = args.has("case_sensitive") && args.get("case_sensitive").isJsonPrimitive()
                && args.get("case_sensitive").getAsBoolean();

        Pattern pattern;
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(patternStr, flags);
        } catch (PatternSyntaxException e) {
            return ToolResult.error("Invalid regex pattern: " + e.getMessage());
        }

        File projectRoot = ListFilesTool.resolveProjectRoot(ctx);
        if (projectRoot == null || !projectRoot.exists()) {
            return ToolResult.error("Project root not found");
        }
        File target = relPath.isEmpty() ? projectRoot : new File(projectRoot, relPath);
        if (!target.exists()) {
            return ToolResult.error("Path does not exist: " + relPath);
        }

        List<Match> matches = new ArrayList<>();
        int[] filesScanned = {0};
        walkAndSearch(target, projectRoot, pattern, matches, filesScanned);

        return ToolResult.success(formatResults(patternStr, relPath, matches, filesScanned[0]));
    }

    private void walkAndSearch(File dir, File root, Pattern pattern,
                                List<Match> matches, int[] filesScanned) {
        if (matches.size() >= MAX_MATCHES || filesScanned[0] >= MAX_FILES_SCANNED) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (matches.size() >= MAX_MATCHES || filesScanned[0] >= MAX_FILES_SCANNED) return;
            if (child.getName().startsWith(".")) continue;
            if (child.isDirectory()) {
                walkAndSearch(child, root, pattern, matches, filesScanned);
            } else if (child.isFile() && isTextFile(child) && child.length() <= MAX_FILE_SIZE_BYTES) {
                searchFile(child, root, pattern, matches);
                filesScanned[0]++;
            }
        }
    }

    private void searchFile(File file, File root, Pattern pattern, List<Match> matches) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String relPath = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
            for (int i = 0; i < lines.size(); i++) {
                if (matches.size() >= MAX_MATCHES) return;
                String line = lines.get(i);
                if (pattern.matcher(line).find()) {
                    matches.add(new Match(relPath, i + 1, line));
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Skip unreadable files.
        }
    }

    private static boolean isTextFile(File f) {
        String name = f.getName().toLowerCase();
        for (String ext : TEXT_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private static String formatResults(String pattern, String path, List<Match> matches, int filesScanned) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pattern: ").append(pattern).append("\n");
        sb.append("Scope: ").append(path.isEmpty() ? "/" : path).append("\n");
        sb.append("Files scanned: ").append(filesScanned).append("\n");
        sb.append("Matches: ").append(matches.size())
          .append(matches.size() >= MAX_MATCHES ? " (capped at " + MAX_MATCHES + ")" : "")
          .append("\n\n");
        if (matches.isEmpty()) {
            sb.append("(no matches found)");
        } else {
            String lastFile = null;
            for (Match m : matches) {
                if (!m.filePath.equals(lastFile)) {
                    sb.append("\n").append(m.filePath).append(":\n");
                    lastFile = m.filePath;
                }
                sb.append("  ").append(m.lineNumber).append(": ").append(truncateLine(m.lineText)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String truncateLine(String line) {
        if (line == null) return "";
        String trimmed = line.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
    }

    /** One matching line. */
    static final class Match {
        final String filePath;
        final int lineNumber;
        final String lineText;

        Match(String filePath, int lineNumber, String lineText) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.lineText = lineText;
        }
    }
}
