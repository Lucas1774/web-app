package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Set;

@Repository
public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {

    OrderedIndexedSetImpl<MarketSnapshot> findBySymbol_Id(Long symbolId, PageRequest page);

    Set<MarketSnapshot> findBySymbol_Id(Long id);

    Set<MarketSnapshot> findAllBySymbol_IdInAndDateBetween(Set<Long> symbolIds, LocalDateTime from, LocalDateTime to);
}
