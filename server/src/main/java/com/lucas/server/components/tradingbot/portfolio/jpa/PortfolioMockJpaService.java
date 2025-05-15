package com.lucas.server.components.tradingbot.portfolio.jpa;

import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class PortfolioMockJpaService implements IPortfolioJpaService<PortfolioMock> {

    @Delegate
    private final PortfolioJpaServiceDelegate<PortfolioMock, PortfolioMockRepository> delegate;

    public PortfolioMockJpaService(PortfolioMockRepository repository) {
        this.delegate = new PortfolioJpaServiceDelegate<>(repository, repository::findTopBySymbolOrderByEffectiveTimestampDesc, PortfolioMock::new);
    }
}
