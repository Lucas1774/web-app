package com.lucas.server.components.tradingbot.news.controller;

import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/embeddings")
public class EmbeddingsController {

    private final NewsEmbeddingsClient client;
    private final NewsJpaService service;

    public EmbeddingsController(NewsEmbeddingsClient client, NewsJpaService service) {
        this.client = client;
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<News> generateEmbeddingsByNewsId(@PathVariable String id) {
        try {
            return service.findById(id)
                    .map(entity -> service.save(client.embed(entity))
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
