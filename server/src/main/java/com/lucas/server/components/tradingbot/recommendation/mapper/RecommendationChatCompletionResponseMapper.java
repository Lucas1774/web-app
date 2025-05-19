package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RecommendationChatCompletionResponseMapper implements Mapper<JsonNode, Recommendation> {

    private final ObjectMapper objectMapper;

    public RecommendationChatCompletionResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Recommendation map(JsonNode json) throws JsonProcessingException {
        try {
            return new Recommendation().setDate(LocalDate.now())
                    .setAction(json.get("action").asText())
                    .setConfidence(new BigDecimal(json.get("confidence").asText()))
                    .setRationale(json.get("rationale").asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "recommendation"), e);
        }
    }

    public List<Recommendation> mapAll(List<Symbol> symbols, JsonNode jsonNode, Message fixedMessage) throws JsonProcessingException {
        try {
            ArrayNode output = (ArrayNode) jsonNode.get("recommendations");
            ArrayNode errors = (ArrayNode) jsonNode.get("errors");

            List<Recommendation> recommendations = new ArrayList<>();
            Map<String, Symbol> symbolByName = symbols.stream().collect(Collectors.toMap(Symbol::getName, Function.identity()));
            for (int i = 0; i < output.size(); i++) {
                JsonNode load = output.get(i);
                recommendations.add(this.map(load)
                        .setErrors(objectMapper.readerForListOf(String.class).readValue(errors))
                        .setInput(i == 0 ? fixedMessage.getText() : "")
                        .setSymbol(symbolByName.get(load.get("symbol").asText())));
            }

            return recommendations;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "recommendations"), e);
        }
    }
}
