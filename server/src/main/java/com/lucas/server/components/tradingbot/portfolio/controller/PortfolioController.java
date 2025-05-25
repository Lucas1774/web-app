package com.lucas.server.components.tradingbot.portfolio.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    public PortfolioController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @PostMapping("/buy")
    public ResponseEntity<PortfolioBase> buy(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                             @RequestParam BigDecimal quantity, @RequestParam BigDecimal commission,
                                             @RequestParam boolean mock, @RequestParam(required = false) LocalDate date) {
        LocalDateTime effectiveDate = date == null ? LocalDateTime.now() : date.atStartOfDay();
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(type, symbolName, price, quantity, commission, effectiveDate, true));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<PortfolioBase> sell(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                              @RequestParam BigDecimal quantity, @RequestParam boolean mock,
                                              @RequestParam(required = false) LocalDate date) {
        LocalDateTime effectiveDate = date == null ? LocalDateTime.now() : date.atStartOfDay();
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(type, symbolName, price, quantity, null, effectiveDate, false));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("/stand")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStandAll(@RequestParam boolean mock,
                                                                                @RequestParam boolean dynamic) {
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        try {
            return ResponseEntity.ok(jpaService.getPortfolioStand(SP500_SYMBOLS, type, dynamic));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stand/{symbols}")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStandSome(@PathVariable List<String> symbols,
                                                                                 @RequestParam boolean mock,
                                                                                 @RequestParam boolean dynamic) {
        PortfolioType type = mock ? PortfolioType.MOCK : PortfolioType.REAL;
        try {
            return ResponseEntity.ok(jpaService.getPortfolioStand(symbols, type, dynamic));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
