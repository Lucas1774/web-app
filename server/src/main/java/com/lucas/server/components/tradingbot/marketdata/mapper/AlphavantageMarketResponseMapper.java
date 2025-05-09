package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class AlphavantageMarketResponseMapper implements Mapper<JsonNode, MarketData> {

    @Override
    public MarketData map(JsonNode json) throws JsonProcessingException {
        try {
            JsonNode quote = json.get("Global Quote");
            return new MarketData()
                    .setSymbol(quote.get("01. symbol").asText())
                    .setOpen(new BigDecimal(quote.get("02. open").asText()))
                    .setHigh(new BigDecimal(quote.get("03. high").asText()))
                    .setLow(new BigDecimal(quote.get("04. low").asText()))
                    .setPrice(new BigDecimal(quote.get("05. price").asText()))
                    .setVolume(quote.get("06. volume").asLong())
                    .setDate(LocalDate.parse(quote.get("07. latest trading day").asText(), Constants.DATE_FMT))
                    .setPreviousClose(new BigDecimal(quote.get("08. previous close").asText()))
                    .setChange(new BigDecimal(quote.get("09. change").asText()))
                    .setChangePercent(quote.get("10. change percent").asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"), e);
        }
    }

    public List<MarketData> mapAll(JsonNode json, String symbol) throws JsonProcessingException {
        try {
            JsonNode series = json.get("Weekly Time Series");

            List<MarketData> history = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = series.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                LocalDate date = LocalDate.parse(entry.getKey(), Constants.DATE_FMT);
                JsonNode data = entry.getValue();
                MarketData md = new MarketData()
                        .setSymbol(symbol)
                        .setOpen(new BigDecimal(data.get("1. open").asText()))
                        .setHigh(new BigDecimal(data.get("2. high").asText()))
                        .setLow(new BigDecimal(data.get("3. low").asText()))
                        .setPrice(new BigDecimal(data.get("4. close").asText()))
                        .setVolume(data.get("5. volume").asLong())
                        .setDate(date);
                history.add(md);
            }

            return history;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"), e);
        }
    }
}
