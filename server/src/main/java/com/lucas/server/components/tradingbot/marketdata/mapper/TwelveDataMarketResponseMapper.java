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
import java.time.LocalDate;

@Component
public class TwelveDataMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    @Override
    public MarketData map(JsonNode json) throws JsonProcessingException {
        try {
            return new MarketData()
                    .setOpen(new BigDecimal(json.get("open").asText()))
                    .setHigh(new BigDecimal(json.get("high").asText()))
                    .setLow(new BigDecimal(json.get("low").asText()))
                    .setPrice(new BigDecimal(json.get("close").asText()))
                    .setDate(LocalDate.parse(json.get("datetime").asText()))
                    .setVolume(json.get("volume").asLong())
                    .setPreviousClose(new BigDecimal(json.get("previous_close").asText()))
                    .setChange(new BigDecimal(json.get("change").asText()))
                    .setChangePercent(new BigDecimal(json.get("percent_change").asText()));
        } catch (Exception e) {
            throw new JsonProcessingException(
                    MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"), e);
        }
    }

    public MarketData map(JsonNode json, Symbol symbol) throws JsonProcessingException {
        if (!symbol.getName().equals(json.path("symbol").asText(null))) {
            throw new JsonProcessingException(
                    MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"));
        }
        return this.map(json).setSymbol(symbol);
    }
}
