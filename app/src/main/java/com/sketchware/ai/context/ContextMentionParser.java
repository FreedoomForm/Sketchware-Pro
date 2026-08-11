package com.sketchware.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code @}-mentions in user input and expands them to inline context.
 * Mirrors Cline's {@code shared/context-mentions.ts} {@code mentionRegex}.
 *
 * <p>Supported mention types:
 * <ul>
 *   <li>{@code @file:<path>} - inline the file's content (truncated to 4000 chars).</li>
 *   <li>{@code @project:<id>} - inline project metadata (name, package, SDK levels).</li>
 *   <li>{@code @component:<id>} - inline a component's properties.</li>
 *   <li>{@code @layout:<name>} - inline a layout's widget tree summary.</li>
 *   <li>{@code @url:<URL>} - inline the URL (the agent can fetch it via a tool).</li>
 *   <li>{@code @image:<path>} - attach an image to the message (vision models).</li>
 *   <li>{@code @problems} - inline the current build errors / lint problems.</li>
 *   <li>{@code @git-changes} - inline the current diff (if a git repo is present).</li>
 * </ul>
 *
 * <p>The parser extracts mentions but does NOT resolve them — resolution
 * requires project context (sc_id, file access) which is the caller's job.
 * The caller walks the list of {@link Mention} objects and replaces each
 * with the appropriate expanded text.
 */
public final class ContextMentionParser {

    private ContextMentionParser() {}

    /** One parsed mention. */
    public static final class Mention {
        public final Type type;
        public final String value;     // path, id, URL, etc.
        public final int startPos;     // index in original input where @mention starts
        public final int endPos;       // index in original input where @mention ends (exclusive)

        public Mention(Type type, String value, int startPos, int endPos) {
            this.type = type;
            this.value = value;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        /** The full @mention text as it appeared in the input. */
        public String rawText() {
            return "@" + type.prefix + (value == null ? "" : ":" + value);
        }
    }

    public enum Type {
        FILE("file"),
        PROJECT("project"),
        COMPONENT("component"),
        LAYOUT("layout"),
        URL("url"),
        IMAGE("image"),
        PROBLEMS("problems"),
        GIT_CHANGES("git-changes");

        public final String prefix;
        Type(String prefix) { this.prefix = prefix; }

        public static Type fromPrefix(String p) {
            for (Type t : values()) if (t.prefix.equals(p)) return t;
            return null;
        }
    }

    // Matches @type:value where value is a non-whitespace sequence (or quoted).
    // Also matches bare @type for value-less mentions like @problems.
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@(file|project|component|layout|url|image|problems|git-changes)(?::([^\\s@]+))?");

    /**
     * Find all mentions in the input string.
     */
    public static List<Mention> parse(String input) {
        List<Mention> result = new ArrayList<>();
        if (input == null || input.isEmpty()) return result;
        Matcher m = MENTION_PATTERN.matcher(input);
        while (m.find()) {
            String prefix = m.group(1);
            String value = m.group(2);
            Type t = Type.fromPrefix(prefix);
            if (t == null) continue;
            result.add(new Mention(t, value, m.start(), m.end()));
        }
        return result;
    }

    /**
     * Replace all mentions in the input with their expanded forms. The
     * {@link Expander} callback is invoked for each mention and returns
     * the replacement text (or null to leave the mention as-is).
     */
    public static String expand(String input, Expander expander) {
        if (input == null || input.isEmpty()) return input;
        List<Mention> mentions = parse(input);
        if (mentions.isEmpty()) return input;
        StringBuilder sb = new StringBuilder(input.length());
        int lastEnd = 0;
        for (Mention mention : mentions) {
            sb.append(input, lastEnd, mention.startPos);
            String replacement = expander.expand(mention);
            if (replacement == null) {
                sb.append(mention.rawText());
            } else {
                sb.append(replacement);
            }
            lastEnd = mention.endPos;
        }
        sb.append(input, lastEnd, input.length());
        return sb.toString();
    }

    /** Callback for expanding a mention to its inline text. */
    public interface Expander {
        /**
         * @param mention the mention to expand.
         * @return the expanded text, or null to leave the mention as-is.
         */
        String expand(Mention mention);
    }

    /**
     * Check whether a string looks like a mention (starts with @ and a known prefix).
     * Useful for input validation in the chat UI.
     */
    public static boolean isMention(String s) {
        if (s == null || !s.startsWith("@")) return false;
        return MENTION_PATTERN.matcher(s).find();
    }

    /** Count mentions of a specific type in the input. */
    public static int countByType(String input, Type type) {
        int count = 0;
        for (Mention m : parse(input)) {
            if (m.type == type) count++;
        }
        return count;
    }
}
