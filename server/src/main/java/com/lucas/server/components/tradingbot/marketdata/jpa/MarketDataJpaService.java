package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataJpaService implements JpaService<MarketData> {

    private final MarketDataRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<MarketData> delegate;

    public MarketDataJpaService(MarketDataRepository repository, UniqueConstraintWearyJpaServiceDelegate<MarketData> delegate) {
        this.repository = repository;
        this.delegate = delegate;
    }

    @Override
    public List<MarketData> createAll(List<MarketData> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<MarketData> findAll() {
        return this.repository.findAll();
    }

    public MarketData getOrCreate(MarketData entity) {
        return this.delegate.getOrCreate(repository, this::findUnique, entity);
    }

    public List<MarketData> createIgnoringDuplicates(Iterable<MarketData> entities) {
        return this.delegate.createIgnoringDuplicates(this.repository, this::findUnique, entities);
    }

    public Optional<MarketData> findUnique(MarketData entity) {
        return this.repository.findBySymbol_IdAndDate(entity.getSymbol().getId(), entity.getDate());
    }

    public Optional<MarketData> findTopBySymbolIdAndDateBeforeOrderByDateDesc(Long symbolId, LocalDate date) {
        return this.repository.findTopBySymbol_IdAndDateBeforeOrderByDateDesc(symbolId, date);
    }

    public List<MarketData> getTopForSymbolId(Long symbolId, int limit) {
        return this.repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }
}
