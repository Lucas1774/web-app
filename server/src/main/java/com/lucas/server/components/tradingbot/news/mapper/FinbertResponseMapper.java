package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.SENTIMENT;

@Component
public class FinbertResponseMapper implements Mapper<JsonNode, News> {

    @Override
    public News map(JsonNode json) throws MappingException {
        try {
            return new News()
                    .setSentiment(json.get("label").asText())
                    .setSentimentConfidence(new BigDecimal(json.get("score").asText()).multiply(BigDecimal.valueOf(100)));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, SENTIMENT), e);
        }
    }

    public News map(JsonNode json, News news) throws MappingException {
        News sentiment = map(json);
        return news.setSentiment(sentiment.getSentiment()).setSentimentConfidence(sentiment.getSentimentConfidence());
    }
}
