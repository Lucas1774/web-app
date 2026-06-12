package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.MARKET_DATA;
import static com.lucas.server.common.Constants.MARKET_STILL_OPEN_WARN;
import static com.lucas.server.common.Constants.SYMBOL;

@Component
@Slf4j
public class TwelveDataMarketResponseMapper implements Mapper<JsonNode, MarketDataDomain> {

    public MarketDataDomain map(JsonNode json, SymbolDomain symbol) throws MappingException {
        if (!symbol.getName().equals(json.path(SYMBOL).asString(null))) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA));
        }
        return map(json).setSymbol(symbol);
    }

    @Override
    public MarketDataDomain map(JsonNode json) throws MappingException {
        try {
            if (json.path("is_market_open").asBoolean(false)) {
                log.warn(MARKET_STILL_OPEN_WARN);
            }
            return new MarketDataDomain().setOpen(new BigDecimal(json.get("open").asString()))
                    .setHigh(new BigDecimal(json.get("high").asString()))
                    .setLow(new BigDecimal(json.get("low").asString()))
                    .setPrice(new BigDecimal(json.get("close").asString()))
                    .setDate(LocalDate.parse(json.get("datetime").asString()))
                    .setVolume(json.get("volume").asLong())
                    .setPreviousClose(new BigDecimal(json.get("previous_close").asString()))
                    .setChange(new BigDecimal(json.get("change").asString()))
                    .setChangePercent(new BigDecimal(json.get("percent_change").asString()));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA), e);
        }
    }

    public OrderedIndexedSet<MarketDataDomain> mapAll(JsonNode json, SymbolDomain symbol) throws MappingException {
        try {
            if (!symbol.getName().equals(json.at("/meta/symbol").asString(null))) {
                throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA));
            }
            JsonNode series = json.get("values");

            OrderedIndexedSet<MarketDataDomain> history = new OrderedIndexedSetImpl<>();
            for (JsonNode node : series) {
                LocalDate date = LocalDate.parse(node.get("datetime").asString().substring(0, 10));
                MarketDataDomain md = new MarketDataDomain().setSymbol(symbol)
                        .setDate(date)
                        .setOpen(new BigDecimal(node.get("open").asString()))
                        .setHigh(new BigDecimal(node.get("high").asString()))
                        .setLow(new BigDecimal(node.get("low").asString()))
                        .setPrice(new BigDecimal(node.get("close").asString()))
                        .setVolume(node.get("volume").asLong());
                history.add(md);
            }

            return OrderedIndexedSet.copyOf(history);
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA), e);
        }
    }
}
