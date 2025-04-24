package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaService;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Override
    public List<MarketData> findAll() {
        return repository.findAll();
    }

    public Optional<MarketData> findTopBySymbolAndDateBeforeOrderByDateDesc(String symbol, LocalDate date) {
        return this.repository.findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, date);
    }

    public List<MarketData> saveAllIgnoringDuplicates(List<MarketData> entities) {
        return entities.stream()
                .map(md -> {
                    try {
                        return Optional.of(repository.save(md));
                    } catch (DataIntegrityViolationException e) {
                        return Optional.<MarketData>empty();
                    }
                })
                .flatMap(Optional::stream)
                .toList();
    }
}
