package com.lucas.server.components.tradingbot.marketdata.controller;

import com.lucas.server.common.ClientException;
import com.lucas.server.common.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.AlphavantageMarketDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketDataController {

    private final AlphavantageMarketDataClient client;
    private final MarketDataJpaService jpaService;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    public MarketDataController(AlphavantageMarketDataClient client, MarketDataJpaService jpaService) {
        this.client = client;
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<MarketData> fetchAndSave(@PathVariable String symbol) {
        MarketData entity;
        try {
            entity = client.retrieveMarketData(symbol);
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        jpaService.save(entity);
        return ResponseEntity.ok(entity);
    }

    @GetMapping("/historic/{symbol}")
    public ResponseEntity<List<MarketData>> fetchAndSaveHistoric(@PathVariable String symbol) {
        List<MarketData> entities;
        try {
            entities = client.retrieveWeeklySeries(symbol);
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        jpaService.saveAll(entities);
        return ResponseEntity.ok(entities);
    }
}
