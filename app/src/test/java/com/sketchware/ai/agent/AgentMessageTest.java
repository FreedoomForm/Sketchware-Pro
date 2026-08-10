package com.sketchware.ai.agent;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for {@link AgentMessage}.
 */
public class AgentMessageTest {

    @Test public void systemMessageHasSystemRole() {
        AgentMessage m = AgentMessage.system("hello");
        assertThat(m.role).isEqualTo(AgentMessage.ROLE_SYSTEM);
        assertThat(m.text).isEqualTo("hello");
    }

    @Test public void userMessageHasUserRole() {
        AgentMessage m = AgentMessage.user("hi");
        assertThat(m.role).isEqualTo(AgentMessage.ROLE_USER);
        assertThat(m.text).isEqualTo("hi");
    }

    @Test public void assistantMessageWithTextOnly() {
        AgentMessage m = AgentMessage.assistant("answer", null, null);
        assertThat(m.role).isEqualTo(AgentMessage.ROLE_ASSISTANT);
        assertThat(m.text).isEqualTo("answer");
        assertThat(m.reasoning).isNull();
        assertThat(m.toolCalls).isNull();
        assertThat(m.hasToolCalls()).isFalse();
    }

    @Test public void assistantMessageWithToolCalls() {
        AgentMessage.ToolCall call = new AgentMessage.ToolCall("tc1", "view_add_widget", "{}");
        AgentMessage m = AgentMessage.assistant("Adding button", null, Collections.singletonList(call));
        assertThat(m.hasToolCalls()).isTrue();
        assertThat(m.toolCalls).hasSize(1);
        assertThat(m.toolCalls.get(0).id).isEqualTo("tc1");
        assertThat(m.toolCalls.get(0).name).isEqualTo("view_add_widget");
    }

    @Test public void toolResultMessage() {
        AgentMessage.ToolResultContent tr = new AgentMessage.ToolResultContent(
                "tc1", "view_add_widget", "Added button1", false);
        AgentMessage m = AgentMessage.toolResult(Collections.singletonList(tr));
        assertThat(m.role).isEqualTo(AgentMessage.ROLE_USER); // tool_result goes in user message
        assertThat(m.hasToolResults()).isTrue();
        assertThat(m.toolResults).hasSize(1);
        assertThat(m.toolResults.get(0).output).isEqualTo("Added button1");
        assertThat(m.toolResults.get(0).isError).isFalse();
    }

    @Test public void userWithImages() {
        AgentMessage m = AgentMessage.userWithImages("describe this", Arrays.asList("data:image/png;base64,abc"));
        assertThat(m.role).isEqualTo(AgentMessage.ROLE_USER);
        assertThat(m.images).hasSize(1);
        assertThat(m.images.get(0)).isEqualTo("data:image/png;base64,abc");
    }

    @Test public void estimateTokensForShortMessage() {
        AgentMessage m = AgentMessage.user("hello world");  // 11 chars
        int tokens = m.estimateTokens();
        assertThat(tokens).isAtLeast(1);
        assertThat(tokens).isAtMost(11); // at most 11 (if 1 char = 1 token)
    }

    @Test public void estimateTokensForEmptyMessage() {
        AgentMessage m = AgentMessage.user("");
        int tokens = m.estimateTokens();
        assertThat(tokens).isAtLeast(1); // never returns 0 (Math.max(1, ...))
    }

    @Test public void estimateTokensForLargeMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("abcdefgh");  // 8000 chars
        AgentMessage m = AgentMessage.user(sb.toString());
        // 8000 chars / 4 = 2000 tokens
        assertThat(m.estimateTokens()).isEqualTo(2000);
    }

    @Test public void estimateTokensIncludesToolCalls() {
        AgentMessage.ToolCall call = new AgentMessage.ToolCall(
                "id12345", "view_add_widget", "{\"widget_type\":\"Button\"}");
        AgentMessage m = AgentMessage.assistant("text", null, Collections.singletonList(call));
        int tokensWithCall = m.estimateTokens();
        AgentMessage withoutCall = AgentMessage.assistant("text", null, null);
        int tokensWithoutCall = withoutCall.estimateTokens();
        assertThat(tokensWithCall).isGreaterThan(tokensWithoutCall);
    }

    @Test public void estimateTokensIncludesToolResults() {
        AgentMessage.ToolResultContent tr = new AgentMessage.ToolResultContent(
                "id12345", "view_add_widget",
                "Added Button with id=button1 to layout 'main'. The widget is now visible on the canvas.",
                false);
        AgentMessage m = AgentMessage.toolResult(Collections.singletonList(tr));
        int tokens = m.estimateTokens();
        assertThat(tokens).isGreaterThan(5);
    }

    @Test public void abortControllerInitiallyNotAborted() {
        AbortController c = new AbortController();
        assertThat(c.isAborted()).isFalse();
    }

    @Test public void abortControllerSetsAbortedFlag() {
        AbortController c = new AbortController();
        c.abort();
        assertThat(c.isAborted()).isTrue();
    }

    @Test public void abortControllerInvokesHttpCancellation() {
        AbortController c = new AbortController();
        boolean[] cancelled = {false};
        c.setHttpCancellation(() -> cancelled[0] = true);
        c.abort();
        assertThat(cancelled[0]).isTrue();
    }

    @Test public void abortControllerClearHttpCancellation() {
        AbortController c = new AbortController();
        boolean[] cancelled = {false};
        c.setHttpCancellation(() -> cancelled[0] = true);
        c.clearHttpCancellation();
        c.abort();
        assertThat(cancelled[0]).isFalse(); // not invoked because cleared
    }
}
