package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.lucas.server.common.Constants.*;

@Component
public class TwelveDataMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    private static final Logger logger = LoggerFactory.getLogger(TwelveDataMarketResponseMapper.class);

    @Override
    public MarketData map(JsonNode json) throws JsonProcessingException {
        try {
            if (json.path("is_market_open").asBoolean(false)) {
                logger.warn(MARKET_STILL_OPEN_WARN);
            }
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
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA), e);
        }
    }

    public MarketData map(JsonNode json, Symbol symbol) throws JsonProcessingException {
        if (!symbol.getName().equals(json.path("symbol").asText(null))) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA));
        }
        return map(json).setSymbol(symbol);
    }

    public List<MarketData> mapAll(JsonNode json, Symbol symbol) throws JsonProcessingException {
        try {
            if (!symbol.getName().equals(json.at("/meta/symbol").asText(null))) {
                throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA));
            }
            JsonNode series = json.get("values");

            List<MarketData> history = new ArrayList<>(series.size());
            for (JsonNode node : series) {
                LocalDate date = LocalDate.parse(node.get("datetime").asText().substring(0, 10));
                MarketData md = new MarketData()
                        .setSymbol(symbol)
                        .setDate(date)
                        .setOpen(new BigDecimal(node.get("open").asText()))
                        .setHigh(new BigDecimal(node.get("high").asText()))
                        .setLow(new BigDecimal(node.get("low").asText()))
                        .setPrice(new BigDecimal(node.get("close").asText()))
                        .setVolume(node.get("volume").asLong());
                history.add(md);
            }

            return history;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA), e);
        }
    }
}
