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
            JsonNode quote = json.path("Global Quote");
            MarketData md = new MarketData();
            md.setSymbol(quote.path("01. symbol").asText());
            md.setOpen(new BigDecimal(quote.path("02. open").asText()));
            md.setHigh(new BigDecimal(quote.path("03. high").asText()));
            md.setLow(new BigDecimal(quote.path("04. low").asText()));
            md.setPrice(new BigDecimal(quote.path("05. price").asText()));
            md.setVolume(quote.path("06. volume").asLong());
            md.setDate(LocalDate.parse(quote.path("07. latest trading day").asText(), Constants.DATE_FMT));
            md.setPreviousClose(new BigDecimal(quote.path("08. previous close").asText()));
            md.setChange(new BigDecimal(quote.path("09. change").asText()));
            md.setChangePercent(quote.path("10. change percent").asText());

            return md;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"), e.getCause());
        }
    }

    public List<MarketData> mapAll(JsonNode json, String symbol) throws JsonProcessingException {
        try {
            JsonNode series = json.path("Weekly Time Series");

            List<MarketData> history = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = series.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                LocalDate date = LocalDate.parse(entry.getKey(), Constants.DATE_FMT);
                JsonNode data = entry.getValue();
                MarketData md = new MarketData();
                md.setSymbol(symbol);
                md.setOpen(new BigDecimal(data.path("1. open").asText()));
                md.setHigh(new BigDecimal(data.path("2. high").asText()));
                md.setLow(new BigDecimal(data.path("3. low").asText()));
                md.setPrice(new BigDecimal(data.path("4. close").asText()));
                md.setVolume(data.path("5. volume").asLong());
                md.setDate(date);
                history.add(md);
            }

            return history;
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"), e.getCause());
        }
    }
}
