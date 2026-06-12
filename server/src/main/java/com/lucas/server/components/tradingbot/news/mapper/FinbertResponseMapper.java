package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.text.MessageFormat;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.SENTIMENT;

@Component
public class FinbertResponseMapper implements Mapper<JsonNode, NewsDomain> {

    public NewsDomain map(JsonNode json, NewsDomain news) throws MappingException {
        NewsDomain sentiment = map(json);
        return news.setSentiment(sentiment.getSentiment()).setSentimentConfidence(sentiment.getSentimentConfidence());
    }

    @Override
    public NewsDomain map(JsonNode json) throws MappingException {
        try {
            return new NewsDomain().setSentiment(json.get("label").asString())
                    .setSentimentConfidence(new BigDecimal(json.get("score")
                            .asString()).multiply(BigDecimal.valueOf(100)));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, SENTIMENT), e);
        }
    }
}
