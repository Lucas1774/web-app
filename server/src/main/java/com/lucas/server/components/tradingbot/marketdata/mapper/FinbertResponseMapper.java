package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

@Component
public class FinbertResponseMapper implements Mapper<JsonNode, News> {

    @Override
    public News map(JsonNode json) throws JsonProcessingException {
        try {
            return new News()
                    .setSentiment(json.get("label").asText())
                    .setSentimentConfidence(new BigDecimal(json.get("score").asText()).multiply(BigDecimal.valueOf(100)));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "sentiment"), e);
        }
    }

    public News map(JsonNode json, News news) throws JsonProcessingException {
        News sentiment = map(json);
        return news.setSentiment(sentiment.getSentiment()).setSentimentConfidence(sentiment.getSentimentConfidence());
    }
}
