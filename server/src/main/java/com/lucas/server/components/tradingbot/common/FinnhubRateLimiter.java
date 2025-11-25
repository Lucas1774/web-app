package com.lucas.server.components.tradingbot.common;

import com.lucas.utils.SlidingWindowRateLimiter;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import com.lucas.utils.orderedindexedset.UnmodifiableOrderedIndexedSet;
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
        OrderedIndexedSetImpl<Map.Entry<String, SlidingWindowRateLimiter>> orderedIndexedSet = new OrderedIndexedSetImpl<>();
        OrderedIndexedSet<String> finnhubRateLimiterNames = getFinnhubRateLimiterNames();
        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String limiterKey = finnhubRateLimiterNames.get(i);
            SlidingWindowRateLimiter rateLimiter = allRateLimiters.get(limiterKey);
            orderedIndexedSet.add(Map.entry(apiKey, rateLimiter));
        }
        keyToLimiterEntries = new UnmodifiableOrderedIndexedSet<>(orderedIndexedSet);
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
