package com.example.ratelimiter.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitDecisionTest {

    @Test
    void allowsRequestBelowLimit() {
        var decision = RateLimitDecision.from(3, 10, 42);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(10);
        assertThat(decision.remaining()).isEqualTo(7);
        assertThat(decision.resetSeconds()).isEqualTo(42);
        assertThat(decision.retryAfterSeconds()).isZero();
    }

    @Test
    void blocksRequestAboveLimit() {
        var decision = RateLimitDecision.from(11, 10, 42);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.resetSeconds()).isEqualTo(42);
        assertThat(decision.retryAfterSeconds()).isEqualTo(42);
    }
}
