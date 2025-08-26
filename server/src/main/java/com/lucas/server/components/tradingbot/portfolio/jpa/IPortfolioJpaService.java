package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IPortfolioJpaService<T extends PortfolioBase> extends JpaService<T> {

    Optional<T> findBySymbol(Symbol symbol);

    @SuppressWarnings("RedundantThrows")
    T executePortfolioAction(Symbol symbol, BigDecimal price, BigDecimal quantity, BigDecimal commission,
                             LocalDateTime timestamp, boolean isBuy) throws IllegalStateException;

    List<T> findActivePortfolio();
}
