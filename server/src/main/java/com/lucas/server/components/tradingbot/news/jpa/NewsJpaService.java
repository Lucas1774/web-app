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

import java.time.*;
import java.util.LinkedHashSet;
import java.util.List;

import static com.lucas.server.common.Constants.*;

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
        ZonedDateTime easternTime = ZonedDateTime.now(NY_ZONE);

        LocalDate lastDate = getLastDate(easternTime);
        LocalTime close = !EARLY_CLOSE_DATES_2025.contains(lastDate) ? MARKET_CLOSE : EARLY_CLOSE;
        LocalDateTime startUtc = ZonedDateTime.of(lastDate, close, NY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = easternTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        Pageable pageRequest = PageRequest.of(
                0, limit, Sort.by("date").descending()
        );

        return this.repository.findBySymbols_IdAndDateBetweenAndSentimentNotOrSymbols_IdAndDateBetweenAndSentimentIsNull(
                        symbolId, startUtc, endUtc, "neutral",
                        symbolId, startUtc, endUtc, pageRequest
                )
                .getContent();
    }

    private static LocalDate getLastDate(ZonedDateTime easternTime) {
        LocalDate lastDate = easternTime.toLocalDate();

        if (easternTime.toLocalTime().isBefore(MARKET_CLOSE)) {
            lastDate = lastDate.minusDays(1);
        }

        while (lastDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || lastDate.getDayOfWeek() == DayOfWeek.SUNDAY
                || isHoliday(lastDate)) {
            lastDate = lastDate.minusDays(1);
        }

        return lastDate;
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

    public List<News> generateSentiment(List<Long> list, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<News> news = repository.findAllBySymbols_IdInAndDateBetween(list, from, to);
        return sentimentClient.generateSentiment(news);
    }

    public void removeOrphanedNews() {
        repository.deleteBySymbolsIsEmpty();
    }
}
