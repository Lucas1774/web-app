package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NewsJpaService implements JpaService<News> {

    @Delegate
    private final GenericJpaServiceDelegate<News, NewsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<News> uniqueConstraintDelegate;
    private final NewsRepository repository;
    private final NewsSentimentClient sentimentClient;

    public NewsJpaService(NewsRepository repository, NewsSentimentClient sentimentClient) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.sentimentClient = sentimentClient;
    }

    public List<News> getTopForSymbolId(Long symbolId, int limit) {
        // This query will prioritize null before high sentiment within a single date.
        // That shouldn't be an issue as long as sentiment is generated per date.
        return this.repository.findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(symbolId, "neutral", symbolId, PageRequest.of(
                        0, limit, Sort.by("date").descending()
                                .and(Sort.by("sentimentConfidence").descending())
                ))
                .getContent();
    }

    public List<News> createOrUpdate(List<News> entities) {
        return uniqueConstraintDelegate.createOrUpdate(entity -> repository.findByExternalId(entity.getExternalId()),
                (oldEntity, newEntity) -> {
                    for (Symbol symbol : newEntity.getSymbols()) {
                        oldEntity.getSymbols().add(symbol);
                        symbol.getNews().add(oldEntity);
                    }
                    return oldEntity;
                },
                entities);
    }

    public List<News> generateSentiment(List<Long> list, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<News> news = repository.findAllBySymbols_IdInAndDateBetween(list, from, to);
        return sentimentClient.generateSentiment(news);
    }

    public void removeOrphanedNews() {
        repository.deleteBySymbolsIsEmpty();
    }
}
