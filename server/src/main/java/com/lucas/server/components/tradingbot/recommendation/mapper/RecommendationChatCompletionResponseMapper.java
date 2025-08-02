package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;

import static com.lucas.server.common.Constants.MAPPING_ERROR;

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
                    .setRationale(StringUtils.left(json.get("rationale").asText(), 1024));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, "recommendation"), e);
        }
    }

    public List<Recommendation> mapAll(List<DataManager.SymbolPayload> payload, JsonNode jsonNode, String message,
                                       String model) throws JsonProcessingException {
        try {
            ArrayNode output = (ArrayNode) jsonNode.get("recommendations");

            List<Recommendation> recommendations = new ArrayList<>();
            Map<String, Symbol> symbolByName = new HashMap<>();
            Map<String, MarketData> latestMarketDataByName = new HashMap<>();
            for (DataManager.SymbolPayload p : payload) {
                String name = p.getSymbol().getName();
                symbolByName.put(name, p.getSymbol());
                MarketData latest = p.getMarketData().stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow();
                latestMarketDataByName.put(name, latest);
            }
            String errors = objectMapper.readerForListOf(String.class).readValue(jsonNode.get("errors")).toString();

            for (int i = 0; i < output.size(); i++) {
                JsonNode load = output.get(i);
                recommendations.add(map(load)
                        .setModel(model)
                        .setInput(message)
                        .setErrors(errors)
                        .setMarketData(latestMarketDataByName.get(load.get("symbol").asText()))
                        .setSymbol(symbolByName.get(load.get("symbol").asText())));
            }

            return recommendations;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, "recommendations"), e);
        }
    }
}
