package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.JSON_MAPPING_ERROR;

@Component
public class RecommendationChatCompletionResponseMapper implements Mapper<JsonNode, Recommendation> {

    private final ObjectMapper objectMapper;

    public RecommendationChatCompletionResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Recommendation map(JsonNode json) throws JsonProcessingException {
        try {
            return new Recommendation()
                    .setDate(LocalDate.now())
                    .setAction(json.get("action").asText())
                    .setConfidence(new BigDecimal(json.get("confidence").asText()))
                    .setRationale(StringUtils.left(json.get("rationale").asText(), 512));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, "recommendation"), e);
        }
    }

    public List<Recommendation> mapAll(Map<Symbol, List<MarketData>> marketData, JsonNode jsonNode, String fixedMessage) throws JsonProcessingException {
        try {
            ArrayNode output = (ArrayNode) jsonNode.get("recommendations");

            List<Recommendation> recommendations = new ArrayList<>();
            Map<String, Symbol> symbolByName = marketData.keySet().stream().collect(Collectors.toMap(Symbol::getName, Function.identity()));
            Map<String, MarketData> latestMarketDataByName = marketData.values().stream()
                    .map(all -> all.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow())
                    .collect(Collectors.toMap(md -> md.getSymbol().getName(), Function.identity()));
            for (int i = 0; i < output.size(); i++) {
                JsonNode load = output.get(i);
                recommendations.add(map(load)
                        .setInput(i == 0 ? fixedMessage : "")
                        .setErrors(i == 0 ? objectMapper.readerForListOf(String.class).readValue(jsonNode.get("errors")) : new ArrayList<>())
                        .setMarketData(latestMarketDataByName.get(load.get("symbol").asText()))
                        .setSymbol(symbolByName.get(load.get("symbol").asText())));
            }

            return recommendations;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, "recommendations"), e);
        }
    }
}
