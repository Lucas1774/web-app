package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RecommendationEngineType;

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final DataManager jpaService;
    private final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

    public RecommendationsController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<List<Recommendation>> generateRecommendations(@PathVariable List<String> symbols,
                                                                        @RequestParam boolean llm,
                                                                        @RequestParam boolean mock,
                                                                        @RequestParam boolean sendFixmeRequest,
                                                                        @RequestParam boolean overwrite) {
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        RecommendationEngineType engineType = llm ? RecommendationEngineType.RAW : RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRecommendations(symbols, type, sendFixmeRequest, engineType, overwrite));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stand")
    public ResponseEntity<List<Recommendation>> generateStandRecommendations(@RequestParam boolean llm,
                                                                             @RequestParam boolean mock,
                                                                             @RequestParam boolean sendFixmeRequest,
                                                                             @RequestParam boolean overwrite) {
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        RecommendationEngineType engineType = llm ? RecommendationEngineType.RAW : RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRecommendationsForStand(type, sendFixmeRequest, engineType, overwrite));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(@PathVariable int count,
                                                                              @RequestParam boolean llm,
                                                                              @RequestParam boolean mock,
                                                                              @RequestParam boolean sendFixmeRequest,
                                                                              @RequestParam boolean overwrite) {
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        RecommendationEngineType engineType = llm ? RecommendationEngineType.RAW : RecommendationEngineType.AZURE;
        try {
            return ResponseEntity.ok(jpaService.getRandomRecommendations(type, count, sendFixmeRequest, engineType, overwrite));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
