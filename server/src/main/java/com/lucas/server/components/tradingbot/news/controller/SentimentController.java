package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static com.lucas.server.common.Constants.SP500_SYMBOLS;
import static com.lucas.server.common.Constants.UTC_ZONE;

@RestController
@RequestMapping("/sentiment")
@RequiredArgsConstructor
public class SentimentController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;

    @GetMapping("/historic")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveHistoricAll(HttpServletRequest request,
                                                                   @RequestParam(required = false) LocalDate from) {
        return fetchAndSaveHistoric(request, SP500_SYMBOLS, from);
    }

    @GetMapping("/historic/{symbols}")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveHistoricSome(HttpServletRequest request,
                                                                    @PathVariable Set<String> symbols,
                                                                    @RequestParam(required = false) LocalDate from) {
        return fetchAndSaveHistoric(request, symbols, from);
    }

    private ResponseEntity<Set<NewsDomain>> fetchAndSaveHistoric(HttpServletRequest request,
                                                                 Set<String> symbols,
                                                                 LocalDate from) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    LocalDateTime effectiveDate =
                            null == from ? LocalDate.now(UTC_ZONE).minusDays(1).atStartOfDay() : from.atStartOfDay();

                    return ResponseEntity.ok(jpaService.generateSentiment(symbols,
                            effectiveDate,
                            LocalDate.now(UTC_ZONE).plusDays(1).atStartOfDay()));
                });
    }
}
