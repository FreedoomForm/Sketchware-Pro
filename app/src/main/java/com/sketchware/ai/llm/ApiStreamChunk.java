package com.sketchware.ai.llm;

import com.sketchware.ai.agent.AgentMessage;

import java.util.List;

/**
 * Sealed-style hierarchy (one Java class per variant since we don't use Java 17
 * sealed records everywhere) describing the chunks emitted by an
 * {@link LlmProvider#stream(LlmRequest)} call.
 *
 * <p>Mirrors Cline's {@code ApiStreamChunk} type:
 * <ul>
 *   <li>{@link Text} - text delta</li>
 *   <li>{@link Reasoning} - thinking delta</li>
 *   <li>{@link ToolCalls} - one or more tool calls (complete, not partial)</li>
 *   <li>{@link Usage} - token usage stats</li>
 *   <li>{@link Done} - end of stream</li>
 * </ul>
 */
public abstract class ApiStreamChunk {

    private ApiStreamChunk() {}

    public boolean isText()      { return this instanceof Text; }
    public boolean isReasoning() { return this instanceof Reasoning; }
    public boolean isToolCalls() { return this instanceof ToolCalls; }
    public boolean isUsage()     { return this instanceof Usage; }
    public boolean isDone()      { return this instanceof Done; }

    public Text      asText()      { return (Text) this; }
    public Reasoning asReasoning() { return (Reasoning) this; }
    public ToolCalls asToolCalls() { return (ToolCalls) this; }
    public Usage     asUsage()     { return (Usage) this; }

    public static final class Text extends ApiStreamChunk {
        public final String text;
        public Text(String text) { this.text = text; }
    }

    public static final class Reasoning extends ApiStreamChunk {
        public final String text;
        public Reasoning(String text) { this.text = text; }
    }

    public static final class ToolCalls extends ApiStreamChunk {
        public final List<AgentMessage.ToolCall> calls;
        public ToolCalls(List<AgentMessage.ToolCall> calls) { this.calls = calls; }
    }

    public static final class Usage extends ApiStreamChunk {
        public final int inputTokens;
        public final int outputTokens;
        public final int reasoningTokens;
        public final int cacheReadTokens;
        public final int cacheWriteTokens;
        public final double cost;
        public Usage(int inputTokens, int outputTokens, int reasoningTokens,
                     int cacheReadTokens, int cacheWriteTokens, double cost) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.reasoningTokens = reasoningTokens;
            this.cacheReadTokens = cacheReadTokens;
            this.cacheWriteTokens = cacheWriteTokens;
            this.cost = cost;
        }
    }

    public static final class Done extends ApiStreamChunk {}
}
