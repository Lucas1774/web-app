package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    OrderedIndexedSetImpl<MarketData> findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(Long id, LocalDate date);

    OrderedIndexedSetImpl<MarketData> findBySymbol_Id(Long symbolId, PageRequest page);

    Set<MarketData> findBySymbol_IdInAndDateIn(Set<Long> symbolIds, Set<LocalDate> dates);

    Set<MarketData> findBySymbol_Id(Long id);

    Optional<MarketData> findTopBySymbol_IdOrderByDateDesc(Long id);
}
