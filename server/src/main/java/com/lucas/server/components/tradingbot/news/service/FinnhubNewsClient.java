package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.FinnhubRateLimiter;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.FinnhubNewsResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class FinnhubNewsClient {

    private final FinnhubNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final FinnhubRateLimiter finnhubRateLimiter;
    private final String endpoint;
    private static final Logger logger = LoggerFactory.getLogger(FinnhubNewsClient.class);

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper, HttpRequestClient httpRequestClient,
                             FinnhubRateLimiter finnhubRateLimiter, @Value("${finnhub.endpoint}") String endpoint) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.finnhubRateLimiter = finnhubRateLimiter;
        this.endpoint = endpoint;
    }

    private List<News> retrieveNewsByDateRange(Symbol symbol, LocalDate from, LocalDate to) throws JsonProcessingException, ClientException {
        logger.info(RETRIEVING_DATA_INFO, NEWS, symbol);
        String apiKey = finnhubRateLimiter.acquirePermission();
        String url = UriComponentsBuilder.fromUriString(endpoint + COMPANY_NEWS)
                .queryParam("symbol", symbol.getName())
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(httpRequestClient.fetch(url, false), symbol);
    }

    public List<News> retrieveNewsByDateRange(List<Symbol> symbols, LocalDate from, LocalDate to) throws ClientException, JsonProcessingException {
        Map<Long, News> newsByExternalId = new HashMap<>();
        for (Symbol symbol : symbols) {
            List<News> updated = retrieveNewsByDateRange(symbol, from, to);
            for (News news : updated) {
                newsByExternalId
                        .computeIfAbsent(news.getExternalId(), id -> news)
                        .addSymbol(symbol);
            }
        }

        return new ArrayList<>(newsByExternalId.values());
    }
}
