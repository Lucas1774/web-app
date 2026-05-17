package com.lucas.server.components.tradingbot.portfolio.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioMockRepository extends JpaRepository<PortfolioMock, Long> {

    Optional<PortfolioMock> findTopBySymbol_IdOrderByEffectiveTimestampDesc(Long symbolId);
}
