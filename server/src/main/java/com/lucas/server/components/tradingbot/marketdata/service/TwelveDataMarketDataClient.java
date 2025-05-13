package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.TwelveDataMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class TwelveDataMarketDataClient {

    private final TwelveDataMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(TwelveDataMarketDataClient.class);

    public TwelveDataMarketDataClient(TwelveDataMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                      @Value("${twelve-data.endpoint}") String endpoint,
                                      @Value("${twelve-data.api-key}") String apiKey) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    private MarketData retrieveMarketData(Symbol symbol) throws JsonProcessingException, ClientException {
        logger.info(Constants.RETRIEVING_MARKET_DATA_INFO, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + Constants.QUOTE)
                .queryParam("symbol", symbol.getName())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        return mapper.map(this.httpRequestClient.fetch(url), symbol);
    }

    public List<MarketData> retrieveMarketData(List<Symbol> symbols) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            MarketData updated = this.retrieveMarketData(symbol);
            try {
                Thread.sleep(7500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            res.add(updated);
        }

        return res;
    }
}
