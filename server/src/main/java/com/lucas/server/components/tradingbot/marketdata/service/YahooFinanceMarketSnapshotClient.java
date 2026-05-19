package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.marketdata.mapper.YahooFinanceMarketResponseMapper;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.lucas.server.common.Constants.MARKET_SNAPSHOT;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.YAHOO_FINANCE_RATE_LIMITER;

@Component
@Slf4j
public class YahooFinanceMarketSnapshotClient {

    private final YahooFinanceMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;

    public YahooFinanceMarketSnapshotClient(YahooFinanceMarketResponseMapper mapper,
                                            HttpRequestClient httpRequestClient,
                                            Map<String, SlidingWindowRateLimiter> rateLimiters,
                                            @Value("${yahoo.market.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        rateLimiter = rateLimiters.get(YAHOO_FINANCE_RATE_LIMITER);
        this.endpoint = endpoint;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public MarketSnapshotDomain retrieveMarketSnapshot(SymbolDomain symbol) throws ClientException, MappingException {
        rateLimiter.acquirePermission();
        log.info(RETRIEVING_DATA_INFO, MARKET_SNAPSHOT, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + "/" + symbol.getName().replace('.', '-'))
                .queryParam("interval", "1h")
                .queryParam("includePrePost", true)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url, true), symbol);
    }
}
