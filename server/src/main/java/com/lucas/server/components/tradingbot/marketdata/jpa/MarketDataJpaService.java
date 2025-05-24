package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataJpaService implements JpaService<MarketData> {

    @Delegate
    private final GenericJpaServiceDelegate<MarketData, MarketDataRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<MarketData> uniqueConstraintDelegate;
    private final MarketDataRepository repository;

    public MarketDataJpaService(MarketDataRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public MarketData getOrCreate(MarketData entity) {
        return uniqueConstraintDelegate.getOrCreate(this::findUnique, entity);
    }

    public List<MarketData> createIgnoringDuplicates(Iterable<MarketData> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(this::findUnique, entities);
    }

    public Optional<MarketData> findUnique(MarketData entity) {
        return repository.findBySymbol_IdAndDate(entity.getSymbol().getId(), entity.getDate());
    }

    public List<MarketData> findTop14BySymbolIdAndDateBeforeOrderByDateDesc(Long id, LocalDate date) {
        return repository.findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(id, date);
    }

    public List<MarketData> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    public List<MarketData> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }

    public void deleteAll(List<MarketData> res) {
        repository.deleteAllInBatch(res);
    }
}
