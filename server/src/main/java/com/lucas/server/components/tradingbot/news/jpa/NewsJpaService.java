package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import jakarta.transaction.Transactional;
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
    private final NewsEmbeddingsClient embeddingsClient;
    private final NewsSentimentClient sentimentClient;

    public NewsJpaService(NewsRepository repository, NewsEmbeddingsClient embeddingsClient, NewsSentimentClient sentimentClient) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.embeddingsClient = embeddingsClient;
        this.sentimentClient = sentimentClient;
    }

    public List<News> createIgnoringDuplicates(Iterable<News> entities, boolean triggerEntityCallback) {
        NewsListener.setActive(triggerEntityCallback);
        return this.createIgnoringDuplicates(entities);
    }

    private List<News> createIgnoringDuplicates(Iterable<News> entities) {
        return this.uniqueConstraintDelegate.createIgnoringDuplicates(entity -> this.repository.findByExternalId(entity.getExternalId()), entities);
    }

    public List<News> getTopForSymbolId(Long symbolId, int limit) {
        return this.repository.findBySymbols_IdAndSentimentNot(symbolId, "neutral", PageRequest.of(
                        0, limit, Sort.by("date").descending()
                                .and(Sort.by("sentimentConfidence").descending())
                ))
                .getContent();
    }

    @Transactional(rollbackOn = {ClientException.class})
    public List<News> generateEmbeddingsByNewsId(List<Long> ids) throws ClientException {
        List<News> news = this.repository.findAllByIdIn(ids);
        NewsListener.setActive(false);
        return this.embeddingsClient.embed(news);
    }

    public List<News> createOrUpdate(List<News> entities, boolean triggerEntityCallback) {
        NewsListener.setActive(triggerEntityCallback);
        return this.createOrUpdate(entities);
    }

    private List<News> createOrUpdate(List<News> entities) {
        return this.uniqueConstraintDelegate.createOrUpdate(entity -> this.repository.findByExternalId(entity.getExternalId()),
                (oldEntity, newEntity) -> {
                    oldEntity.getSymbols().addAll(newEntity.getSymbols());
                    return oldEntity;
                },
                entities);
    }

    public List<News> generateSentiment(List<Long> list, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<News> news = this.repository.findAllBySymbols_IdInAndDateBetween(list, from, to);
        NewsListener.setActive(false);
        return this.sentimentClient.generateSentiment(news);
    }

    public void removeOrphanedNews() {
        this.repository.deleteBySymbolsIsEmpty();
    }
}
