package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.YahooFinanceMarketResponseMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceMarketDataClient {

    private final YahooFinanceMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final RateLimiter rateLimiter;
    private final String endpoint;
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceMarketDataClient.class);

    public YahooFinanceMarketDataClient(YahooFinanceMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                        Map<String, RateLimiter> rateLimiters, @Value("${yahoo.market.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.rateLimiter = rateLimiters.get(YAHOO_FINANCE_RATE_LIMITER);
        this.endpoint = endpoint;
    }

    public MarketData retrieveMarketData(Symbol symbol) throws JsonProcessingException, ClientException {
        logger.info(RETRIEVING_DATA_INFO, MARKET_DATA, symbol);
        rateLimiter.acquirePermission();
        String url = UriComponentsBuilder.fromUriString(endpoint + "/" + symbol.getName())
                .queryParam("interval", "1h")
                .queryParam("includePrePost", true)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url), symbol);
    }
}
