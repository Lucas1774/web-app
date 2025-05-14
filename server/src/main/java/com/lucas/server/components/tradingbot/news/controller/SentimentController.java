package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
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
@RequestMapping("/sentiment")
public class SentimentController {

    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(SentimentController.class);

    public SentimentController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("/historic/{from}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricAll(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        try {
            return ResponseEntity.ok(this.jpaService.generateSentiment(Constants.SP500_SYMBOLS, from, LocalDate.now()));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{from}/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricSome(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(this.jpaService.generateSentiment(symbols, from, LocalDate.now()));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
