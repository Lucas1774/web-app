package com.lucas.server.components.tradingbot.common;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lucas.server.common.Constants.FINNHUB_RATE_LIMITERS;

@Component
public class FinnhubRateLimiter {

    private final List<Map.Entry<String, RateLimiter>> keyToLimiterEntries;
    private final AtomicInteger pointer = new AtomicInteger();

    public FinnhubRateLimiter(@Value("${finnhub.api-keys}") List<String> apiKeys, Map<String, RateLimiter> allRateLimiters) {
        this.keyToLimiterEntries = new ArrayList<>(apiKeys.size());
        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String limiterKey = FINNHUB_RATE_LIMITERS.get(i);
            RateLimiter rateLimiter = allRateLimiters.get(limiterKey);
            keyToLimiterEntries.add(Map.entry(apiKey, rateLimiter));
        }
    }

    public String acquirePermission() {
        int size = keyToLimiterEntries.size();
        while (true) {
            for (int i = 0; i < size; i++) {
                int idx = pointer.getAndIncrement() % size;
                Map.Entry<String, RateLimiter> entry = keyToLimiterEntries.get(idx);
                if (entry.getValue().acquirePermission()) {
                    return entry.getKey();
                }
            }
        }
    }
}
