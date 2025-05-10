package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
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

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final RecommendationChatCompletionClient client;
    private final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

    public RecommendationsController(RecommendationChatCompletionClient client) {
        this.client = client;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<String> generateRecommendations(@PathVariable List<String> symbols) {
        try {
            return ResponseEntity.ok(client.getRecommendations(symbols));
        } catch (ClientException | IllegalStateException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
