package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.FinnhubRateLimiter;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.mapper.FinnhubMarketResponseMapper;
import com.lucas.utils.exception.MappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import static com.lucas.server.common.Constants.MARKET_DATA;
import static com.lucas.server.common.Constants.QUOTE;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.SYMBOL;

@Component
@Slf4j
public class FinnhubMarketDataClient {

    private final FinnhubMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final FinnhubRateLimiter finnhubRateLimiter;
    private final String endpoint;

    public FinnhubMarketDataClient(FinnhubMarketResponseMapper mapper,
                                   HttpRequestClient httpRequestClient,
                                   FinnhubRateLimiter finnhubRateLimiter,
                                   @Value("${finnhub.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.finnhubRateLimiter = finnhubRateLimiter;
        this.endpoint = endpoint;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public MarketDataDomain retrieveMarketData(SymbolDomain symbol) throws ClientException, MappingException {
        String apiKey = finnhubRateLimiter.acquirePermission();
        log.info(RETRIEVING_DATA_INFO, MARKET_DATA, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + QUOTE)
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("token", apiKey)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url, false), symbol);
    }
}
