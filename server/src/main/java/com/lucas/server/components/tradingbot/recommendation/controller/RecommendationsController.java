package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseEntity<Set<Recommendation>> generateRecommendations(HttpServletRequest request,
                                                                       @PathVariable Set<Long> symbols,
                                                                       @RequestParam boolean overwrite,
                                                                       @RequestParam boolean afterHoursContext,
                                                                       @RequestParam boolean useOldNews,
                                                                       @RequestParam(required = false) Set<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<AIClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.NOT_RANDOM)
                : models.stream().map(clients::get).collect(Collectors.toSet());
        return ResponseEntity.ok(jpaService.getRecommendationsById(symbols, selectedClients, getPortfolioType(username),
                overwrite, false, afterHoursContext, useOldNews));
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<Set<Recommendation>> generateRandomRecommendations(HttpServletRequest request,
                                                                             @PathVariable int count,
                                                                             @RequestParam boolean overwrite,
                                                                             @RequestParam boolean afterHoursContext,
                                                                             @RequestParam boolean useOldNews,
                                                                             @RequestParam(required = false) Set<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<AIClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.RANDOM)
                : models.stream().map(clients::get).collect(Collectors.toSet());
        return ResponseEntity.ok(jpaService.getRandomRecommendations(SP500_SYMBOLS, selectedClients, getPortfolioType(username), count,
                overwrite, false, false, afterHoursContext, useOldNews));
    }

    @GetMapping("/models")
    public ResponseEntity<Set<String>> getModels() {
        return ResponseEntity.ok(clients.keySet().stream().sorted(String::compareTo).collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet()));
    }

    @GetMapping("/daily/{confidenceThreshold}")
    public ResponseEntity<Set<Recommendation>> getDailyRecommendations(@PathVariable BigDecimal confidenceThreshold,
                                                                       @RequestParam(required = false) LocalDate date,
                                                                       @RequestParam(required = false) String action,
                                                                       @RequestParam(required = false) Set<String> models) {
        LocalDate selectedDate = null == date ? LocalDate.now() : date;
        String selectedAction = null == action ? BUY : action;
        Set<String> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.FINE_GRAIN).stream().map(c -> c.getConfig().name())
                .collect(Collectors.toSet())
                : models;
        return ResponseEntity.ok(jpaService.getDailyRecommendations(confidenceThreshold, selectedDate, selectedAction, new HashSet<>(selectedClients)));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Set<Recommendation>> purge(HttpServletRequest request, @RequestParam int toKeep) {
        return controllerUtil.<Set<Recommendation>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep)));
    }

    private PortfolioType getPortfolioType(String username) {
        return controllerUtil.isAdmin(username) ? PortfolioType.REAL : PortfolioType.MOCK;
    }
}
