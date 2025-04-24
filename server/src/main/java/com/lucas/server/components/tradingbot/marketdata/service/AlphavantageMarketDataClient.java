package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.AlphavantageMarketResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Comparator;
import java.util.List;

@Service
public class AlphavantageMarketDataClient {

    private final AlphavantageMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;

    @Value("${market-data.endpoint}")
    String endpoint;

    @Value("${market-data.api-key}")
    String apiKey;

    private static final String QUOTE_PATH = "GLOBAL_QUOTE";
    private static final String TIME_SERIES_WEEKLY = "TIME_SERIES_WEEKLY";

    public AlphavantageMarketDataClient(AlphavantageMarketResponseMapper mapper, HttpRequestClient httpRequestClient) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
    }

    public MarketData retrieveMarketData(String symbol) throws JsonProcessingException, HttpClientErrorException {
        String url = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("function", QUOTE_PATH)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        return mapper.map(this.httpRequestClient.fetch(url));
    }

    public List<MarketData> retrieveWeeklySeries(String symbol) throws JsonProcessingException, HttpClientErrorException {
        String url = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("function", TIME_SERIES_WEEKLY)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        return mapper.mapAll(this.httpRequestClient.fetch(url), symbol)
                .stream()
                .sorted(Comparator.comparing(MarketData::getDate))
                .toList();
    }
}
