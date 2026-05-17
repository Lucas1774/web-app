package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.portfolio.mapper.PortfolioMockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
public class PortfolioMockJpaService implements IPortfolioJpaService {

    private final GenericJpaServiceDelegate<PortfolioMock, PortfolioDomain, PortfolioMockRepository> delegate;
    private final PortfolioJpaServiceDelegate<PortfolioMock, PortfolioMockRepository> portfolioDelegate;

    public PortfolioMockJpaService(PortfolioMockRepository repository, PortfolioMockMapper portfolioMockMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, portfolioMockMapper);
        portfolioDelegate = new PortfolioJpaServiceDelegate<>(repository,
                repository::findTopBySymbol_IdOrderByEffectiveTimestampDesc,
                PortfolioMock::new,
                portfolioMockMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioDomain> findBySymbol(SymbolDomain symbol) {
        return portfolioDelegate.findBySymbol(symbol);
    }

    @Override
    @Transactional
    public PortfolioDomain executePortfolioAction(SymbolDomain symbol, BigDecimal price, BigDecimal quantity, BigDecimal commission,
                                                  LocalDateTime timestamp, boolean isBuy) throws IllegalStateException {
        return portfolioDelegate.executePortfolioAction(symbol, price, quantity, commission, timestamp, isBuy);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PortfolioDomain> findActivePortfolio() {
        return portfolioDelegate.findActivePortfolio();
    }

    @Override
    @Transactional
    public Set<PortfolioDomain> saveAll(Set<PortfolioDomain> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PortfolioDomain> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<PortfolioDomain> elements) {
        delegate.deleteAll(elements);
    }
}
