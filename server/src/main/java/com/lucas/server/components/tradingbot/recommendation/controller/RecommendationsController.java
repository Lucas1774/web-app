package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private final Map<String, AIClient> clients;

    public RecommendationsController(ControllerUtil controllerUtil, DataManager jpaService, Map<String, AIClient> clients) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
        this.clients = clients;
    }

    @GetMapping("/{symbols}")
    public ResponseEntity<List<Recommendation>> generateRecommendations(HttpServletRequest request,
                                                                        @PathVariable List<Long> symbols,
                                                                        @RequestParam boolean overwrite,
                                                                        @RequestParam(required = false) List<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<AIClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.NOT_RANDOM)
                : models.stream().map(clients::get).toList();
        return ResponseEntity.ok(jpaService.getRecommendationsById(symbols, getPortfolioType(username),
                overwrite, false, selectedClients));
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Recommendation>> generateRandomRecommendations(HttpServletRequest request,
                                                                              @PathVariable int count,
                                                                              @RequestParam boolean overwrite,
                                                                              @RequestParam(required = false) List<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<AIClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.RANDOM)
                : models.stream().map(clients::get).toList();
        return ResponseEntity.ok(jpaService.getRandomRecommendations(SP500_SYMBOLS, getPortfolioType(username), count,
                overwrite, false, false, selectedClients));
    }

    @GetMapping("/models")
    public ResponseEntity<List<String>> getModels() {
        return ResponseEntity.ok(clients.keySet().stream().sorted(String::compareTo).toList());
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<Recommendation>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep));
    }

    private PortfolioType getPortfolioType(String username) {
        return controllerUtil.isAdmin(username) ? PortfolioType.REAL : PortfolioType.MOCK;
    }
}
