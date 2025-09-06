package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    List<MarketData> findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(Long id, LocalDate date);

    Slice<MarketData> findBySymbol_Id(Long symbolId, PageRequest page);

    List<MarketData> findBySymbol_IdInAndDateIn(Collection<Long> symbolIds, Collection<LocalDate> dates);

    List<MarketData> findBySymbol_Id(Long id);

    Optional<MarketData> findTopBySymbol_IdOrderByDateDesc(Long id);
}
