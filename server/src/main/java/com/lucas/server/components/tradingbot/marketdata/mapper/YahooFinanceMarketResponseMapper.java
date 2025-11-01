package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    @Override
    public MarketData map(JsonNode json) throws MappingException {
        try {
            JsonNode quote = json.get("indicators").get("quote").get(0);

            BigDecimal maxHigh = new BigDecimal(quote.get("high").get(0).asText());
            for (JsonNode node : quote.get("high")) {
                BigDecimal value = new BigDecimal(node.asText());
                if (0 < value.compareTo(maxHigh)) {
                    maxHigh = value;
                }
            }

            BigDecimal minLow = new BigDecimal(quote.get("low").get(0).asText());
            for (JsonNode node : quote.get("high")) {
                BigDecimal value = new BigDecimal(node.asText());
                if (0 > value.compareTo(minLow)) {
                    minLow = value;
                }
            }

            return new MarketData()
                    .setOpen(new BigDecimal(quote.get("open").get(0).asText()))
                    .setHigh(maxHigh)
                    .setLow(minLow)
                    .setPrice(new BigDecimal(quote.get("close").get(quote.get("close").size() - 1).asText()));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, PREMARKET), e);
        }
    }

    public MarketData map(JsonNode json, Symbol symbol) throws MappingException {
        try {
            JsonNode result = json.get("chart").get("result").get(0);
            if (!symbol.getName().equals(result.get("meta").get(SYMBOL).asText())) {
                throw new MappingException(MessageFormat.format(MAPPING_ERROR, PREMARKET));
            }
            return map(result).setSymbol(symbol);
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, PREMARKET), e);
        }
    }
}
