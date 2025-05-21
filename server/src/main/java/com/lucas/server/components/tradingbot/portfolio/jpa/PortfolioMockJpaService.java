package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class PortfolioMockJpaService implements IPortfolioJpaService<PortfolioMock> {

    @Delegate
    private final GenericJpaServiceDelegate<PortfolioMock, PortfolioMockRepository> delegate;
    @Delegate
    private final PortfolioJpaServiceDelegate<PortfolioMock, PortfolioMockRepository> portfolioDelegate;

    public PortfolioMockJpaService(PortfolioMockRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        portfolioDelegate = new PortfolioJpaServiceDelegate<>(repository, repository::findTopBySymbolOrderByEffectiveTimestampDesc, PortfolioMock::new);
    }
}
