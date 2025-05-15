package com.lucas.server.components.tradingbot.marketdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.AlphavantageMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

import static com.lucas.server.common.Constants.Granularity;

@Component
public class AlphavantageMarketDataClient {

    private final AlphavantageMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(AlphavantageMarketDataClient.class);
    private static final EnumMap<Granularity, String> granularityToFunctionPath = new EnumMap<>(Map.of(
            Granularity.DAILY, Constants.ALPHAVANTAGE_QUOTE_PATH,
            Granularity.WEEKLY, Constants.TIME_SERIES_WEEKLY
    ));
    private static final Map<Granularity, JsonToMarketDataFunction> granularityToMapper = Map.of(
            Granularity.DAILY, (s, m, j) -> List.of(m.map(j).setSymbol(s)),
            Granularity.WEEKLY, (s, m, j) -> m.mapAll(j, s).stream()
                    .sorted(Comparator.comparing(MarketData::getDate))
                    .toList()
    );

    @FunctionalInterface
    private interface JsonToMarketDataFunction {
        List<MarketData> apply(Symbol symbol, AlphavantageMarketResponseMapper mapper, JsonNode jsonNode) throws JsonProcessingException;
    }

    public AlphavantageMarketDataClient(AlphavantageMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                        @Value("${alphavantage.endpoint}") String endpoint, @Value("${alphavantage.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    private List<MarketData> retrieveMarketData(Symbol symbol, Granularity granularity) throws JsonProcessingException, ClientException {
        logger.info(Constants.RETRIEVING_MARKET_DATA_INFO, symbol);
        String url = UriComponentsBuilder
                .fromUriString(endpoint)
                .queryParam("function", granularityToFunctionPath.get(granularity))
                .queryParam("symbol", symbol.getName())
                .queryParam("apikey", apiKey)
                .toUriString();

        return granularityToMapper.get(granularity).apply(symbol, mapper, httpRequestClient.fetch(url));
    }

    public List<MarketData> retrieveMarketData(List<Symbol> symbols, Granularity granularity) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            res.addAll(this.retrieveMarketData(symbol, granularity));
            Constants.backOff(1000);
        }
        return res;
    }
}
