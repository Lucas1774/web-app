package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{symbol}")
    public ResponseEntity<List<News>> fetchAndSaveLatest(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveLatestNews(symbol));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{symbol}/{from}")
    public ResponseEntity<List<News>> fetchAndSaveHistoric(
            @PathVariable String symbol,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        try {
            return ResponseEntity.ok(this.jpaService.retrieveNewsByDateRange(symbol, from, LocalDate.now()));
        } catch (JsonProcessingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
