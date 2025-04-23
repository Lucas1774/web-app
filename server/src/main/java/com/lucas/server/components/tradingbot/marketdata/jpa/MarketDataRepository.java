package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    Optional<MarketData> findTopBySymbolAndDateBeforeOrderByDateDesc(String symbol, LocalDate date);

}
