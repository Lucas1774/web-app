package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.marketdata.mapper.MarketSnapshotMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketSnapshotJpaService
        extends GenericJpaServiceDelegate<MarketSnapshot, MarketSnapshotDomain, MarketSnapshotRepository> {

    private final MarketSnapshotMapper marketSnapshotMapper;

    public MarketSnapshotJpaService(MarketSnapshotRepository repository,
                                    EntityMapper<MarketSnapshot, MarketSnapshotDomain> mapper,
                                    MarketSnapshotMapper marketSnapshotMapper) {
        super(repository, mapper);
        this.marketSnapshotMapper = marketSnapshotMapper;
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public Set<MarketSnapshotDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()))
                .stream()
                .map(marketSnapshotMapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public Set<MarketSnapshotDomain> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id)
                .stream()
                .map(marketSnapshotMapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }
}
