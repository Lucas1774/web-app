package com.lucas.server.components.tradingbot.recommendation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.lucas.server.common.Constants.PortfolioType.MOCK;
import static com.lucas.server.common.Constants.PortfolioType.REAL;

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final DataManager jpaService;
    private final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

    public RecommendationsController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<JsonNode> generateRecommendations(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(this.jpaService.getRecommendations(symbols, REAL));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/mock/{symbols}")
    public ResponseEntity<JsonNode> generateRecommendationsMock(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(this.jpaService.getRecommendations(symbols, MOCK));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
