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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    // TODO: batch
    public List<News> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(
                symbolId, "neutral", symbolId, PageRequest.of(
                        0, limit, Sort.by("date").descending()
                )
        ).getContent();
    }

    public List<News> createOrUpdate(List<News> entities) {
        return uniqueConstraintDelegate.createOrUpdate(allEntities -> repository.findByExternalIdIn(
                        allEntities.stream().map(News::getExternalId).toList()
                ),
                (oldEntity, newEntity) -> {
                    for (Symbol symbol : newEntity.getSymbols()) {
                        oldEntity.getSymbols().add(symbol);
                        symbol.getNews().add(oldEntity);
                    }
                    return oldEntity;
                },
                new LinkedHashSet<>(entities));
    }

    public List<News> generateSentiment(List<Long> list, LocalDateTime from, LocalDateTime to)
            throws ClientException, JsonProcessingException {
        List<News> newsList = repository.findAllBySymbols_IdInAndDateBetween(list, from, to);
        List<News> res = new ArrayList<>(newsList.size());
        for (News news : newsList) {
            if (null != news.getSentiment() && null != news.getSentimentConfidence()) {
                continue;
            }
            res.add(sentimentClient.generateSentiment(news));
        }
        return res;
    }

    public void removeOrphanedNews() {
        repository.deleteBySymbolsIsEmpty();
    }
}
