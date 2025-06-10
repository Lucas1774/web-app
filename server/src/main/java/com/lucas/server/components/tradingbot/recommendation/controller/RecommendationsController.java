package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lucas.server.common.Constants.*;

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
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jpaService.getRecommendationsById(symbols, getPortfolioType(username),
                overwrite, RECOMMENDATION_CLIENTS));
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(HttpServletRequest request,
                                                                              @PathVariable int count,
                                                                              @RequestParam boolean overwrite) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(jpaService.getRandomRecommendations(getPortfolioType(username), count, overwrite, false));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<Recommendation>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep));
    }

    private PortfolioType getPortfolioType(String username) {
        return controllerUtil.isAdmin(username) ? PortfolioType.REAL : PortfolioType.MOCK;
    }
}
