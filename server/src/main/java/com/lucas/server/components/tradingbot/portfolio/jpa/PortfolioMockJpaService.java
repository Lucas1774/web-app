package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import org.springframework.stereotype.Service;

@Service
public class PortfolioMockJpaService extends PortfolioJpaServiceDelegate<PortfolioMock, PortfolioMockRepository> {

    public PortfolioMockJpaService(PortfolioMockRepository repository, EntityMapper<PortfolioMock, PortfolioDomain> mapper) {
        super(repository, mapper, repository::findTopBySymbol_IdOrderByEffectiveTimestampDesc, PortfolioMock::new);
    }
}
