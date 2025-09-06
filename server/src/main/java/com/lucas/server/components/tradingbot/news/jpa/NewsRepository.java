package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Slice<News> findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(
            Long symbolId1, String sentiment, Long symbolId2, Pageable pageable
    );

    List<News> findByExternalIdIn(Collection<Long> externalIds);

    List<News> findAllBySymbols_IdInAndDateBetween(Collection<Long> symbolIds, LocalDateTime from, LocalDateTime to);

    void deleteBySymbolsIsEmpty();
}
