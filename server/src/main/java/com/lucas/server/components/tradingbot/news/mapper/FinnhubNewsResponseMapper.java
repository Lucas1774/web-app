package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lucas.server.common.Constants.JSON_MAPPING_ERROR;

@Component
public class FinnhubNewsResponseMapper implements Mapper<JsonNode, News> {

    @Override
    public News map(JsonNode json) throws JsonProcessingException {
        try {
            long externalId = json.get("id").asLong();
            LocalDateTime date = Instant
                    .ofEpochSecond(json.get("datetime").asLong())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime();

            return new News()
                    .setExternalId(externalId)
                    .setDate(date)
                    .setHeadline(json.get("headline").asText())
                    .setSummary(StringUtils.left(json.get("summary").asText(), 1024))
                    .setUrl(json.get("url").asText())
                    .setSource(json.get("source").asText())
                    .setCategory(json.get("category").asText())
                    .setImage(json.get("image").asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, "news"), e);
        }
    }

    public List<News> mapAll(JsonNode json, Symbol symbol) throws JsonProcessingException {
        if (!json.isArray()) {
            return Collections.emptyList();
        }

        List<News> newsList = new ArrayList<>();
        for (JsonNode node : json) {
            newsList.add(map(node).addSymbol(symbol));
        }

        return newsList;
    }
}
