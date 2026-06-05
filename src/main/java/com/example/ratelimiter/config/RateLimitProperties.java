package com.example.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(int maxRequests, long windowSeconds) {

    public RateLimitProperties {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("rate-limit.max-requests must be greater than zero");
        }
        if (windowSeconds < 1) {
            throw new IllegalArgumentException("rate-limit.window-seconds must be greater than zero");
        }
    }
}
