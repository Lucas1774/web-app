package com.lucas.server.components.tradingbot.config;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowRateLimiter {

    private final int maxRequests;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private final long timeout;

    public SlidingWindowRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        windowNanos = window.toNanos();
        timeout = 0;
    }

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Duration timeout) {
        this.maxRequests = maxRequests;
        windowNanos = window.toNanos();
        this.timeout = timeout.toNanos();
    }


    public synchronized boolean acquirePermission() {
        long deadline = 0 < timeout ? System.nanoTime() + timeout : Long.MAX_VALUE;
        while (true) {
            long now = System.nanoTime();
            if (now >= deadline) {
                return false;
            }
            boolean removed = false;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowNanos) {
                timestamps.pollFirst();
                removed = true;
            }
            if (removed) {
                notifyAll();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            Long oldest = timestamps.peekFirst();
            if (null == oldest) {
                continue;
            }
            long waitTime = (oldest + windowNanos) - now;
            if (0 < waitTime) {
                try {
                    wait(waitTime / 1_000_000L, (int) (waitTime % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
    }
}
