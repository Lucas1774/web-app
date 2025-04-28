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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestPerSecondLimitFilter implements RateLimitFilter {

    private static final int MAX_REQUESTS_PER_SECOND = 5;
    private static final Map<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String clientIP = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();
        synchronized (requestTimestamps) {
            List<Long> clientRequests = requestTimestamps.computeIfAbsent(clientIP, k -> new ArrayList<>());
            clientRequests.removeIf(timestamp -> timestamp < now - 1);
            if (clientRequests.size() >= MAX_REQUESTS_PER_SECOND) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests from this IP.");
                return;
            }
            clientRequests.add(now);
        }
        filterChain.doFilter(request, response);
    }
}
