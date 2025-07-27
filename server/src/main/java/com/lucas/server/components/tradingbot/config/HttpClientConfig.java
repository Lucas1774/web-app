package com.lucas.server.components.tradingbot.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Configuration
public class HttpClientConfig {

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy(new RetryOptions(new ExponentialBackoffOptions().setMaxRetries(0)));
    }

    @Bean
    public HttpClient httpClient() {
        return new OkHttpAsyncHttpClientBuilder()
                .responseTimeout(Duration.ofMinutes(10))
                .build();
    }

    @Bean
    public Map<String, RateLimiter> rateLimiter() {
        Map<String, RateLimiter> res = new HashMap<>();
        res.put(AI_PER_MINUTE_RATE_LIMITER,
                RateLimiter.of(AI_PER_MINUTE_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofMinutes(1).dividedBy(24))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(5))
                        .build()));
        res.put(AI_PER_SECOND_RATE_LIMITER,
                RateLimiter.of(AI_PER_SECOND_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(2))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build()));
        res.put(TWELVEDATA_RATE_LIMITER,
                RateLimiter.of(TWELVEDATA_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofMinutes(1).dividedBy(7))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build()));
        res.put(YAHOO_FINANCE_RATE_LIMITER,
                RateLimiter.of(YAHOO_FINANCE_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1).dividedBy(4))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build()));
        FINNHUB_RATE_LIMITERS.forEach(name -> res.put(name,
                RateLimiter.of(name, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build())));
        return res;
    }
}
