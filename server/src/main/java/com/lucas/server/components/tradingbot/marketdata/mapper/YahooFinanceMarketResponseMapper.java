package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.MARKET_DATA;

@Component
public class YahooFinanceMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    @Override
    public MarketData map(JsonNode json) throws JsonProcessingException {
        try {
            JsonNode quote = json.get("indicators").get("quote").get(0);

            BigDecimal maxHigh = new BigDecimal(quote.get("high").get(0).asText());
            for (JsonNode node : quote.get("high")) {
                BigDecimal value = new BigDecimal(node.asText());
                if (value.compareTo(maxHigh) > 0) {
                    maxHigh = value;
                }
            }

            BigDecimal minLow = new BigDecimal(quote.get("low").get(0).asText());
            for (JsonNode node : quote.get("high")) {
                BigDecimal value = new BigDecimal(node.asText());
                if (value.compareTo(minLow) < 0) {
                    minLow = value;
                }
            }

            return new MarketData()
                    .setOpen(new BigDecimal(quote.get("open").get(0).asText()))
                    .setHigh(maxHigh)
                    .setLow(minLow)
                    .setPrice(new BigDecimal(quote.get("close").get(quote.get("close").size() - 1).asText()));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA), e);
        }
    }

    public MarketData map(JsonNode json, Symbol symbol) throws JsonProcessingException {
        try {
            JsonNode result = json.get("chart").get("result").get(0);
            if (!symbol.getName().equals(result.get("meta").get("symbol").asText())) {
                throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA));
            }
            return map(result).setSymbol(symbol);
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA), e);
        }
    }
}
