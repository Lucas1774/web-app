package com.lucas.server.components.tradingbot.marketdata.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.utils.exception.MappingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

import static com.lucas.server.common.Constants.MarketDataType;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/market")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;
    private final String apiKey;

    public MarketDataController(ControllerUtil controllerUtil, DataManager jpaService, @Value("${security.api-key}") String apiKey) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
        this.apiKey = apiKey;
    }

    @GetMapping("last")
    public ResponseEntity<Set<MarketData>> fetchAndSaveAll(HttpServletRequest request) {
        return controllerUtil.<Set<MarketData>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveMarketData(SP500_SYMBOLS, MarketDataType.LAST, false));
                    } catch (ClientException | MappingException e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("last/{symbols}")
    public ResponseEntity<Set<MarketData>> fetchAndSaveSome(HttpServletRequest request, @PathVariable Set<String> symbols) {
        return controllerUtil.<Set<MarketData>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveMarketData(new HashSet<>(symbols), MarketDataType.LAST, false));
                    } catch (ClientException | MappingException e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/historic")
    public ResponseEntity<Set<MarketData>> fetchAndSaveHistoricAll(HttpServletRequest request) {
        return controllerUtil.<Set<MarketData>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveMarketData(SP500_SYMBOLS, MarketDataType.HISTORIC, false));
                    } catch (MappingException | ClientException e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/historic/{symbols}")
    public ResponseEntity<Set<MarketData>> fetchAndSaveHistoricSome(HttpServletRequest request, @PathVariable Set<String> symbols) {
        return controllerUtil.<Set<MarketData>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveMarketData(new HashSet<>(symbols), MarketDataType.HISTORIC, false));
                    } catch (MappingException | ClientException e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/historic/api/{symbols}")
    public ResponseEntity<Set<MarketData>> fetchAndSaveHistoricSomeApi(@RequestHeader("X-API-Key") String requestApiKey, @PathVariable Set<String> symbols) {
        if (!apiKey.equals(requestApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.retrieveMarketData(new HashSet<>(symbols), MarketDataType.HISTORIC, false));
        } catch (MappingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Set<MarketData>> purge(HttpServletRequest request, @RequestParam int toKeep) {
        return controllerUtil.<Set<MarketData>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> ResponseEntity.ok(jpaService.removeOldMarketData(toKeep)));
    }
}
