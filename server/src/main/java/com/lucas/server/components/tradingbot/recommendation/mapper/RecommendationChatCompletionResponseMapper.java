package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;

import static com.lucas.server.common.Constants.*;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Component
public class RecommendationChatCompletionResponseMapper implements Mapper<JsonNode, Recommendation> {

    @Override
    public Recommendation map(JsonNode json) throws JsonProcessingException {
        try {
            return new Recommendation()
                    .setDate(LocalDate.now())
                    .setAction(json.get("action").asText())
                    .setConfidence(new BigDecimal(json.get("confidence").asText()))
                    .setRationale(StringUtils.left(json.get("rationale").asText(), 1024));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, RECOMMENDATION), e);
        }
    }

    public List<Recommendation> mapAll(List<DataManager.SymbolPayload> payload, JsonNode jsonNode, String message,
                                       String model) throws JsonProcessingException {
        try {
            List<Recommendation> recommendations = new ArrayList<>();
            Map<String, Symbol> symbolByName = new HashMap<>();
            Map<String, MarketData> latestMarketDataByName = new HashMap<>();
            Map<String, List<News>> newsByName = new HashMap<>();
            for (DataManager.SymbolPayload p : payload) {
                String name = p.getSymbol().getName();
                symbolByName.put(name, p.getSymbol());
                MarketData latest = p.getMarketData().stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow();
                latestMarketDataByName.put(name, latest);
                newsByName.put(name, p.getNews());
            }

            if (jsonNode.isObject()) {
                String symbolName = jsonNode.get(SYMBOL).asText();
                return List.of(map(jsonNode)
                        .setModel(model)
                        .setInput(message)
                        .setErrors(EMPTY_STRING)
                        .setMarketData(latestMarketDataByName.get(symbolName))
                        .setSymbol(symbolByName.get(symbolName))
                        .addNews(newsByName.get(symbolName)));
            }

            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode load = jsonNode.get(i);
                String symbolName = load.get(SYMBOL).asText();
                recommendations.add(map(load)
                        .setModel(model)
                        .setInput(message)
                        .setErrors(EMPTY_STRING)
                        .setMarketData(latestMarketDataByName.get(symbolName))
                        .setSymbol(symbolByName.get(symbolName))
                        .addNews(newsByName.get(symbolName)));
            }

            return recommendations;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, "recommendations"), e);
        }
    }
}
