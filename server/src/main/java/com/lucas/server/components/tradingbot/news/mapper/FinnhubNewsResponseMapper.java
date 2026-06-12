package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.NEWS;

@Component
public class FinnhubNewsResponseMapper implements Mapper<JsonNode, NewsDomain> {

    public Set<NewsDomain> mapAll(JsonNode json, SymbolDomain symbol) throws MappingException {
        if (!json.isArray()) {
            return Set.of();
        }

        Set<NewsDomain> newsList = new HashSet<>();
        for (JsonNode node : json) {
            newsList.add(map(node).addSymbol(symbol));
        }

        return newsList;
    }

    @Override
    public NewsDomain map(JsonNode json) throws MappingException {
        try {
            return new NewsDomain().setExternalId(json.get("id").asLong())
                    .setDate(Instant.ofEpochSecond(json.get("datetime").asLong())
                            .atZone(ZoneOffset.UTC)
                            .toLocalDateTime())
                    .setHeadline(json.get("headline").asString())
                    .setSummary(StringUtils.left(json.get("summary").asString(), 1024))
                    .setUrl(json.get("url").asString())
                    .setSource(json.get("source").asString())
                    .setCategory(json.get("category").asString())
                    .setImage(json.get("image").asString());
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, NEWS), e);
        }
    }
}
