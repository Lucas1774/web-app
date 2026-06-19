package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketDataJpaService
        extends GenericJpaServiceDelegate<MarketData, MarketDataDomain, MarketDataRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<MarketData> delegate;
    private final MarketDataKpiGenerator kpiGenerator;

    public MarketDataJpaService(MarketDataRepository repository,
                                EntityMapper<MarketData, MarketDataDomain> mapper,
                                MarketDataKpiGenerator kpiGenerator) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.kpiGenerator = kpiGenerator;
    }

    /**
     * Saves all entities first, then updates them one by one (oldest first) with KPI computation.
     */
    @Transactional
    public Set<MarketDataDomain> createIgnoringDuplicates(OrderedIndexedSet<MarketDataDomain> dtos) {
        Set<MarketData> entitySet = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        Set<MarketData> saved = delegate.createIgnoringDuplicates(this::findUnique, entitySet);
        for (MarketDataDomain md : saved.stream()
                .sorted(Comparator.comparing(MarketData::getDate))
                .map(mapper::toDto)
                .toList()) {
            kpiGenerator.computeDerivedFields(md);
            repository.saveAndFlush(mapper.toEntity(md));
        }
        return saved.stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Saves or updates all entities first, then updates them one by one (oldest first) with KPI computation.
     */
    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Set<MarketDataDomain> createOrUpdate(OrderedIndexedSet<MarketDataDomain> dtos) {
        Set<MarketData> entitySet = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        Set<MarketData> saved = delegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity.setOpen(newEntity.getOpen())
                        .setHigh(newEntity.getHigh())
                        .setLow(newEntity.getLow())
                        .setPrice(newEntity.getPrice())
                        .setVolume(newEntity.getVolume())
                        .setPreviousClose(newEntity.getPreviousClose())
                        .setChange(newEntity.getChange())
                        .setChangePercent(newEntity.getChangePercent()),
                entitySet);
        for (MarketDataDomain md : saved.stream()
                .sorted(Comparator.comparing(MarketData::getDate))
                .map(mapper::toDto)
                .toList()) {
            kpiGenerator.computeDerivedFields(md);
            repository.saveAndFlush(mapper.toEntity(md));
        }
        return saved.stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public OrderedIndexedSet<MarketDataDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()))
                .stream()
                .map(mapper::toDto)
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public Set<MarketDataDomain> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Optional<MarketDataDomain> findLatestBySymbolId(Long id) {
        return repository.findTopBySymbol_IdOrderByDateDesc(id).map(mapper::toDto);
    }

    private Set<MarketData> findUnique(Set<MarketData> marketDataEntities) {
        return repository.findBySymbol_IdInAndDateIn(marketDataEntities.stream()
                        .map(md -> md.getSymbol().getId())
                        .collect(Collectors.toUnmodifiableSet()),
                marketDataEntities.stream().map(MarketData::getDate).collect(Collectors.toSet()));
    }
}
