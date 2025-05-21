package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);

    public NewsController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("/last")
    public ResponseEntity<List<News>> fetchAndSaveAll() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRange(Constants.SP500_SYMBOLS, from, to));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/last/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveSome(@PathVariable List<String> symbols) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRange(symbols, from, to));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{from}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricAll(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRange(Constants.SP500_SYMBOLS, from, LocalDate.now()));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{from}/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricSome(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRange(symbols, from, LocalDate.now()));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<News>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldNews(toKeep));
    }
}
