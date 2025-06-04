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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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
        LocalDate today = LocalDate.now();
        LocalDate lastMarketDay;
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastMarketDay = today.minusDays(2);
        } else if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            lastMarketDay = today.minusDays(3);
        } else {
            lastMarketDay = today.minusDays(1);
        }
        Pageable pageRequest = PageRequest.of(
                0, limit, Sort.by("date").descending()
                        .and(Sort.by("sentimentConfidence").descending())
        );

        return this.repository.findBySymbols_IdAndDateBetweenAndSentimentNotOrSymbols_IdAndDateBetweenAndSentimentIsNull(
                        symbolId, lastMarketDay, today, "neutral",
                        symbolId, lastMarketDay, today, pageRequest
                )
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
