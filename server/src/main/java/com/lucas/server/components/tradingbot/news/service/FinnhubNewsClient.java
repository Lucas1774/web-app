package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.FinnhubNewsResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinnhubNewsClient {

    private final FinnhubNewsResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;

    @Value("${market-news.endpoint}")
    String endpoint;

    @Value("${market-news.api-key}")
    String apiKey;

    private static final String COMPANY_NEWS = "/company-news";

    public FinnhubNewsClient(FinnhubNewsResponseMapper mapper, HttpRequestClient httpRequestClient) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
    }

    public List<News> retrieveLatestNews(String symbol) throws JsonProcessingException, HttpClientErrorException {
        LocalDate to = LocalDate.now();
        return this.retrieveNewsByDateRange(symbol, to.minusDays(1), to);
    }

    public List<News> retrieveNewsByDateRange(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, HttpClientErrorException {
        String url = UriComponentsBuilder.fromUriString(endpoint + COMPANY_NEWS)
                .queryParam("symbol", symbol)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("token", apiKey)
                .toUriString();

        return mapper.mapAll(this.httpRequestClient.fetch(url), symbol);
    }
}
