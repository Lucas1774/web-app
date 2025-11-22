package com.lucas.server.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RequestPerSecondLimitFilter implements RateLimitFilter {

    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final Map<String, Set<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String clientIP = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();
        synchronized (requestTimestamps) {
            Set<Long> clientRequests = requestTimestamps.computeIfAbsent(clientIP, k -> new HashSet<>());
            clientRequests.removeIf(timestamp -> timestamp < now - 1);
            if (MAX_REQUESTS_PER_SECOND <= clientRequests.size()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests from this IP.");
                return;
            }
            clientRequests.add(now);
        }
        filterChain.doFilter(request, response);
    }
}
