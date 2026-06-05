package com.example.ratelimiter.ratelimit;

public interface RateLimitService {

    RateLimitDecision check(String clientId, String route);
}
