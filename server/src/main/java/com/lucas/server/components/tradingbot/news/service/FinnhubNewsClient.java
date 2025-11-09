package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.FinnhubRateLimiter;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.FinnhubNewsResponseMapper;
import com.lucas.utils.exception.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

import static com.lucas.server.common.Constants.*;

@Component
public class FinnhubNewsClient {

    private static final Logger logger = LoggerFactory.getLogger(FinnhubNewsClient.class);
    private final FinnhubNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final FinnhubRateLimiter finnhubRateLimiter;
    private final String endpoint;

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper, HttpRequestClient httpRequestClient,
                             FinnhubRateLimiter finnhubRateLimiter, @Value("${finnhub.endpoint}") String endpoint) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.finnhubRateLimiter = finnhubRateLimiter;
        this.endpoint = endpoint;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public List<News> retrieveNewsByDateRange(Symbol symbol, LocalDate from, LocalDate to) throws ClientException, MappingException {
        String apiKey = finnhubRateLimiter.acquirePermission();
        logger.info(RETRIEVING_DATA_INFO, NEWS, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + COMPANY_NEWS)
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(httpRequestClient.fetch(url, false), symbol);
    }
}
