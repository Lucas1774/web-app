package com.lucas.server.components.tradingbot.recommendation.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationsRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByDateBetween(LocalDate from, LocalDate to);

    Optional<Recommendation> findBySymbol_IdAndDate(Long id, LocalDate date);

    Slice<Recommendation> findBySymbol_Id(Long symbolId, PageRequest date);

    List<Recommendation> findBySymbol_Id(Long id);

    List<Recommendation> findByActionAndConfidenceGreaterThanEqualAndDate(String action,
                                                                BigDecimal confidenceThreshold,
                                                                LocalDate recommendationDate);
}
