package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * JPA service for portfolio management operations.
 */
public interface IPortfolioJpaService {

    /**
     * @param symbol the symbol to search by
     * @return the portfolio entry for the given symbol, if present
     */
    Optional<PortfolioDomain> findBySymbol(SymbolDomain symbol);

    /**
     * Executes a buy or sell action on the portfolio for the given symbol.
     *
     * @param symbol     the traded symbol
     * @param price      execution price per unit
     * @param quantity   number of units traded
     * @param commission transaction commission
     * @param timestamp  time of execution
     * @param isBuy      {@code true} for a buy action, {@code false} for a sell
     * @return the updated portfolio entry
     * @throws IllegalStateException if the action is invalid given the current portfolio state
     */
    PortfolioDomain executePortfolioAction(SymbolDomain symbol, BigDecimal price, BigDecimal quantity,
                                           BigDecimal commission, LocalDateTime timestamp,
                                           boolean isBuy) throws IllegalStateException;

    /**
     * @return all portfolio entries with an active position
     */
    Set<PortfolioDomain> findActivePortfolio();
}
