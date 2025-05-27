package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(SentimentController.class);

    public SentimentController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @GetMapping("/historic")
    public ResponseEntity<List<News>> fetchAndSaveHistoricAll(HttpServletRequest request,
                                                              @RequestParam(required = false) LocalDate from) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate effectiveDate = from == null ? LocalDate.now().minusDays(1) : from;
        try {
            return ResponseEntity.ok(jpaService.generateSentiment(SP500_SYMBOLS, effectiveDate, LocalDate.now()));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricSome(HttpServletRequest request,
                                                               @PathVariable List<String> symbols,
                                                               @RequestParam(required = false) LocalDate from) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDate effectiveDate = from == null ? LocalDate.now().minusDays(1) : from;
        try {
            return ResponseEntity.ok(jpaService.generateSentiment(symbols, effectiveDate, LocalDate.now()));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
