package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecommendationsJpaService implements JpaService<Recommendation> {

    @Delegate
    private final GenericJpaServiceDelegate<Recommendation, RecommendationsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Recommendation> uniqueConstraintDelegate;
    private final RecommendationsRepository repository;

    public RecommendationsJpaService(RecommendationsRepository repository) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public List<Recommendation> createIgnoringDuplicates(Iterable<Recommendation> entities) {
        return this.uniqueConstraintDelegate.createIgnoringDuplicates(
                entity -> this.repository.findBySymbol_IdAndDate(entity.getSymbol().getId(), entity.getDate()), entities);
    }

    public List<Recommendation> findByDate(LocalDate now) {
        return this.repository.findByDate(now);
    }

    public void deleteAll(List<Recommendation> res) {
        this.repository.deleteAllInBatch(res);
    }

    public List<Recommendation> getTopForSymbolId(Long symbolId, int limit) {
        return this.repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    public List<Recommendation> findBySymbolId(Long id) {
        return this.repository.findBySymbol_Id(id);
    }
}
