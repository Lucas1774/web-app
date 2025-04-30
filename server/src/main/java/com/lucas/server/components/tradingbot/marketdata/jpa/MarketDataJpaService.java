package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
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
    public Optional<MarketData> save(MarketData entity) {
        return this.delegate.save(repository, entity);
    }

    @Override
    public List<MarketData> saveAll(Iterable<MarketData> entities) {
        return this.delegate.saveAllIgnoringDuplicates(this.repository, entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<MarketData> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<MarketData> findById(String id) {
        return this.repository.findById(Long.valueOf(id));
    }

    public Optional<MarketData> findTopBySymbolAndDateBeforeOrderByDateDesc(String symbol, LocalDate date) {
        return this.repository.findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, date);
    }
}
