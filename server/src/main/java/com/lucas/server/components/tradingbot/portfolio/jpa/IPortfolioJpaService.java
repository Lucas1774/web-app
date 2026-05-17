package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface IPortfolioJpaService extends JpaService<PortfolioDomain> {

    Optional<PortfolioDomain> findBySymbol(SymbolDomain symbol);

    @SuppressWarnings("RedundantThrows")
    PortfolioDomain executePortfolioAction(SymbolDomain symbol, BigDecimal price, BigDecimal quantity, BigDecimal commission,
                                           LocalDateTime timestamp, boolean isBuy) throws IllegalStateException;

    Set<PortfolioDomain> findActivePortfolio();
}
