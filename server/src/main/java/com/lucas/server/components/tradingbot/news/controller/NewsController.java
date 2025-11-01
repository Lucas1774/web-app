package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.utils.exception.MappingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.lucas.server.common.Constants.DEFAULT_USERNAME;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/news")
public class NewsController {

    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);
    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;

    public NewsController(ControllerUtil controllerUtil, DataManager jpaService) {
        this.controllerUtil = controllerUtil;
        this.jpaService = jpaService;
    }

    @GetMapping("/last")
    public ResponseEntity<List<News>> fetchAndSaveAll(HttpServletRequest request) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByName(SP500_SYMBOLS));
        } catch (ClientException | MappingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/last/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveSome(HttpServletRequest request, @PathVariable List<Long> symbols) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsById(symbols));
        } catch (ClientException | MappingException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{from}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricAll(HttpServletRequest request, @PathVariable LocalDate from) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRangeAndName(SP500_SYMBOLS, from, LocalDate.now()));
        } catch (MappingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/historic/{from}/{symbols}")
    public ResponseEntity<List<News>> fetchAndSaveHistoricSome(HttpServletRequest request,
                                                               @PathVariable LocalDate from,
                                                               @PathVariable List<Long> symbols) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(jpaService.retrieveNewsByDateRangeAndId(symbols, from, LocalDate.now()));
        } catch (MappingException | ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<List<News>> purge(@RequestParam int toKeep) {
        return ResponseEntity.ok(jpaService.removeOldNews(toKeep));
    }
}
