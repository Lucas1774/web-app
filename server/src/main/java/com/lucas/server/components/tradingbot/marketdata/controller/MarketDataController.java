package com.lucas.server.components.tradingbot.marketdata.controller;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
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

    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    public MarketDataController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<MarketData> fetchAndSave(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveMarketData(symbol));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{symbol}")
    public ResponseEntity<List<MarketData>> fetchAndSaveHistoric(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveWeeklySeries(symbol));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/batch")
    public ResponseEntity<List<MarketData>> fetchAndSaveAll() {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveMarketData(Constants.SP500_SYMBOLS));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/batch/{symbols}")
    public ResponseEntity<List<MarketData>> fetchAndSaveSome(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveMarketData(symbols));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
