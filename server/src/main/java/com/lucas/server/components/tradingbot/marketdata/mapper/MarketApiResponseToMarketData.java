package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.Constants;
import com.lucas.server.common.JsonProcessingException;
import com.lucas.server.common.Mapper;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class MarketApiResponseToMarketData implements Mapper<String, MarketData> {

    private final ObjectMapper objectMapper;

    public MarketApiResponseToMarketData(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MarketData map(String json) throws JsonProcessingException {
        try {
            JsonNode quote = objectMapper.readTree(json).path("Global Quote");

            String symbol = quote.path("01. symbol").asText();
            BigDecimal open = new BigDecimal(quote.path("02. open").asText());
            BigDecimal high = new BigDecimal(quote.path("03. high").asText());
            BigDecimal low = new BigDecimal(quote.path("04. low").asText());
            BigDecimal price = new BigDecimal(quote.path("05. price").asText());
            Long volume = quote.path("06. volume").asLong();
            LocalDate lastTradingDay = LocalDate.parse(quote.path("07. latest trading day").asText(), Constants.DATE_FMT);
            BigDecimal previousClose = new BigDecimal(quote.path("08. previous close").asText());
            BigDecimal change = new BigDecimal(quote.path("09. change").asText());
            String changePercent = quote.path("10. change percent").asText();

            return new MarketData(symbol, open, high, low, price, volume, lastTradingDay, previousClose, change, changePercent);
        } catch (Exception e) {
            throw new JsonProcessingException("Error mapping market data", e.getCause());
        }
    }
}
