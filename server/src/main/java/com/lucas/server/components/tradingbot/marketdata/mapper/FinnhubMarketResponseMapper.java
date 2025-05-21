package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class FinnhubMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    @Override
    public MarketData map(JsonNode json) throws JsonProcessingException {
        try {
            return new MarketData()
                    .setOpen(new BigDecimal(json.get("o").asText()))
                    .setHigh(new BigDecimal(json.get("h").asText()))
                    .setLow(new BigDecimal(json.get("l").asText()))
                    .setPrice(new BigDecimal(json.get("c").asText()))
                    .setDate(Instant.ofEpochSecond(json.get("t").asLong())
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate())
                    .setPreviousClose(new BigDecimal(json.get("pc").asText()))
                    .setChange(new BigDecimal(json.get("d").asText()))
                    .setChangePercent(new BigDecimal(json.get("dp").asText()));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, Constants.MARKET), e);
        }
    }

    public MarketData map(JsonNode json, Symbol symbol) throws JsonProcessingException {
        return map(json).setSymbol(symbol);
    }
}
