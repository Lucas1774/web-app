package com.lucas.server.components.tradingbot.portfolio.service;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class PortfolioManager {

    public record SymbolStand(
            Symbol symbol,
            BigDecimal price,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal percentDayChange,
            Long volume,
            LocalDateTime lastMoveDate,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal positionValue,
            BigDecimal pnL,
            BigDecimal percentPnl,
            BigDecimal netRelativePosition,
            Recommendation recommendation
    ) {
    }

    /**
     * @param portfolio portfolio
     * @param last      last market data for the portfolio's symbol
     * @return current stand of the portfolio
     */
    public SymbolStand computeStand(PortfolioBase portfolio, MarketData last) {
        BigDecimal quantity = portfolio.getQuantity();
        BigDecimal averageCost = quantity == null || quantity.signum() == 0 ? null : portfolio.getAverageCost();
        BigDecimal averageCommission = portfolio.getAverageCommission();
        BigDecimal positionValue;
        BigDecimal pnL;
        BigDecimal percentPnL;
        BigDecimal netRelativePosition;
        if (null == quantity || null == averageCost) {
            positionValue = null;
            pnL = null;
            percentPnL = null;
            netRelativePosition = null;
        } else {
            positionValue = quantity.multiply(averageCost).setScale(4, RoundingMode.HALF_UP);
            pnL = last.getPrice().subtract(averageCost).multiply(quantity).setScale(4, RoundingMode.HALF_UP);
            percentPnL = (last.getPrice().subtract(averageCost)).divide(averageCost, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
            if (null == averageCommission) {
                netRelativePosition = null;
            } else {
                BigDecimal averageCostNoCommission = averageCost
                        .divide(BigDecimal.ONE.add(averageCommission), 8, RoundingMode.HALF_UP);
                netRelativePosition = last.getPrice().subtract(averageCostNoCommission).divide(averageCostNoCommission, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
            }
        }
        BigDecimal percentDayChange = last.getPrice().subtract(last.getPreviousClose()).divide(last.getPreviousClose(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);

        return new SymbolStand(portfolio.getSymbol(), last.getPrice(), last.getOpen(), last.getHigh(), last.getLow(), percentDayChange, last.getVolume(),
                portfolio.getEffectiveTimestamp(), quantity, averageCost, positionValue, pnL, percentPnL, netRelativePosition,
                last.getRecommendations().stream()
                        .max(Comparator.comparing(Recommendation::getDate))
                        .orElse(null));
    }
}
