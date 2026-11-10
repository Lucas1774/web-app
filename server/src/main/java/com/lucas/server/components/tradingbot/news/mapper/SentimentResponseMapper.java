package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.SENTIMENT;

@Component
public class SentimentResponseMapper implements Mapper<JsonNode, NewsDomain> {

    private static final Set<String> SENTIMENTS = Set.of("positive", "negative", "neutral", "irrelevant");

    public void mapAll(OrderedIndexedSet<NewsDomain> candidates, JsonNode jsonNode) throws MappingException {
        try {
            Map<Integer, JsonNode> byIndex = new HashMap<>();
            for (JsonNode node : jsonNode.isObject() ? List.of(jsonNode) : jsonNode) {
                byIndex.put(node.get("id").asInt(), node);
            }
            for (int i = 0; i < candidates.size(); i++) {
                NewsDomain mapped = map(byIndex.get(i + 1));
                candidates.get(i)
                        .setSentiment(mapped.getSentiment())
                        .setSentimentConfidence(mapped.getSentimentConfidence());
            }
        } catch (MappingException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, SENTIMENT), e);
        }
    }

    @Override
    public NewsDomain map(JsonNode json) throws MappingException {
        try {
            // TODO: same validation on other mappers
            JsonNode sentiment = json.required("sentiment");
            JsonNode score = json.required("score");
            if (!SENTIMENTS.contains(sentiment.asString())) {
                throw new IllegalStateException("Invalid sentiment label: " + sentiment);
            }
            BigDecimal value = score.decimalValue();
            if (!score.isNumber() || 0 > value.signum() || 0 < value.compareTo(BigDecimal.ONE)) {
                throw new IllegalStateException("Score out of range [0, 1]: " + score);
            }
            return new NewsDomain().setSentiment(sentiment.asString())
                    .setSentimentConfidence(value.multiply(BigDecimal.valueOf(100)));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, SENTIMENT), e);
        }
    }
}