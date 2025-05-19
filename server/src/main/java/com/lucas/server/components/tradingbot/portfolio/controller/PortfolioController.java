package com.lucas.server.components.tradingbot.portfolio.controller;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.portfolio.jpa.Portfolio;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioMock;
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

import static com.lucas.server.common.Constants.PortfolioType.MOCK;
import static com.lucas.server.common.Constants.PortfolioType.REAL;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    public PortfolioController(DataManager jpaService) {
        this.jpaService = jpaService;
    }

    @PostMapping("/buy")
    public ResponseEntity<Portfolio> buy(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                         @RequestParam BigDecimal quantity) {
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(REAL, symbolName, price, quantity, LocalDateTime.now(), true));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<Portfolio> sell(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                          @RequestParam BigDecimal quantity) {
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(REAL, symbolName, price, quantity, LocalDateTime.now(), false));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("/stand")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStand() {
        return ResponseEntity.ok(jpaService.getPortfolioStand(REAL));
    }

    @PostMapping("/mock/buy")
    public ResponseEntity<PortfolioMock> buyMock(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                                 @RequestParam BigDecimal quantity, @RequestParam LocalDate date) {
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(MOCK, symbolName, price, quantity, date.atStartOfDay(), true));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PostMapping("/mock/sell")
    public ResponseEntity<PortfolioMock> sellMock(@RequestParam String symbolName, @RequestParam BigDecimal price,
                                                  @RequestParam BigDecimal quantity, @RequestParam LocalDate date) {
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(MOCK, symbolName, price, quantity, date.atStartOfDay(), false));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("/mock/stand")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStandMock() {
        return ResponseEntity.ok(jpaService.getPortfolioStand(MOCK));
    }
}
