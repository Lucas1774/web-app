package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataJpaService implements JpaService<MarketData> {

    private final MarketDataRepository repository;

    public MarketDataJpaService(MarketDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarketData save(MarketData entity) {
        return this.repository.save(entity);
    }

    @Override
    public List<MarketData> saveAll(Iterable<MarketData> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    public Optional<MarketData> findTopBySymbolAndDateBeforeOrderByDateDesc(String symbol, LocalDate date) {
        return this.repository.findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, date);
    }

}
