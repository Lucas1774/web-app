package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.config.SlidingWindowRateLimiter;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.YahooFinanceNewsResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceNewsClient {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceNewsClient.class);
    private final YahooFinanceNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;

    public YahooFinanceNewsClient(YahooFinanceNewsResponseMapper mapper, HttpRequestClient httpRequestClient,
                                  Map<String, SlidingWindowRateLimiter> rateLimiters, @Value("${yahoo.news.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.rateLimiter = rateLimiters.get(YAHOO_FINANCE_RATE_LIMITER);
        this.endpoint = endpoint;
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Retryable(retryFor = {ClientException.class, JsonProcessingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public List<News> retrieveNews(Symbol symbol) throws ClientException, JsonProcessingException {
        rateLimiter.acquirePermission();
        logger.info(RETRIEVING_DATA_INFO, NEWS, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("s", symbol.getName())
                .queryParam("region", "US")
                .queryParam("lang", "en-US")
                .toUriString();

        return mapper.mapAll(httpRequestClient.fetchXml(url), symbol);
    }
}
