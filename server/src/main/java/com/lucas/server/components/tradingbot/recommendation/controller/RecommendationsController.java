package com.lucas.server.components.tradingbot.recommendation.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.BUY;
import static com.lucas.server.common.Constants.DEFAULT_USERNAME;
import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RecommendationMode;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;
import static com.lucas.server.common.Constants.UTC_ZONE;
import static com.lucas.server.common.Constants.filterClients;
import static com.lucas.server.common.Constants.getFineGrainClientNames;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationsController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private final Map<String, AiClient> clients;

    @GetMapping("/{symbols}")
    public ResponseEntity<Set<RecommendationDomain>> generateRecommendations(HttpServletRequest request,
                                                                             @PathVariable Set<Long> symbols,
                                                                             @RequestParam boolean overwrite,
                                                                             @RequestParam boolean afterHoursContext,
                                                                             @RequestParam boolean useOldNews,
                                                                             @RequestParam(required = false)
                                                                             Set<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<AiClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.NOT_RANDOM)
                : models.stream().map(clients::get).collect(Collectors.toUnmodifiableSet());
        return ResponseEntity.ok(jpaService.getRecommendationsById(symbols,
                selectedClients,
                DataManager.CheekyClients.empty(),
                Set.of(),
                getPortfolioType(username),
                overwrite,
                false,
                afterHoursContext,
                useOldNews));
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<Set<RecommendationDomain>> generateRandomRecommendations(HttpServletRequest request,
                                                                                   @PathVariable int count,
                                                                                   @RequestParam boolean overwrite,
                                                                                   @RequestParam
                                                                                   boolean afterHoursContext,
                                                                                   @RequestParam boolean useOldNews,
                                                                                   @RequestParam(required = false)
                                                                                   Set<String> models) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<AiClient> selectedClients = null == models
                ? filterClients(clients, RecommendationMode.RANDOM)
                : models.stream().map(clients::get).collect(Collectors.toUnmodifiableSet());
        return ResponseEntity.ok(jpaService.getRandomRecommendations(SP500_SYMBOLS,
                selectedClients,
                DataManager.CheekyClients.empty(),
                Set.of(),
                getPortfolioType(username),
                count,
                overwrite,
                false,
                false,
                afterHoursContext,
                useOldNews));
    }

    @GetMapping("/models")
    public ResponseEntity<Set<String>> getModels() {
        return ResponseEntity.ok(clients.keySet()
                .stream()
                .sorted(String::compareTo)
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet()));
    }

    @GetMapping("/daily/{confidenceThreshold}")
    public ResponseEntity<Set<RecommendationDomain>> getDailyRecommendations(
            @PathVariable BigDecimal confidenceThreshold,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Set<String> models) {
        LocalDate selectedDate = null == date ? LocalDate.now(UTC_ZONE) : date;
        String selectedAction = null == action ? BUY : action;
        Set<String> selectedClients = null == models ? getFineGrainClientNames(clients) : models;
        return ResponseEntity.ok(jpaService.getDailyRecommendations(confidenceThreshold,
                selectedDate,
                selectedAction,
                selectedClients));
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Set<RecommendationDomain>> purge(HttpServletRequest request, @RequestParam int toKeep) {
        return controllerUtil.<Set<RecommendationDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> ResponseEntity.ok(jpaService.removeOldRecommendations(toKeep)));
    }

    private PortfolioType getPortfolioType(String username) {
        return controllerUtil.isAdmin(username) ? PortfolioType.REAL : PortfolioType.MOCK;
    }
}
