package com.example.ratelimiter.web;

import com.example.ratelimiter.ratelimit.RateLimitDecision;
import com.example.ratelimiter.ratelimit.RateLimitService;
import com.example.ratelimiter.ratelimit.RateLimitUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String PROTECTED_ROUTE = "/api/protected";

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_ROUTE.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var clientId = resolveClientId(request);

        try {
            var decision = rateLimitService.check(clientId, PROTECTED_ROUTE);
            addRateLimitHeaders(response, decision);

            if (!decision.allowed()) {
                LOGGER.warn("Rate limit exceeded for client={} route={}", clientId, PROTECTED_ROUTE);
                writeError(response, 429, "rate_limit_exceeded", "Too many requests. Try again later.");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (RateLimitUnavailableException ex) {
            LOGGER.warn("Rate limit unavailable for client={} route={} reason={}",
                    clientId, PROTECTED_ROUTE, ex.getMessage());
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "rate_limit_unavailable", "Rate limit service is unavailable.");
        }
    }

    private static String resolveClientId(HttpServletRequest request) {
        var header = request.getHeader("X-Client-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return request.getRemoteAddr();
    }

    private static void addRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetSeconds()));

        if (!decision.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        }
    }

    private static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":"%s","message":"%s"}""".formatted(error, message));
    }
}
