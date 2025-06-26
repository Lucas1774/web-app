package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public List<Recommendation> createIgnoringDuplicates(Iterable<Recommendation> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(
                entity -> repository.findBySymbol_IdAndDate(entity.getSymbol().getId(), entity.getDate()), entities);
    }

    public List<Recommendation> findByDateBetween(LocalDate from, LocalDate to) {
        return repository.findByDateBetween(from, to);
    }

    public void deleteAll(List<Recommendation> res) {
        repository.deleteAllInBatch(res);
    }

    public List<Recommendation> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    public List<Recommendation> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }

    @Transactional
    public List<Recommendation> createOrUpdate(List<Recommendation> entities) {
        return uniqueConstraintDelegate.createOrUpdate(entity -> repository.findBySymbol_IdAndDate(entity.getSymbol().getId(), entity.getDate()),
                (oldEntity, newEntity) -> oldEntity
                        .setModel(newEntity.getModel())
                        .setAction(newEntity.getAction())
                        .setConfidence(newEntity.getConfidence())
                        .setRationale(newEntity.getRationale())
                        .setMarketData(newEntity.getMarketData())
                        .setInput(newEntity.getInput())
                        .setErrors(newEntity.getErrors()),
                entities);
    }

    public List<Long> getTopRecommendedSymbols(String action, BigDecimal confidenceThreshold, LocalDate recommendationDate) {
        return repository.findByActionAndConfidenceGreaterThanEqualAndDate(action, confidenceThreshold, recommendationDate)
                .stream()
                .map(r -> r.getSymbol().getId())
                .distinct()
                .toList();
    }
}
