package com.lucas.server.components.tradingbot.portfolio.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import jakarta.servlet.http.HttpServletRequest;
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

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    public PortfolioController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @PostMapping("/buy")
    public ResponseEntity<PortfolioBase> buy(HttpServletRequest request,
                                             @RequestParam Long symbolId,
                                             @RequestParam BigDecimal price,
                                             @RequestParam BigDecimal quantity,
                                             @RequestParam BigDecimal commission,
                                             @RequestParam(required = false) LocalDate date) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDateTime effectiveDate = date == null ? LocalDateTime.now() : date.atStartOfDay();
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(PortfolioType.MOCK, symbolId, price, quantity, commission, effectiveDate, true));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<PortfolioBase> sell(HttpServletRequest request,
                                              @RequestParam Long symbolId,
                                              @RequestParam BigDecimal price,
                                              @RequestParam BigDecimal quantity,
                                              @RequestParam(required = false) LocalDate date) {
        if (!controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDateTime effectiveDate = date == null ? LocalDateTime.now() : date.atStartOfDay();
        try {
            return ResponseEntity.ok(jpaService.executePortfolioAction(PortfolioType.MOCK, symbolId, price, quantity, null, effectiveDate, false));
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("/stand")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStandPortfolio(HttpServletRequest request, @RequestParam boolean dynamic) {
        if (dynamic && !controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.getPortfolioStand(SP500_SYMBOLS, PortfolioType.MOCK, dynamic));
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stand/all")
    public ResponseEntity<List<PortfolioManager.SymbolStand>> getGlobalStandAll() {
        return ResponseEntity.ok(jpaService.getAllAsPortfolio(SP500_SYMBOLS, PortfolioType.MOCK));
    }
}
