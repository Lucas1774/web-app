package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.FinnhubNewsResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Component
public class FinnhubNewsClient {

    private final FinnhubNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper, HttpRequestClient httpRequestClient,
                             @Value("${finnhub.endpoint}") String endpoint, @Value("${finnhub.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public List<News> retrieveLatestNews(String symbol) throws JsonProcessingException, ClientException {
        LocalDate to = LocalDate.now();
        return this.retrieveNewsByDateRange(symbol, to.minusDays(1), to);
    }

    public List<News> retrieveNewsByDateRange(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, ClientException {
        String url = UriComponentsBuilder.fromUriString(endpoint + Constants.COMPANY_NEWS)
                .queryParam("symbol", symbol)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(this.httpRequestClient.fetch(url), symbol);
    }
}
