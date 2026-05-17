package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.utils.exception.MappingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

import static com.lucas.server.common.Constants.SP500_SYMBOLS;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final ControllerUtil controllerUtil;
    private final DataManager jpaService;

    @GetMapping("/last")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveAll(HttpServletRequest request) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveNewsByName(SP500_SYMBOLS));
                    } catch (ClientException | MappingException e) {
                        log.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/last/{symbols}")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveSome(HttpServletRequest request, @PathVariable Set<Long> symbols) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveNewsById(symbols));
                    } catch (ClientException | MappingException e) {
                        log.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/historic/{from}")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveHistoricAll(HttpServletRequest request, @PathVariable LocalDate from) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveNewsByDateRangeAndName(SP500_SYMBOLS, from, LocalDate.now(), false));
                    } catch (MappingException | ClientException e) {
                        log.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @GetMapping("/historic/{from}/{symbols}")
    public ResponseEntity<Set<NewsDomain>> fetchAndSaveHistoricSome(HttpServletRequest request,
                                                                    @PathVariable LocalDate from,
                                                                    @PathVariable Set<Long> symbols) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    try {
                        return ResponseEntity.ok(jpaService.retrieveNewsByDateRangeAndId(symbols, from, LocalDate.now()));
                    } catch (MappingException | ClientException e) {
                        log.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                });
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Set<NewsDomain>> purge(HttpServletRequest request, @RequestParam int toKeep) {
        return controllerUtil.<Set<NewsDomain>>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> ResponseEntity.ok(jpaService.removeOldNews(toKeep)));
    }
}
