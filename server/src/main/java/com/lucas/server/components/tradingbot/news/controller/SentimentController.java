package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static com.lucas.server.common.Constants.DEFAULT_USERNAME;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;

    public SentimentController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @GetMapping("/historic")
    public ResponseEntity<Set<News>> fetchAndSaveHistoricAll(HttpServletRequest request,
                                                             @RequestParam(required = false) LocalDate from) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDateTime effectiveDate = null == from ? LocalDate.now().minusDays(1).atStartOfDay() : from.atStartOfDay();
        return ResponseEntity.ok(jpaService.generateSentiment(SP500_SYMBOLS, effectiveDate, LocalDate.now().plusDays(1).atStartOfDay()));
    }

    @GetMapping("/historic/{symbols}")
    public ResponseEntity<Set<News>> fetchAndSaveHistoricSome(HttpServletRequest request,
                                                              @PathVariable Set<String> symbols,
                                                              @RequestParam(required = false) LocalDate from) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDateTime effectiveDate = null == from ? LocalDate.now().minusDays(1).atStartOfDay() : from.atStartOfDay();
        return ResponseEntity.ok(jpaService.generateSentiment(symbols, effectiveDate, LocalDate.now().plusDays(1).atStartOfDay()));
    }
}
