package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.FinnhubMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.lucas.server.common.Constants.*;

@Component
public class FinnhubMarketDataClient {

    private final FinnhubMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(FinnhubMarketDataClient.class);

    public FinnhubMarketDataClient(FinnhubMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                   @Value("${finnhub.endpoint}") String endpoint,
                                   @Value("${finnhub.api-key}") String apiKey) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public MarketData retrieveMarketData(Symbol symbol) throws JsonProcessingException, ClientException {
        logger.info(RETRIEVING_MARKET_DATA_INFO, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + QUOTE)
                .queryParam("symbol", symbol.getName())
                .queryParam("token", apiKey)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url), symbol);
    }

    public List<MarketData> retrieveMarketData(List<Symbol> symbols) throws JsonProcessingException, ClientException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            res.add(retrieveMarketData(symbol));
            backOff(FINNHUB_BACKOFF_MILLIS);
        }
        return res;
    }
}
