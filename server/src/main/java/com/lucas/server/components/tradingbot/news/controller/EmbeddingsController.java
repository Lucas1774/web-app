package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/embeddings")
public class EmbeddingsController {

    private final NewsEmbeddingsClient client;
    private final NewsJpaService service;
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingsController.class);

    public EmbeddingsController(NewsEmbeddingsClient client, NewsJpaService service) {
        this.client = client;
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<News> generateEmbeddingsByNewsId(@PathVariable Long id) {
        return service.findById(id)
                .flatMap(entity -> {
                    try {
                        return service.save(client.embed(entity));
                    } catch (ClientException e) {
                        logger.error(e.getMessage(), e);
                        return Optional.empty();
                    }
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
