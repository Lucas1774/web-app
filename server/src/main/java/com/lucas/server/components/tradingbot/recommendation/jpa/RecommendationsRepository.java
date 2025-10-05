package com.lucas.server.components.tradingbot.recommendation.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RecommendationsRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByDateBetween(LocalDate from, LocalDate to);

    List<Recommendation> findBySymbol_IdInAndDateIn(Collection<Long> ids, Collection<LocalDate> dates);

    Slice<Recommendation> findBySymbol_Id(Long symbolId, PageRequest date);

    List<Recommendation> findBySymbol_Id(Long id);

    List<Recommendation> findByActionAndConfidenceGreaterThanEqualAndDate(String action,
                                                                          BigDecimal confidenceThreshold,
                                                                          LocalDate recommendationDate);

    List<Recommendation> findByConfidenceGreaterThanEqualAndDateAndActionAndModelIn(BigDecimal confidenceThreshold,
                                                                                    LocalDate date,
                                                                                    String action,
                                                                                    List<String> models);
}
