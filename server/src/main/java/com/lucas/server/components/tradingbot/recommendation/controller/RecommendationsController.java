package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
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
    public ResponseEntity<List<Recommendation>> generateRecommendations(@PathVariable List<String> symbols) {
        Constants.RecommendationEngineType type = symbols.size() > Constants.RECOMMENDATIONS_CHUNK_SIZE
                ? Constants.RecommendationEngineType.RAW : Constants.RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRecommendations(symbols, REAL, true, type));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/mock/{symbols}")
    public ResponseEntity<List<Recommendation>> generateRecommendationsMock(@PathVariable List<String> symbols) {
        Constants.RecommendationEngineType type = symbols.size() > Constants.RECOMMENDATIONS_CHUNK_SIZE
                ? Constants.RecommendationEngineType.RAW : Constants.RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRecommendations(symbols, MOCK, true, type));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(@PathVariable int count) {
        Constants.RecommendationEngineType type = count > Constants.RECOMMENDATIONS_CHUNK_SIZE
                ? Constants.RecommendationEngineType.RAW : Constants.RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRandomRecommendations(REAL, count, true, type));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/random/mock/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendationsMock(@PathVariable int count) {
        Constants.RecommendationEngineType type = count > Constants.RECOMMENDATIONS_CHUNK_SIZE
                ? Constants.RecommendationEngineType.RAW : Constants.RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRandomRecommendations(MOCK, count, true, type));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
