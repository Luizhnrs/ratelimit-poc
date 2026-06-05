package com.example.ratelimiter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimiter.ratelimit.RateLimitDecision;
import com.example.ratelimiter.ratelimit.RateLimitService;
import com.example.ratelimiter.ratelimit.RateLimitUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void addsHeadersAndContinuesWhenAllowed() throws ServletException, IOException {
        var chainReached = new AtomicBoolean(false);
        var filter = new RateLimitFilter((clientId, route) -> RateLimitDecision.from(1, 10, 60));
        var request = new MockHttpServletRequest("GET", "/api/protected");
        request.addHeader("X-Client-Id", "client-a");
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> chainReached.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainReached).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("60");
    }

    @Test
    void returns429WhenBlocked() throws ServletException, IOException {
        var filter = new RateLimitFilter((clientId, route) -> RateLimitDecision.from(11, 10, 42));
        var request = new MockHttpServletRequest("GET", "/api/protected");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentAsString()).contains("rate_limit_exceeded");
    }

    @Test
    void returns503WhenRateLimiterIsUnavailable() throws ServletException, IOException {
        RateLimitService service = (clientId, route) -> {
            throw new RateLimitUnavailableException("Redis is unavailable");
        };
        var filter = new RateLimitFilter(service);
        var request = new MockHttpServletRequest("GET", "/api/protected");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("rate_limit_unavailable");
    }

    @Test
    void skipsHealthEndpoint() throws ServletException, IOException {
        var chainReached = new AtomicBoolean(false);
        var filter = new RateLimitFilter((clientId, route) -> {
            throw new AssertionError("Health endpoint must not be rate limited");
        });
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> chainReached.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainReached).isTrue();
    }
}
