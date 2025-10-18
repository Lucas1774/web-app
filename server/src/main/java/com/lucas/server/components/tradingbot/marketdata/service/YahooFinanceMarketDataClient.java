package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.config.SlidingWindowRateLimiter;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.YahooFinanceMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceMarketDataClient {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceMarketDataClient.class);
    private final YahooFinanceMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;

    public YahooFinanceMarketDataClient(YahooFinanceMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                        Map<String, SlidingWindowRateLimiter> rateLimiters, @Value("${yahoo.market.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.rateLimiter = rateLimiters.get(YAHOO_FINANCE_RATE_LIMITER);
        this.endpoint = endpoint;
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Retryable(retryFor = {ClientException.class, JsonProcessingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public MarketData retrieveMarketData(Symbol symbol) throws ClientException, JsonProcessingException {
        rateLimiter.acquirePermission();
        logger.info(RETRIEVING_DATA_INFO, PREMARKET, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + "/" + symbol.getName())
                .queryParam("interval", "1h")
                .queryParam("includePrePost", true)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url, true), symbol);
    }
}
