package com.lucas.server.components.tradingbot.portfolio.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findTopBySymbol_IdOrderByEffectiveTimestampDesc(Long symbolId);
}
