package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    List<MarketData> findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(Long id, LocalDate date);

    Slice<MarketData> findBySymbol_Id(Long symbolId, PageRequest page);

    Optional<MarketData> findBySymbol_IdAndDate(Long symbolId, LocalDate date);

    List<MarketData> findBySymbol_Id(Long id);
}
