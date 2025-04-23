package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.MarketApiResponseToMarketData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class MarketDataClient {

    private final RestTemplate restTemplate;

    @Value("${market-data.endpoint}")
    String endpoint;

    @Value("${market-data.api-key}")
    String apiKey;
    private final MarketApiResponseToMarketData mapper;

    public MarketDataClient(RestTemplate restTemplate, MarketApiResponseToMarketData mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public MarketData retrieveMarketData(String symbol) throws JsonProcessingException {
        String url = String.format(
                "%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                endpoint, symbol, apiKey
        );
        String response = restTemplate.getForObject(url, String.class);

        return this.mapper.map(response);
    }

    public List<MarketData> retrieveWeeklySeries(String symbol) throws JsonProcessingException {
        String url = String.format(
                "%s?function=TIME_SERIES_WEEKLY&symbol=%s&apikey=%s",
                endpoint, symbol, apiKey
        );
        String response = restTemplate.getForObject(url, String.class);

        return this.mapper.mapAll(response, symbol);
    }

}
