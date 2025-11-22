package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(
            Long symbolId1, String sentiment, Long symbolId2, Pageable pageable
    );

    Set<News> findByExternalIdIn(Set<Long> externalIds);

    Set<News> findAllBySymbols_IdInAndDateBetween(Set<Long> symbolIds, LocalDateTime from, LocalDateTime to);

    Set<News> findBySymbolsIsEmpty();
}
