package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class PortfolioJpaService implements IPortfolioJpaService<Portfolio> {

    @Delegate
    private final GenericJpaServiceDelegate<Portfolio, PortfolioRepository> delegate;
    @Delegate
    private final PortfolioJpaServiceDelegate<Portfolio, PortfolioRepository> portfolioDelegate;

    public PortfolioJpaService(PortfolioRepository repository) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.portfolioDelegate = new PortfolioJpaServiceDelegate<>(repository, repository::findTopBySymbolOrderByEffectiveTimestampDesc, Portfolio::new);
    }
}
