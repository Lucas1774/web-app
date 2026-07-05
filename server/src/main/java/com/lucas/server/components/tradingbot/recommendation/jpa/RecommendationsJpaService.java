package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
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
public class RecommendationsJpaService
        extends GenericJpaServiceDelegate<Recommendation, RecommendationDomain, RecommendationsRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<Recommendation> delegate;

    public RecommendationsJpaService(RecommendationsRepository repository,
                                     EntityMapper<Recommendation, RecommendationDomain> mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Set<RecommendationDomain> createIgnoringDuplicates(Set<RecommendationDomain> dtos) {
        Set<Recommendation> recommendationEntities = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return delegate.createIgnoringDuplicates(this::findUnique, recommendationEntities)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findByDateBetween(LocalDate from, LocalDate to) {
        return repository.findByDateBetween(from, to)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public Set<RecommendationDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()))
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public Set<RecommendationDomain> createOrUpdate(Set<RecommendationDomain> entities) {
        Set<Recommendation> entitySet = entities.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return delegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity.setModel(newEntity.getModel())
                        .setAction(newEntity.getAction())
                        .setConfidence(newEntity.getConfidence())
                        .setRationale(newEntity.getRationale())
                        .setMarketData(newEntity.getMarketData())
                        .setInput(newEntity.getInput())
                        .setErrors(newEntity.getErrors())
                        .addNews(newEntity.getNews()),
                entitySet).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public OrderedIndexedSet<Long> getTopRecommendedSymbols(String action,
                                                            BigDecimal confidenceThreshold,
                                                            LocalDate recommendationDate) {
        return repository.findByActionAndConfidenceGreaterThanEqualAndDate(action,
                        confidenceThreshold,
                        recommendationDate)
                .stream()
                .sorted(Comparator.comparing(Recommendation::getConfidence).reversed())
                .map(r -> r.getSymbol().getId())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> getDailyRecommendations(BigDecimal confidenceThreshold,
                                                             LocalDate date,
                                                             String action,
                                                             Set<String> models) {
        return repository.findByConfidenceGreaterThanEqualAndDateAndActionAndModelIn(confidenceThreshold,
                date,
                action,
                models).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<RecommendationDomain> findByNewsId(Set<Long> newsIds) {
        return repository.findByNews_IdIn(newsIds).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Recommendation> findUnique(Set<Recommendation> recommendations) {
        return repository.findBySymbol_IdInAndDateIn(recommendations.stream()
                        .map(r -> r.getSymbol().getId())
                        .collect(Collectors.toUnmodifiableSet()),
                recommendations.stream().map(Recommendation::getDate).collect(Collectors.toSet()));
    }
}
