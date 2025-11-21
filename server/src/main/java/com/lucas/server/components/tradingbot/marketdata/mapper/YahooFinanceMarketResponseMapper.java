package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceMarketResponseMapper implements Mapper<JsonNode, MarketSnapshot> {

    @Override
    public MarketSnapshot map(JsonNode json) throws MappingException {
        try {
            JsonNode quote = json.get("indicators").get("quote").get(0);
            JsonNode timeStamps = json.get("timestamp");

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

            return new MarketSnapshot()
                    .setDate(Instant.ofEpochSecond(timeStamps.get(timeStamps.size() - 1).asLong())
                            .atZone(ZoneOffset.UTC)
                            .toLocalDateTime())
                    .setOpen(new BigDecimal(quote.get("open").get(0).asText()))
                    .setHigh(maxHigh)
                    .setLow(minLow)
                    .setPrice(new BigDecimal(quote.get("close").get(quote.get("close").size() - 1).asText()));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_SNAPSHOT), e);
        }
    }

    public MarketSnapshot map(JsonNode json, Symbol symbol) throws MappingException {
        try {
            JsonNode result = json.get("chart").get("result").get(0);
            if (!symbol.getName().equals(result.get("meta").get(SYMBOL).asText())) {
                throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_SNAPSHOT));
            }
            return map(result).setSymbol(symbol);
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_SNAPSHOT), e);
        }
    }
}
