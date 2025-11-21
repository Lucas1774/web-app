package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {

    Slice<MarketSnapshot> findBySymbol_Id(Long symbolId, PageRequest page);

    List<MarketSnapshot> findAllBySymbol_IdInAndDateBetween(Collection<Long> symbolIds, LocalDateTime from, LocalDateTime to);

    List<MarketSnapshot> findBySymbol_Id(Long id);
}
