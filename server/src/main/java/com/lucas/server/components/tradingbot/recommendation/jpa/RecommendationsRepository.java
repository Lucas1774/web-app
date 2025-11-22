package com.lucas.server.components.tradingbot.recommendation.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Repository
public interface RecommendationsRepository extends JpaRepository<Recommendation, Long> {

    Set<Recommendation> findByDateBetween(LocalDate from, LocalDate to);

    Set<Recommendation> findBySymbol_IdInAndDateIn(Set<Long> ids, Set<LocalDate> dates);

    Set<Recommendation> findBySymbol_Id(Long symbolId, PageRequest date);

    Set<Recommendation> findBySymbol_Id(Long id);

    Set<Recommendation> findByNews_IdIn(Set<Long> newsIds);

    Set<Recommendation> findByActionAndConfidenceGreaterThanEqualAndDate(String action,
                                                                         BigDecimal confidenceThreshold,
                                                                         LocalDate recommendationDate);

    Set<Recommendation> findByConfidenceGreaterThanEqualAndDateAndActionAndModelIn(BigDecimal confidenceThreshold,
                                                                                   LocalDate date,
                                                                                   String action,
                                                                                   Set<String> models);
}
