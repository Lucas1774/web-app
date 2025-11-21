package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketSnapshotJpaService implements JpaService<MarketSnapshot> {

    private final MarketSnapshotRepository repository;

    @Delegate
    private final GenericJpaServiceDelegate<MarketSnapshot, MarketSnapshotRepository> delegate;

    public MarketSnapshotJpaService(MarketSnapshotRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    // TODO: batch
    public List<MarketSnapshot> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    // TODO: batch
    public List<MarketSnapshot> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }
}
