package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.AlphavantageMarketResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Comparator;
import java.util.List;

@Component
public class AlphavantageMarketDataClient {

    private final AlphavantageMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;

    public AlphavantageMarketDataClient(AlphavantageMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                        @Value("${market-data.endpoint}") String endpoint, @Value("${market-data.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public MarketData retrieveMarketData(String symbol) throws JsonProcessingException, ClientException {
        String url = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("function", Constants.QUOTE_PATH)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        return mapper.map(this.httpRequestClient.fetch(url));
    }

    public List<MarketData> retrieveWeeklySeries(String symbol) throws JsonProcessingException, ClientException {
        String url = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("function", Constants.TIME_SERIES_WEEKLY)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        return mapper.mapAll(this.httpRequestClient.fetch(url), symbol)
                .stream()
                .sorted(Comparator.comparing(MarketData::getDate))
                .toList();
    }
}
