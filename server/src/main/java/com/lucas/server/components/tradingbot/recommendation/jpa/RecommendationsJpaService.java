package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationMapper;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationsJpaService implements JpaService<RecommendationDomain> {

    private final GenericJpaServiceDelegate<Recommendation, RecommendationDomain, RecommendationsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Recommendation> uniqueConstraintDelegate;
    private final RecommendationsRepository repository;
    private final RecommendationMapper recommendationMapper;

    public RecommendationsJpaService(RecommendationsRepository repository, RecommendationMapper recommendationMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, recommendationMapper);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.recommendationMapper = recommendationMapper;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Set<RecommendationDomain> createIgnoringDuplicates(Set<RecommendationDomain> entities) {
        Set<Recommendation> recommendationEntities = entities.stream()
                .map(recommendationMapper::toEntity)
                .collect(Collectors.toSet());
        return uniqueConstraintDelegate.createIgnoringDuplicates(this::findUnique, recommendationEntities).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    private Set<Recommendation> findUnique(Set<Recommendation> recommendations) {
        return repository.findBySymbol_IdInAndDateIn(
                recommendations.stream().map(r -> r.getSymbol().getId()).collect(Collectors.toSet()),
                recommendations.stream().map(Recommendation::getDate).collect(Collectors.toSet())
        );
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findByDateBetween(LocalDate from, LocalDate to) {
        return repository.findByDateBetween(from, to).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public Set<RecommendationDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<RecommendationDomain> createOrUpdate(Set<RecommendationDomain> entities) {
        Set<Recommendation> recommendationEntities = entities.stream()
                .map(recommendationMapper::toEntity)
                .collect(Collectors.toSet());
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                        (oldEntity, newEntity) -> oldEntity
                                .setModel(newEntity.getModel())
                                .setAction(newEntity.getAction())
                                .setConfidence(newEntity.getConfidence())
                                .setRationale(newEntity.getRationale())
                                .setMarketData(newEntity.getMarketData())
                                .setInput(newEntity.getInput())
                                .setErrors(newEntity.getErrors())
                                .addNews(newEntity.getNews()),
                        recommendationEntities).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public OrderedIndexedSet<Long> getTopRecommendedSymbols(String action, BigDecimal confidenceThreshold, LocalDate recommendationDate) {
        return repository.findByActionAndConfidenceGreaterThanEqualAndDate(action, confidenceThreshold, recommendationDate)
                .stream()
                .sorted(Comparator.comparing(Recommendation::getConfidence).reversed())
                .map(r -> r.getSymbol().getId())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> getDailyRecommendations(BigDecimal confidenceThreshold, LocalDate date, String action, Set<String> models) {
        return repository.findByConfidenceGreaterThanEqualAndDateAndActionAndModelIn(confidenceThreshold, date, action, models).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findByNewsId(Set<Long> newsIds) {
        return repository.findByNews_IdIn(newsIds).stream()
                .map(recommendationMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<RecommendationDomain> saveAll(Set<RecommendationDomain> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<RecommendationDomain> elements) {
        delegate.deleteAll(elements);
    }
}
