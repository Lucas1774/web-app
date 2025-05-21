package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/embeddings")
public class EmbeddingsController {

    private final NewsJpaService service;
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingsController.class);

    public EmbeddingsController(NewsJpaService service) {
        this.service = service;
    }

    @GetMapping("/{ids}")
    public ResponseEntity<List<News>> generateEmbeddingsByNewsId(@PathVariable List<Long> ids) {
        try {
            return ResponseEntity.ok(service.generateEmbeddingsByNewsId(ids));
        } catch (ClientException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
