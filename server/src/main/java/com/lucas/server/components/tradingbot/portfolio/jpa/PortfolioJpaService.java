package com.lucas.server.components.tradingbot.portfolio.jpa;

import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class PortfolioJpaService implements IPortfolioJpaService<Portfolio> {

    @Delegate
    private final PortfolioJpaServiceDelegate<Portfolio, PortfolioRepository> delegate;

    public PortfolioJpaService(PortfolioRepository repository) {
        this.delegate = new PortfolioJpaServiceDelegate<>(repository, repository::findTopBySymbolOrderByEffectiveTimestampDesc, Portfolio::new);
    }
}
