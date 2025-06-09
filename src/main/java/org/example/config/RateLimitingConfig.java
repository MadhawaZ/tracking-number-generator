package org.example.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter();
    }

    public static class RateLimitingFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
        private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

        // Allow 100 requests per minute per IP
        private static final int REQUESTS_PER_MINUTE = 100;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {

            // Only apply rate limiting to the tracking number endpoint
            if (!request.getRequestURI().equals("/next-tracking-number")) {
                filterChain.doFilter(request, response);
                return;
            }

            String clientIp = getClientIpAddress(request);
            Bucket bucket = getBucket(clientIp);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                logger.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"RATE_LIMIT_EXCEEDED\"," +
                    "\"message\":\"Too many requests. Please try again later.\"," +
                    "\"retryAfter\":60}"
                );
            }
        }

        private Bucket getBucket(String clientIp) {
            return buckets.computeIfAbsent(clientIp, key -> createNewBucket());
        }

        private Bucket createNewBucket() {
            Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
    }
}
