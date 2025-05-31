package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RECOMMENDATION_CLIENTS;

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

    public RecommendationsController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<List<Recommendation>> generateRecommendations(HttpServletRequest request,
                                                                        @PathVariable List<Long> symbols,
                                                                        @RequestParam boolean sendFixmeRequest,
                                                                        @RequestParam boolean overwrite) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.getRecommendationsById(symbols, PortfolioType.MOCK, sendFixmeRequest,
                    overwrite, RECOMMENDATION_CLIENTS));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(HttpServletRequest request,
                                                                              @PathVariable int count,
                                                                              @RequestParam boolean sendFixmeRequest,
                                                                              @RequestParam boolean overwrite) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.getRandomRecommendations(PortfolioType.MOCK, count, sendFixmeRequest, overwrite));
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<Recommendation>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep));
    }
}
