package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

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

    public List<MarketData> createIgnoringDuplicates(Collection<MarketData> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(this::findUnique, new LinkedHashSet<>(entities));
    }

    private Collection<MarketData> findUnique(Collection<MarketData> marketData) {
        return repository.findBySymbol_IdInAndDateIn(
                marketData.stream().map(md -> md.getSymbol().getId()).toList(),
                marketData.stream().map(MarketData::getDate).toList()
        );
    }

    public List<MarketData> findTop14BySymbolIdAndDateBeforeOrderByDateDesc(Long id, LocalDate date) {
        return repository.findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(id, date);
    }

    // TODO: batch
    public List<MarketData> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    // TODO: batch
    public List<MarketData> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }

    public void deleteAll(List<MarketData> res) {
        repository.deleteAllInBatch(res);
    }
}
