package com.example.ratelimiter.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long resetSeconds,
        long retryAfterSeconds
) {

    public static RateLimitDecision from(long currentCount, long limit, long ttlSeconds) {
        var reset = Math.max(ttlSeconds, 0);
        var allowed = currentCount <= limit;
        var remaining = Math.max(limit - currentCount, 0);
        var retryAfter = allowed ? 0 : reset;
        return new RateLimitDecision(allowed, limit, remaining, reset, retryAfter);
    }
}
