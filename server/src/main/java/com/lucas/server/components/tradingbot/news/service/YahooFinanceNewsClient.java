package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.mapper.YahooFinanceNewsResponseMapper;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Set;

import static com.lucas.server.common.Constants.NEWS;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.YAHOO_FINANCE_RATE_LIMITER;

@Component
@Slf4j
public class YahooFinanceNewsClient {

    private final YahooFinanceNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;

    public YahooFinanceNewsClient(YahooFinanceNewsResponseMapper mapper,
                                  HttpRequestClient httpRequestClient,
                                  Map<String, SlidingWindowRateLimiter> rateLimiters,
                                  @Value("${yahoo.news.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        rateLimiter = rateLimiters.get(YAHOO_FINANCE_RATE_LIMITER);
        this.endpoint = endpoint;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public Set<NewsDomain> retrieveNews(SymbolDomain symbol) throws ClientException, MappingException {
        rateLimiter.acquirePermission();
        log.info(RETRIEVING_DATA_INFO, NEWS, symbol);
        String symbolName = symbol.getName().replace('.', '-');
        String url = UriComponentsBuilder.fromUriString(endpoint)
                .queryParam("s", symbolName)
                .queryParam("region", "US")
                .queryParam("lang", "en-US")
                .toUriString();

        return mapper.mapAll(httpRequestClient.fetchXml(url), symbol);
    }
}
