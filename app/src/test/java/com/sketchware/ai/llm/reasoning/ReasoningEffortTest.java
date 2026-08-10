package com.sketchware.ai.llm.reasoning;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ReasoningEffort} and {@link ReasoningRequest}.
 */
public class ReasoningEffortTest {

    @Test public void ratioMatchesSpec() {
        assertThat(ReasoningEffort.NONE.ratio).isEqualTo(0.0);
        assertThat(ReasoningEffort.MINIMAL.ratio).isEqualTo(0.1);
        assertThat(ReasoningEffort.LOW.ratio).isEqualTo(0.2);
        assertThat(ReasoningEffort.MEDIUM.ratio).isEqualTo(0.5);
        assertThat(ReasoningEffort.HIGH.ratio).isEqualTo(0.8);
        assertThat(ReasoningEffort.XHIGH.ratio).isEqualTo(0.95);
        assertThat(ReasoningEffort.MAX.ratio).isEqualTo(1.0);
    }

    @Test public void parseLowercase() {
        assertThat(ReasoningEffort.parse("none")).isEqualTo(ReasoningEffort.NONE);
        assertThat(ReasoningEffort.parse("minimal")).isEqualTo(ReasoningEffort.MINIMAL);
        assertThat(ReasoningEffort.parse("low")).isEqualTo(ReasoningEffort.LOW);
        assertThat(ReasoningEffort.parse("medium")).isEqualTo(ReasoningEffort.MEDIUM);
        assertThat(ReasoningEffort.parse("high")).isEqualTo(ReasoningEffort.HIGH);
        assertThat(ReasoningEffort.parse("xhigh")).isEqualTo(ReasoningEffort.XHIGH);
        assertThat(ReasoningEffort.parse("max")).isEqualTo(ReasoningEffort.MAX);
    }

    @Test public void parseUppercase() {
        assertThat(ReasoningEffort.parse("MEDIUM")).isEqualTo(ReasoningEffort.MEDIUM);
        assertThat(ReasoningEffort.parse("HIGH")).isEqualTo(ReasoningEffort.HIGH);
    }

    @Test public void parseNullReturnsNone() {
        assertThat(ReasoningEffort.parse(null)).isEqualTo(ReasoningEffort.NONE);
    }

    @Test public void parseUnknownReturnsNone() {
        assertThat(ReasoningEffort.parse("garbage")).isEqualTo(ReasoningEffort.NONE);
    }

    @Test public void fromEffortNoneReturnsDisabled() {
        ReasoningRequest r = ReasoningRequest.fromEffort(ReasoningEffort.NONE, 4096);
        assertThat(r.enabled).isFalse();
        assertThat(r.effort).isEqualTo(ReasoningEffort.NONE);
        assertThat(r.budgetTokens).isNull();
    }

    @Test public void fromEffortMediumComputesBudget() {
        ReasoningRequest r = ReasoningRequest.fromEffort(ReasoningEffort.MEDIUM, 4096);
        assertThat(r.enabled).isTrue();
        assertThat(r.effort).isEqualTo(ReasoningEffort.MEDIUM);
        assertThat(r.budgetTokens).isNotNull();
        // ratio 0.5 * 4096 = 2048
        assertThat(r.budgetTokens).isEqualTo(2048);
    }

    @Test public void fromEffortMaxComputesFullBudget() {
        ReasoningRequest r = ReasoningRequest.fromEffort(ReasoningEffort.MAX, 8192);
        assertThat(r.budgetTokens).isEqualTo(8192);
    }

    @Test public void fromEffortHasMinimumBudget() {
        // Even with a small maxTokens, the budget should be at least 1024.
        ReasoningRequest r = ReasoningRequest.fromEffort(ReasoningEffort.LOW, 100);
        assertThat(r.budgetTokens).isAtLeast(1024);
    }

    @Test public void disabledReturnsRequestWithEnabledFalse() {
        ReasoningRequest r = ReasoningRequest.disabled();
        assertThat(r.enabled).isFalse();
        assertThat(r.effort).isEqualTo(ReasoningEffort.NONE);
        assertThat(r.budgetTokens).isNull();
        assertThat(r.isReasoningEnabled()).isFalse();
    }

    @Test public void isReasoningEnabledTrueWhenEnabledTrue() {
        ReasoningRequest r = new ReasoningRequest(true, ReasoningEffort.HIGH, 4096);
        assertThat(r.isReasoningEnabled()).isTrue();
    }
}
