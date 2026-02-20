package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.experimental.Delegate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * @param entities marketData. Need to be sorted so callbacks correctly calculate previous and change
     * @return the newly saved entities
     */
    public Set<MarketData> createIgnoringDuplicates(OrderedIndexedSet<MarketData> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(this::findUnique, entities);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Set<MarketData> createOrUpdate(OrderedIndexedSet<MarketData> entities) {
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity
                        .setOpen(newEntity.getOpen())
                        .setHigh(newEntity.getHigh())
                        .setLow(newEntity.getLow())
                        .setPrice(newEntity.getPrice())
                        .setVolume(newEntity.getVolume())
                        .setPreviousClose(newEntity.getPreviousClose())
                        .setChange(newEntity.getChange())
                        .setChangePercent(newEntity.getChangePercent())
                , entities);
    }

    private Set<MarketData> findUnique(Set<MarketData> marketData) {
        return repository.findBySymbol_IdInAndDateIn(
                marketData.stream().map(md -> md.getSymbol().getId()).collect(Collectors.toSet()),
                marketData.stream().map(MarketData::getDate).collect(Collectors.toSet())
        );
    }

    public OrderedIndexedSet<MarketData> findTop14BySymbolIdAndDateBeforeOrderByDateDesc(Long id, LocalDate date) {
        return repository.findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(id, date);
    }

    // TODO: batch
    public OrderedIndexedSet<MarketData> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()));
    }

    // TODO: batch
    public Set<MarketData> findBySymbolId(Long id) {
        return repository.findBySymbol_Id(id);
    }

    public Optional<MarketData> findLatestBySymbolId(Long id) {
        return repository.findTopBySymbol_IdOrderByDateDesc(id);
    }
}
