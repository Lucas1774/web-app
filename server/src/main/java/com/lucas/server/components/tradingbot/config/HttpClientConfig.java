package com.lucas.server.components.tradingbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Configuration
public class HttpClientConfig {

    @Bean
    public Map<String, SlidingWindowRateLimiter> rateLimiter() {
        Map<String, SlidingWindowRateLimiter> res = new HashMap<>();
        res.put(AI_PER_MINUTE_RATE_LIMITER, new SlidingWindowRateLimiter(24, Duration.ofMinutes(1)));
        res.put(AI_PER_SECOND_RATE_LIMITER, new SlidingWindowRateLimiter(1, Duration.ofSeconds(1)));
        res.put(TWELVEDATA_RATE_LIMITER, new SlidingWindowRateLimiter(8, Duration.ofMinutes(1)));
        res.put(YAHOO_FINANCE_RATE_LIMITER, new SlidingWindowRateLimiter(1, Duration.ofSeconds(1).dividedBy(4)));
        FINNHUB_RATE_LIMITERS.forEach(name -> res.put(name,
                new SlidingWindowRateLimiter(60, Duration.ofMinutes(1), Duration.ofMinutes(1))));

        return res;
    }
}
