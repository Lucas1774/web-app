package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import org.springframework.stereotype.Service;

@Service
public class PortfolioJpaService extends PortfolioJpaServiceDelegate<Portfolio, PortfolioRepository> {

    public PortfolioJpaService(PortfolioRepository repository, EntityMapper<Portfolio, PortfolioDomain> mapper) {
        super(repository, mapper, repository::findTopBySymbol_IdOrderByEffectiveTimestampDesc, Portfolio::new);
    }
}
