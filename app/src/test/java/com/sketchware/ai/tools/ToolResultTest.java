package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ToolResult}.
 */
public class ToolResultTest {

    @Test public void successCarriesOutput() {
        ToolResult r = ToolResult.success("hello");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.isError()).isFalse();
        assertThat(r.getOutput()).isEqualTo("hello");
        assertThat(r.getError()).isNull();
        assertThat(r.toLLMString()).isEqualTo("hello");
    }

    @Test public void errorCarriesMessage() {
        ToolResult r = ToolResult.error("bad input");
        assertThat(r.isError()).isTrue();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getError()).isEqualTo("bad input");
        assertThat(r.toLLMString()).startsWith("ERROR: bad input");
    }

    @Test public void errorFromThrowableExtractsMessage() {
        Throwable t = new RuntimeException("disk full");
        ToolResult r = ToolResult.error(t);
        assertThat(r.getError()).isEqualTo("disk full");
    }

    @Test public void errorFromThrowableWithoutMessageUsesClassName() {
        Throwable t = new RuntimeException();
        ToolResult r = ToolResult.error(t);
        assertThat(r.getError()).isEqualTo("RuntimeException");
    }

    @Test public void truncatesLargeOutputWithMiddleCut() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100_000; i++) sb.append('x');
        ToolResult r = ToolResult.success(sb.toString());
        assertThat(r.getOutput().length()).isLessThan(100_000);
        assertThat(r.getOutput()).contains("output truncated");
        assertThat(r.getOutput()).contains("chars");
    }

    @Test public void doesNotTruncateSmallOutput() {
        ToolResult r = ToolResult.success("small");
        assertThat(r.getOutput()).isEqualTo("small");
    }
}
