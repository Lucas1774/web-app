package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Slice<News> findBySymbols_IdAndDateBetweenAndSentimentNotOrSymbols_IdAndDateBetweenAndSentimentIsNull(
            Long symbolId1, LocalDateTime startDate1, LocalDateTime endDate1, String sentiment,
            Long symbolId2, LocalDateTime startDate2, LocalDateTime endDate2, Pageable pageable
    );

    Optional<News> findByExternalId(Long externalId);

    List<News> findAllBySymbols_IdInAndDateBetween(Collection<Long> symbolIds, LocalDate from, LocalDate to);

    void deleteBySymbolsIsEmpty();
}
