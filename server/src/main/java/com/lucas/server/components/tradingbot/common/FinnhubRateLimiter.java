package com.lucas.server.components.tradingbot.common;

import com.lucas.utils.Interrupts;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import com.lucas.utils.ratelimiter.DefaultSlidingWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lucas.server.common.Constants.FINNHUB_RATE_LIMITER_ROTATION_DEBOUNCE_MS;
import static com.lucas.server.common.Constants.getFinnhubRateLimiterNames;

@Component
@Slf4j
public class FinnhubRateLimiter {

    private final OrderedIndexedSet<Map.Entry<String, DefaultSlidingWindowRateLimiter>> keyToLimiterEntries;
    private final AtomicInteger pointer = new AtomicInteger();

    public FinnhubRateLimiter(@Value("${finnhub.api-keys}") List<String> apiKeys,
                              Map<String, DefaultSlidingWindowRateLimiter> rateLimiters) {
        OrderedIndexedSetImpl<Map.Entry<String, DefaultSlidingWindowRateLimiter>> orderedIndexedSet =
                new OrderedIndexedSetImpl<>();
        OrderedIndexedSet<String> finnhubRateLimiterNames = getFinnhubRateLimiterNames();
        for (int i = 0; i < apiKeys.size(); i++) {
            String apiKey = apiKeys.get(i);
            String limiterKey = finnhubRateLimiterNames.get(i);
            DefaultSlidingWindowRateLimiter rateLimiter = rateLimiters.get(limiterKey);
            orderedIndexedSet.add(Map.entry(apiKey, rateLimiter));
        }
        keyToLimiterEntries = OrderedIndexedSet.copyOf(orderedIndexedSet);
    }

    public String acquirePermission() {
        int size = keyToLimiterEntries.size();
        while (true) {
            for (int i = 0; i < size; i++) {
                int idx = pointer.getAndIncrement() % size;
                Map.Entry<String, DefaultSlidingWindowRateLimiter> entry = keyToLimiterEntries.get(idx);
                if (entry.getValue().tryAcquirePermission()) {
                    return entry.getKey();
                }

                Interrupts.runOrSwallow(() -> Thread.sleep(FINNHUB_RATE_LIMITER_ROTATION_DEBOUNCE_MS),
                        e -> log.error(e.getMessage(), e));
            }
        }
    }
}
