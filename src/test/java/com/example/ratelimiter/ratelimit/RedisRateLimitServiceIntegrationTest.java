package com.example.ratelimiter.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.RateLimiterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = RateLimiterApplication.class,
        properties = {
                "rate-limit.max-requests=2",
                "rate-limit.window-seconds=1"
        }
)
class RedisRateLimitServiceIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private RateLimitService service;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void blocksAfterLimitAndKeepsClientsIndependent() {
        var first = service.check("client-a", "/api/protected");
        var second = service.check("client-a", "/api/protected");
        var third = service.check("client-a", "/api/protected");
        var otherClient = service.check("client-b", "/api/protected");

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isZero();
        assertThat(third.allowed()).isFalse();
        assertThat(third.retryAfterSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(otherClient.allowed()).isTrue();
        assertThat(otherClient.remaining()).isEqualTo(1);
    }
}
