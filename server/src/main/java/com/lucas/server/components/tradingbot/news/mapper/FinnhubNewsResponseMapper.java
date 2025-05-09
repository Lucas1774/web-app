package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FinnhubNewsResponseMapper implements Mapper<JsonNode, News> {

    @Override
    public News map(JsonNode json) throws JsonProcessingException {
        try {
            long externalId = json.path("id").asLong();
            LocalDateTime date = Instant
                    .ofEpochSecond(json.path("datetime").asLong())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime();

            News news = new News();
            news.setExternalId(externalId);
            news.setDate(date);
            news.setHeadline(json.path("headline").asText());
            news.setSummary(json.path("summary").asText());
            news.setUrl(json.path("url").asText());
            news.setSource(json.path("source").asText());
            news.setCategory(json.path("category").asText());
            news.setImage(json.path("image").asText());

            return news;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "news"), e.getCause());
        }
    }

    private News map(JsonNode json, String symbol) throws JsonProcessingException {
        return this.map(json).setSymbol(symbol);
    }

    public List<News> mapAll(JsonNode json, String symbol) throws JsonProcessingException {
        if (!json.isArray()) {
            return Collections.emptyList();
        }

        List<News> newsList = new ArrayList<>();
        for (JsonNode node : json) {
            newsList.add(this.map(node, symbol));
        }

        return newsList;
    }
}
