package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.FinnhubMarketResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class FinnhubMarketDataClient {

    private final FinnhubMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;

    public FinnhubMarketDataClient(FinnhubMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                   @Value("${finnhub.endpoint}") String endpoint,
                                   @Value("${finnhub.api-key}") String apiKey) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public MarketData retrieveMarketData(String symbol) throws JsonProcessingException, ClientException {
        String url = UriComponentsBuilder.fromUriString(endpoint + Constants.QUOTE)
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .build()
                .toUriString();

        return mapper.map(this.httpRequestClient.fetch(url), symbol);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbols) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (String symbol : symbols) {
            MarketData updated = this.retrieveMarketData(symbol);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            res.add(updated);
        }

        return res;
    }
}
