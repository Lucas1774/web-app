package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
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
    private final String endpoint;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(FinnhubNewsClient.class);

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper, HttpRequestClient httpRequestClient,
                             @Value("${finnhub.endpoint}") String endpoint, @Value("${finnhub.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    private List<News> retrieveNewsByDateRange(Symbol symbol, LocalDate from, LocalDate to) throws JsonProcessingException, ClientException {
        logger.info(RETRIEVING_NEWS_INFO, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + COMPANY_NEWS)
                .queryParam("symbol", symbol.getName())
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(httpRequestClient.fetch(url), symbol);
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
            backOff(FINNHUB_BACKOFF_MILLIS);
        }

        return new ArrayList<>(newsByExternalId.values());
    }
}
