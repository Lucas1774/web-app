package com.lucas.server.components.tradingbot.common;

import com.lucas.utils.OrderedIndexedSet;
import com.lucas.utils.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lucas.server.common.Constants.getFinnhubRateLimiterNames;

@Component
public class FinnhubRateLimiter {

    private final OrderedIndexedSet<Map.Entry<String, SlidingWindowRateLimiter>> keyToLimiterEntries;
    private final AtomicInteger pointer = new AtomicInteger();

    public FinnhubRateLimiter(@Value("${finnhub.api-keys}") List<String> apiKeys, Map<String, SlidingWindowRateLimiter> allRateLimiters) {
        keyToLimiterEntries = new OrderedIndexedSet<>();
        OrderedIndexedSet<String> finnhubRateLimiterNames = getFinnhubRateLimiterNames();
        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String limiterKey = finnhubRateLimiterNames.get(i);
            SlidingWindowRateLimiter rateLimiter = allRateLimiters.get(limiterKey);
            keyToLimiterEntries.add(Map.entry(apiKey, rateLimiter));
        }
    }

    public String acquirePermission() {
        int size = keyToLimiterEntries.size();
        while (true) {
            for (int i = 0; i < size; i++) {
                int idx = pointer.getAndIncrement() % size;
                Map.Entry<String, SlidingWindowRateLimiter> entry = keyToLimiterEntries.get(idx);
                if (entry.getValue().acquirePermission()) {
                    return entry.getKey();
                }
            }
        }
    }
}
