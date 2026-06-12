package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.MARKET_DATA;

@Component
public class FinnhubMarketResponseMapper implements Mapper<JsonNode, MarketDataDomain> {

    public MarketDataDomain map(JsonNode json, SymbolDomain symbol) throws MappingException {
        return map(json).setSymbol(symbol);
    }

    @Override
    public MarketDataDomain map(JsonNode json) throws MappingException {
        try {
            return new MarketDataDomain().setOpen(new BigDecimal(json.get("o").asString()))
                    .setHigh(new BigDecimal(json.get("h").asString()))
                    .setLow(new BigDecimal(json.get("l").asString()))
                    .setPrice(new BigDecimal(json.get("c").asString()))
                    .setDate(Instant.ofEpochSecond(json.get("t").asLong()).atZone(ZoneOffset.UTC).toLocalDate())
                    .setPreviousClose(new BigDecimal(json.get("pc").asString()))
                    .setChange(new BigDecimal(json.get("d").asString()))
                    .setChangePercent(new BigDecimal(json.get("dp").asString()));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, MARKET_DATA), e);
        }
    }
}
