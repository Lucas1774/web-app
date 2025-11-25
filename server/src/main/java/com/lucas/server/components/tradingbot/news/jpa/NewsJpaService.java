package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.RETRIEVAL_FAILED_WARN;
import static com.lucas.server.common.Constants.SENTIMENT;

@Service
public class NewsJpaService implements JpaService<News> {

    private static final Logger logger = LoggerFactory.getLogger(NewsJpaService.class);
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

    // TODO: batch
    public OrderedIndexedSet<News> getTopForSymbolId(Long symbolId, int limit) {
        return OrderedIndexedSet.copyOf(repository.findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(
                symbolId, "neutral", symbolId, PageRequest.of(
                        0, limit, Sort.by("date").descending()
                )
        ));
    }

    public Set<News> createOrUpdate(Set<News> entities) {
        return uniqueConstraintDelegate.createOrUpdate(allEntities -> repository.findByExternalIdIn(
                        allEntities.stream().map(News::getExternalId).collect(Collectors.toSet())
                ),
                (oldEntity, newEntity) -> {
                    for (Symbol symbol : newEntity.getSymbols()) {
                        oldEntity.getSymbols().add(symbol);
                        symbol.getNews().add(oldEntity);
                    }
                    return oldEntity;
                },
                entities);
    }

    public Set<News> generateSentiment(Set<Long> symbolIds, LocalDateTime from, LocalDateTime to) {
        return repository.findAllBySymbols_IdInAndDateBetween(symbolIds, from, to).stream()
                .filter(news -> null == news.getSentiment() || null == news.getSentimentConfidence())
                .flatMap(news -> {
                    try {
                        return Stream.of(sentimentClient.generateSentiment(news));
                    } catch (Exception e) {
                        logger.warn(RETRIEVAL_FAILED_WARN, SENTIMENT, news);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());
    }

    public Set<News> findOrphanedNews() {
        return repository.findBySymbolsIsEmpty();
    }
}
