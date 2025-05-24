package com.lucas.server.components.tradingbot.portfolio.service;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class PortfolioManager {

    public record SymbolStand(
            Symbol symbol,
            LocalDateTime lastMoveDate,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal positionValue,
            BigDecimal pnL,
            BigDecimal percentPnl
    ) {
    }

    /**
     * @param portfolio portfolio
     * @param last      last market data for the portfolio's symbol
     * @return current stand of the portfolio
     */
    public SymbolStand computeStand(PortfolioBase portfolio, MarketData last) {
        BigDecimal quantity = portfolio.getQuantity();
        BigDecimal averageCost = portfolio.getAverageCost();
        BigDecimal positionValue;
        BigDecimal pnL;
        BigDecimal percentPnL;
        if (null != quantity && null != averageCost) {
            positionValue = quantity.multiply(averageCost).setScale(4, RoundingMode.HALF_UP);
            pnL = last.getPrice().subtract(averageCost).multiply(quantity).setScale(4, RoundingMode.HALF_UP);
            percentPnL = (last.getPrice().subtract(averageCost)).divide(averageCost, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        } else {
            positionValue = null;
            pnL = null;
            percentPnL = null;
        }

        return new SymbolStand(portfolio.getSymbol(), portfolio.getEffectiveTimestamp(), quantity, averageCost, positionValue, pnL, percentPnL);
    }
}
