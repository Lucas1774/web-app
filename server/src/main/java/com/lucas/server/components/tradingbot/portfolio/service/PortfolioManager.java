package com.lucas.server.components.tradingbot.portfolio.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PortfolioManager {

    public record SymbolStand(
            @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
            Symbol symbol,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal positionValue,
            BigDecimal pnL
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
        if (null != quantity && null != averageCost) {
            positionValue = quantity.multiply(averageCost).setScale(4, RoundingMode.HALF_UP);
            pnL = last.getPrice().subtract(averageCost).multiply(quantity).setScale(4, RoundingMode.HALF_UP);
        } else {
            positionValue = null;
            pnL = null;
        }

        return new SymbolStand(portfolio.getSymbol(), quantity, averageCost, positionValue, pnL);
    }
}
