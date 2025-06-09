package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RECOMMENDATION_CLIENTS;

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;

    public RecommendationsController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<List<Recommendation>> generateRecommendations(HttpServletRequest request,
                                                                        @PathVariable List<Long> symbols,
                                                                        @RequestParam boolean overwrite) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jpaService.getRecommendationsById(symbols, PortfolioType.MOCK,
                overwrite, RECOMMENDATION_CLIENTS));
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(HttpServletRequest request,
                                                                              @PathVariable int count,
                                                                              @RequestParam boolean overwrite) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jpaService.getRandomRecommendations(PortfolioType.MOCK, count, overwrite, false));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<Recommendation>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep));
    }
}
