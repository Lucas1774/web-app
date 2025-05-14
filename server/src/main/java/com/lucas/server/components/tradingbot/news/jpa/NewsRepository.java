package com.lucas.server.components.tradingbot.news.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Slice<News> findBySymbols_IdAndSentimentNot(Long symbolId, String sentiment, PageRequest pageRequest);

    Optional<News> findByExternalId(Long externalId);

    List<News> findAllByIdIn(List<Long> ids);

    List<News> findAllBySymbols_IdInAndDateBetween(Collection<Long> symbolIds, LocalDateTime from, LocalDateTime to);
}
