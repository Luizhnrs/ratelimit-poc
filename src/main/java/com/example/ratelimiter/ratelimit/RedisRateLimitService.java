package com.example.ratelimiter.ratelimit;

import com.example.ratelimiter.config.RateLimitProperties;
import java.util.List;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitService implements RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> script;
    private final RateLimitProperties properties;

    public RedisRateLimitService(
            StringRedisTemplate redisTemplate,
            RedisScript<List> rateLimitScript,
            RateLimitProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.script = rateLimitScript;
        this.properties = properties;
    }

    @Override
    public RateLimitDecision check(String clientId, String route) {
        try {
            var key = "rate-limit:%s:%s".formatted(clientId, route);
            var result = redisTemplate.execute(script, List.of(key), String.valueOf(properties.windowSeconds()));

            if (result == null || result.size() < 2) {
                throw new RateLimitUnavailableException("Redis returned an invalid rate limit result");
            }

            var current = toLong(result.get(0));
            var ttl = toLong(result.get(1));
            return RateLimitDecision.from(current, properties.maxRequests(), ttl);
        } catch (RedisConnectionFailureException ex) {
            throw new RateLimitUnavailableException("Redis is unavailable", ex);
        } catch (RuntimeException ex) {
            throw new RateLimitUnavailableException("Could not evaluate rate limit", ex);
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
