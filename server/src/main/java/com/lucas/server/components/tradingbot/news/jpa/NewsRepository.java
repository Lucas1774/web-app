package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Slice<News> findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(Long symbolId1, String neutralSentiment,
                                                                              Long symbolId2, PageRequest pageable);

    Optional<News> findByExternalId(Long externalId);

    List<News> findAllByIdIn(List<Long> ids);

    List<News> findAllBySymbols_IdInAndDateBetween(Collection<Long> symbolIds, LocalDate from, LocalDate to);

    void deleteBySymbolsIsEmpty();
}
