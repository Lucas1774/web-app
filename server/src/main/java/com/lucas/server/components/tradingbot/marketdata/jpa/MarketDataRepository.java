package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    Optional<MarketData> findTopBySymbol_IdAndDateBeforeOrderByDateDesc(Long symbolId, LocalDate date);

    Slice<MarketData> findBySymbol_Id(Long symbolId, PageRequest page);

    Optional<MarketData> findBySymbol_IdAndDate(Long symbolId, LocalDate date);
}
