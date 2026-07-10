package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.FinnhubRateLimiter;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.mapper.FinnhubNewsResponseMapper;
import com.lucas.utils.exception.MappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Set;

import static com.lucas.server.common.Constants.COMPANY_NEWS;
import static com.lucas.server.common.Constants.NEWS;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.SYMBOL;

@Component
@Slf4j
public class FinnhubNewsClient {

    private final FinnhubNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final FinnhubRateLimiter finnhubRateLimiter;
    private final String endpoint;

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper,
                             HttpRequestClient httpRequestClient,
                             FinnhubRateLimiter finnhubRateLimiter,
                             @Value("${finnhub.endpoint}") String endpoint) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.finnhubRateLimiter = finnhubRateLimiter;
        this.endpoint = endpoint;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public Set<NewsDomain> retrieveNewsByDateRange(SymbolDomain symbol, LocalDate from, LocalDate to)
            throws ClientException, MappingException {
        String apiKey = finnhubRateLimiter.acquirePermission();
        log.info(RETRIEVING_DATA_INFO, NEWS, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + COMPANY_NEWS)
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(httpRequestClient.get(url, false), symbol);
    }
}
