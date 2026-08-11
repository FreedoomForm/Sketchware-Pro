package com.sketchware.ai.ui.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses and dispatches slash commands typed in the chat input. Mirrors
 * Cline's {@code core/slash-commands/index.ts} {@code parseSlashCommands}.
 *
 * <p>Supported commands:
 * <ul>
 *   <li>{@code /new} - clear conversation history, start fresh.</li>
 *   <li>{@code /clear} - alias for /new.</li>
 *   <li>{@code /compact} - manually trigger context compaction.</li>
 *   <li>{@code /help} - show available commands.</li>
 *   <li>{@code /export} - export the conversation to a file.</li>
 *   <li>{@code /mode <act|plan|yolo>} - switch agent mode.</li>
 *   <li>{@code /cost} - show token usage and cost summary.</li>
 *   <li>{@code /undo} - undo the last user message + assistant response.</li>
 *   <li>{@code /exit} - close the chat session.</li>
 *   <li>{@code /model <id>} - switch the active model (if supported).</li>
 *   <li>{@code /maxiter <n>} - set the max iterations for the next run.</li>
 *   <li>{@code /context} - show the current context window usage.</li>
 *   <li>{@code /tools} - list all registered tools.</li>
 *   <li>{@code /approve <tool> [on|off]} - configure per-tool auto-approval.</li>
 * </ul>
 *
 * <p>Commands are parsed but NOT executed here — execution requires access
 * to the {@link com.sketchware.ai.agent.AgentRuntime} and UI components,
 * which is the caller's job. The parser returns a {@link ParsedCommand} that
 * the caller dispatches on.
 */
public final class SlashCommandProcessor {

    private SlashCommandProcessor() {}

    /** All recognized slash commands. */
    public static final List<CommandSpec> COMMANDS = Arrays.asList(
            new CommandSpec("new",      "Clear conversation and start fresh",                    "",          false),
            new CommandSpec("clear",    "Alias for /new",                                        "",          false),
            new CommandSpec("compact",  "Manually trigger context compaction",                   "",          false),
            new CommandSpec("help",     "Show available slash commands",                         "",          false),
            new CommandSpec("export",   "Export conversation to a file",                         "[path]",    false),
            new CommandSpec("mode",     "Switch agent mode (act, plan, yolo)",                   "<mode>",    true),
            new CommandSpec("cost",     "Show token usage and cost summary",                     "",          false),
            new CommandSpec("undo",     "Undo the last user message + assistant response",       "",          false),
            new CommandSpec("exit",     "Close the chat session",                                "",          false),
            new CommandSpec("model",    "Switch the active model",                               "<model_id>",true),
            new CommandSpec("maxiter",  "Set max iterations for the next run",                   "<n>",       true),
            new CommandSpec("context",  "Show current context window usage",                     "",          false),
            new CommandSpec("tools",    "List all registered tools",                             "",          false),
            new CommandSpec("approve",  "Configure per-tool auto-approval",                     "<tool> [on|off]", true)
    );

    /** Specification of a slash command. */
    public static final class CommandSpec {
        public final String name;
        public final String description;
        public final String argSyntax;
        public final boolean requiresArg;
        public CommandSpec(String name, String description, String argSyntax, boolean requiresArg) {
            this.name = name;
            this.description = description;
            this.argSyntax = argSyntax;
            this.requiresArg = requiresArg;
        }
    }

    /** A parsed slash command. */
    public static final class ParsedCommand {
        public final String name;
        public final String arg;       // null if no argument
        public final String rawInput;  // the original input string

        public ParsedCommand(String name, String arg, String rawInput) {
            this.name = name;
            this.arg = arg;
            this.rawInput = rawInput;
        }

        public boolean hasArg() { return arg != null && !arg.isEmpty(); }
    }

    /** Parse a single line of user input. Returns null if not a slash command. */
    public static ParsedCommand parse(String input) {
        if (input == null || input.isEmpty() || input.charAt(0) != '/') return null;
        String trimmed = input.trim();
        int spaceIdx = trimmed.indexOf(' ');
        String name = spaceIdx > 0 ? trimmed.substring(1, spaceIdx) : trimmed.substring(1);
        String arg = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : null;
        // Validate the command exists.
        for (CommandSpec spec : COMMANDS) {
            if (spec.name.equals(name)) {
                if (spec.requiresArg && (arg == null || arg.isEmpty())) {
                    return null; // missing required arg
                }
                return new ParsedCommand(name, arg, input);
            }
        }
        return null; // unknown command
    }

    /**
     * Split input that may contain a slash command followed by additional text.
     * Example: "/compact and then build the app" -&gt; command=compact, remaining="and then build the app".
     *
     * <p>If the input doesn't start with a slash command, returns null.
     */
    public static ParsedWithRemaining parseWithRemaining(String input) {
        if (input == null || input.isEmpty() || input.charAt(0) != '/') return null;
        ParsedCommand cmd = parse(input);
        if (cmd == null) return null;
        // Find where the command ends in the raw input.
        String remaining = "";
        String trimmed = input.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            // For commands with required args, the first token after the command is the arg,
            // and everything after that is the remaining text.
            if (cmd.hasArg()) {
                int argStart = spaceIdx + 1;
                int nextSpace = trimmed.indexOf(' ', argStart);
                if (nextSpace > 0) {
                    remaining = trimmed.substring(nextSpace + 1).trim();
                }
            } else {
                remaining = trimmed.substring(spaceIdx + 1).trim();
            }
        }
        return new ParsedWithRemaining(cmd, remaining);
    }

    /** Result of {@link #parseWithRemaining}. */
    public static final class ParsedWithRemaining {
        public final ParsedCommand command;
        public final String remaining;
        public ParsedWithRemaining(ParsedCommand command, String remaining) {
            this.command = command;
            this.remaining = remaining;
        }
    }

    /** Build the help text shown when the user types /help. */
    public static String helpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available slash commands:\n");
        for (CommandSpec c : COMMANDS) {
            sb.append("  /").append(c.name);
            if (!c.argSyntax.isEmpty()) sb.append(" ").append(c.argSyntax);
            sb.append(" - ").append(c.description).append("\n");
        }
        sb.append("\nTip: You can also use @file:path, @project:id, @layout:name, ")
          .append("@component:id, @url:URL, @image:path, @problems, @git-changes ")
          .append("to inline context into your message.");
        return sb.toString();
    }

    /**
     * Generate autocomplete suggestions for a partial slash command.
     * Returns command names that start with the given prefix (without the leading /).
     */
    public static List<String> suggest(String prefix) {
        List<String> result = new ArrayList<>();
        if (prefix == null) prefix = "";
        for (CommandSpec c : COMMANDS) {
            if (c.name.startsWith(prefix)) result.add("/" + c.name);
        }
        return result;
    }
}
