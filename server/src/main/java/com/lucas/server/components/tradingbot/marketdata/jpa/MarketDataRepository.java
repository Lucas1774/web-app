package com.lucas.server.components.tradingbot.marketdata.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
}
