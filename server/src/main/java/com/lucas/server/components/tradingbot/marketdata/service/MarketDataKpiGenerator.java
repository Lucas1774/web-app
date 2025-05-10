package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class MarketDataKpiGenerator {

    private final MarketDataJpaService service;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataKpiGenerator.class);

    public MarketDataKpiGenerator(MarketDataJpaService service) {
        this.service = service;
    }

    public MarketData computeDerivedFields(MarketData md) {
        this.service.findTopBySymbolIdAndDateBeforeOrderByDateDesc(md.getSymbol().getId(), md.getDate())
                .ifPresent(previous -> {
                    BigDecimal previousPrice = previous.getPrice();
                    md.setPreviousClose(previousPrice);
                    BigDecimal change = md.getPrice().subtract(previousPrice);
                    md.setChange(change);
                    if (0 != previousPrice.compareTo(BigDecimal.ZERO)) {
                        String percentage = change.divide(previousPrice, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP) + "%";
                        md.setChangePercent(percentage);
                    }
                });

        return md;
    }

    /**
     * @param history list of n consecutive MarketData entries
     * @return simple moving n-average of closing prices
     */
    public BigDecimal computeMovingAverage(List<MarketData> history) throws IllegalStateException {
        if (history.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "SMA Calculation"));
        }

        BigDecimal sumPrices = history.stream()
                .map(MarketData::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumPrices.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return sumPrices.divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @return average true range over n periods (ATR)
     */
    public BigDecimal computeAtr(List<MarketData> history) throws IllegalStateException {
        if (history.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "ATR Calculation"));
        }

        BigDecimal totalTrueRange = history.stream()
                .map(md -> {
                    BigDecimal highLow = md.getHigh().subtract(md.getLow());
                    BigDecimal highPrevClose = md.getHigh().subtract(md.getPreviousClose());
                    BigDecimal lowPrevClose = md.getLow().subtract(md.getPreviousClose());
                    return Stream.of(highLow, highPrevClose, lowPrevClose)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalTrueRange.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return totalTrueRange.divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @return RSI (0–100) using simple average gains/losses over n periods
     */
    public BigDecimal computeRsi(List<MarketData> history) throws IllegalStateException {
        if (history.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "RSI Calculation"));
        }

        BigDecimal totalGains = BigDecimal.ZERO;
        BigDecimal totalLosses = BigDecimal.ZERO;

        for (MarketData md : history) {
            BigDecimal change = md.getPrice().subtract(md.getPreviousClose());
            if (change.signum() > 0) {
                totalGains = totalGains.add(change);
            } else {
                totalLosses = totalLosses.add(change.abs());
            }
        }

        BigDecimal averageGain = totalGains.divide(BigDecimal.valueOf(history.size()), 8, RoundingMode.HALF_UP);
        BigDecimal averageLoss = totalLosses.divide(BigDecimal.valueOf(history.size()), 8, RoundingMode.HALF_UP);

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            if (averageGain.compareTo(BigDecimal.ZERO) == 0) {
                logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
            }
            return BigDecimal.valueOf(100);
        }

        BigDecimal relativeStrength = averageGain.divide(averageLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100)
                .divide(relativeStrength.add(BigDecimal.ONE), 4, RoundingMode.HALF_UP));
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @return annualized volatility in percent (std dev of daily returns)
     */
    public BigDecimal computeVolatility(List<MarketData> history) throws IllegalStateException {
        if (history.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "Volatility Calculation"));
        }

        List<BigDecimal> dailyReturns = new java.util.ArrayList<>();
        for (MarketData md : history) {
            dailyReturns.add(
                    md.getPrice().subtract(md.getPreviousClose())
                            .divide(md.getPreviousClose(), 8, RoundingMode.HALF_UP)
            );
        }

        BigDecimal meanReturn = dailyReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);
        BigDecimal totalSquaredDeviation = dailyReturns.stream()
                .map(r -> r.subtract(meanReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = totalSquaredDeviation
                .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (standardDeviation.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return standardDeviation.multiply(BigDecimal.valueOf(Math.sqrt(252)))
                .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }
}
