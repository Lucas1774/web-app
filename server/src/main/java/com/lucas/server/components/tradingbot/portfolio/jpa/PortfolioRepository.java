package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findTopBySymbolOrderByEffectiveTimestampDesc(Symbol symbol);
}
