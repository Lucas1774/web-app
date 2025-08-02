package com.lucas.server.components.tradingbot.marketdata.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lucas.server.common.Constants.MarketDataType;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/market")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    private final DataManager jpaService;

    public MarketDataController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("last")
    public ResponseEntity<List<MarketData>> fetchAndSaveAll() {
        try {
            return ResponseEntity.ok(jpaService.retrieveMarketData(SP500_SYMBOLS, MarketDataType.LAST));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("last/{symbols}")
    public ResponseEntity<List<MarketData>> fetchAndSaveSome(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(jpaService.retrieveMarketData(symbols, MarketDataType.LAST));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic")
    public ResponseEntity<List<MarketData>> fetchAndSaveHistoricAll() {
        try {
            return ResponseEntity.ok(jpaService.retrieveMarketData(SP500_SYMBOLS, MarketDataType.HISTORIC));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{symbols}")
    public ResponseEntity<List<MarketData>> fetchAndSaveHistoricSome(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(jpaService.retrieveMarketData(symbols, MarketDataType.HISTORIC));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<MarketData>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldMarketData(toKeep));
    }
}
