package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationsJpaService implements JpaService<Recommendation> {

    @Delegate
    private final GenericJpaServiceDelegate<Recommendation, RecommendationsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Recommendation> uniqueConstraintDelegate;
    private final RecommendationsRepository repository;

    public RecommendationsJpaService(RecommendationsRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Set<Recommendation> createIgnoringDuplicates(Set<Recommendation> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(this::findUnique, entities);
    }

    private Set<Recommendation> findUnique(Set<Recommendation> recommendations) {
        return repository.findBySymbol_IdInAndDateIn(
                recommendations.stream().map(r -> r.getSymbol().getId()).collect(Collectors.toSet()),
                recommendations.stream().map(Recommendation::getDate).collect(Collectors.toSet())
        );
    }

    public Set<Recommendation> findByDateBetween(LocalDate from, LocalDate to) {
        return repository.findByDateBetween(from, to);
    }

    // TODO: batch
    public Set<Recommendation> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()));
    }

    public Set<Recommendation> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }

    @Transactional
    public Set<Recommendation> createOrUpdate(Set<Recommendation> entities) {
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity
                        .setModel(newEntity.getModel())
                        .setAction(newEntity.getAction())
                        .setConfidence(newEntity.getConfidence())
                        .setRationale(newEntity.getRationale())
                        .setMarketData(newEntity.getMarketData())
                        .setInput(newEntity.getInput())
                        .setErrors(newEntity.getErrors())
                        .setNews(newEntity.getNews()),
                entities);
    }

    public OrderedIndexedSet<Long> getTopRecommendedSymbols(String action, BigDecimal confidenceThreshold, LocalDate recommendationDate) {
        return repository.findByActionAndConfidenceGreaterThanEqualAndDate(action, confidenceThreshold, recommendationDate)
                .stream()
                .sorted(Comparator.comparing(Recommendation::getConfidence).reversed())
                .map(r -> r.getSymbol().getId())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    public Set<Recommendation> getDailyRecommendations(BigDecimal confidenceThreshold, LocalDate date, String action, Set<String> models) {
        return repository.findByConfidenceGreaterThanEqualAndDateAndActionAndModelIn(confidenceThreshold, date, action, models);
    }

    public Set<Recommendation> findByNewsId(Set<Long> newsIds) {
        return repository.findByNews_IdIn(newsIds);
    }
}
