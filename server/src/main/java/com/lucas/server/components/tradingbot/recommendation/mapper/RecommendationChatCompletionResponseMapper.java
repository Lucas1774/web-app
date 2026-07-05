package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.RECOMMENDATION;
import static com.lucas.server.common.Constants.SYMBOL;
import static com.lucas.server.common.Constants.UTC_ZONE;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Component
public class RecommendationChatCompletionResponseMapper implements Mapper<JsonNode, RecommendationDomain> {

    public Set<RecommendationDomain> mapAll(Set<DataManager.SymbolPayload> payload,
                                            JsonNode jsonNode,
                                            String message,
                                            String model) throws MappingException {
        try {
            Set<RecommendationDomain> recommendations = new HashSet<>();
            Map<String, SymbolDomain> symbolByName = new HashMap<>();
            Map<String, MarketDataDomain> latestMarketDataByName = new HashMap<>();
            Map<String, Set<NewsDomain>> newsByName = new HashMap<>();
            for (DataManager.SymbolPayload p : payload) {
                String name = p.getSymbol().getName();
                symbolByName.put(name, p.getSymbol());
                MarketDataDomain latest =
                        p.getMarketData().stream().max(Comparator.comparing(MarketDataDomain::getDate)).orElseThrow();
                latestMarketDataByName.put(name, latest);
                newsByName.put(name, p.getNews());
            }

            if (jsonNode.isObject()) {
                String symbolName = jsonNode.get(SYMBOL).asString();
                return Set.of(map(jsonNode).setModel(model)
                        .setInput(message)
                        .setErrors(EMPTY_STRING)
                        .setMarketDataId(latestMarketDataByName.get(symbolName).getId())
                        .setSymbol(symbolByName.get(symbolName))
                        .addNews(newsByName.get(symbolName)));
            }

            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode load = jsonNode.get(i);
                String symbolName = load.get(SYMBOL).asString();
                recommendations.add(map(load).setModel(model)
                        .setInput(message)
                        .setErrors(EMPTY_STRING)
                        .setMarketDataId(latestMarketDataByName.get(symbolName).getId())
                        .setSymbol(symbolByName.get(symbolName))
                        .addNews(newsByName.get(symbolName)));
            }

            return recommendations;
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, "recommendations"), e);
        }
    }

    @Override
    public RecommendationDomain map(JsonNode json) throws MappingException {
        try {
            return new RecommendationDomain().setDate(LocalDate.now(UTC_ZONE))
                    .setAction(json.get("action").asString())
                    .setConfidence(new BigDecimal(json.get("confidence").asString()))
                    .setRationale(StringUtils.left(json.get("rationale").asString(), 1024));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, RECOMMENDATION), e);
        }
    }
}
