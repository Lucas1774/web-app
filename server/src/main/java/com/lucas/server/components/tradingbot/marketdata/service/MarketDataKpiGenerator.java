package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.Constants;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
                    computeIfAbsent(md::getPreviousClose, md::setPreviousClose, previousPrice);

                    BigDecimal change = md.getPrice().subtract(previousPrice);
                    computeIfAbsent(md::getChange, md::setChange, change);

                    if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal percentage = change
                                .divide(previousPrice, 8, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(4, RoundingMode.HALF_UP);
                        computeIfAbsent(md::getChangePercent, md::setChangePercent, percentage);
                    }
                });
        return md;
    }

    private static <T> void computeIfAbsent(Supplier<T> getter, Consumer<T> setter, T value) {
        if (getter.get() == null) {
            setter.accept(value);
        }
    }

    /**
     * @param history list of n consecutive MarketData entries
     * @return simple moving n-average of closing prices
     */
    public BigDecimal computeMovingAverage(List<MarketData> history) {
        BigDecimal sumPrices = history.stream()
                .map(MarketData::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumPrices.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return sumPrices.divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of n consecutive MarketData entries
     * @return exponential moving n‑average of closing prices (EMA)
     */
    public BigDecimal computeEma(List<MarketData> history) {
        int n = history.size();
        BigDecimal k = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(n + 1L), 8, RoundingMode.HALF_UP);

        Iterator<MarketData> iterator = history.iterator();
        BigDecimal ema = iterator.next().getPrice();

        while (iterator.hasNext()) {
            BigDecimal price = iterator.next().getPrice();
            ema = price.subtract(ema).multiply(k).add(ema).setScale(8, RoundingMode.HALF_UP);
        }

        if (ema.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return ema.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of ≥26 consecutive MarketData entries
     * @return MACD line = EMA(12) − EMA(26) of closing prices
     */
    public BigDecimal computeMacdLine(List<MarketData> history) {
        List<MarketData> fastHist = history.subList(history.size() - 12, history.size());
        BigDecimal ema12 = this.computeEma(fastHist);

        // TODO: replace 21 with 26
        List<MarketData> slowHist = history.subList(history.size() - 21, history.size());
        BigDecimal ema26 = this.computeEma(slowHist);

        return ema12.subtract(ema26).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * @param macdHistory list of ≥9 consecutive MACD‐line values (BigDecimal)
     * @return signal line = 9‑day EMA of the MACD line
     */
    public BigDecimal computeSignalLine(List<BigDecimal> macdHistory) {
        BigDecimal k = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(9 + 1L), 8, RoundingMode.HALF_UP);

        Iterator<BigDecimal> iterator = macdHistory.iterator();
        BigDecimal ema = iterator.next();

        while (iterator.hasNext()) {
            BigDecimal val = iterator.next();
            ema = val.subtract(ema).multiply(k).add(ema).setScale(8, RoundingMode.HALF_UP);
        }

        return ema.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @return RSI (0–100) using simple average gains/losses over n periods
     */
    public BigDecimal computeRsi(List<MarketData> history) {
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
     * @return average true range over n periods (ATR)
     */
    public BigDecimal computeAtr(List<MarketData> history) {
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
     * @return annualized volatility in percent (std dev of daily returns)
     */
    public BigDecimal computeVolatility(List<MarketData> history) {
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

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @return On‑Balance Volume (OBV) accumulated over n periods
     */
    public BigDecimal computeObv(List<MarketData> history) {
        BigDecimal obv = BigDecimal.ZERO;

        for (MarketData md : history) {
            if (null != md.getVolume()) {
                BigDecimal price = md.getPrice();
                BigDecimal prev = md.getPreviousClose();
                BigDecimal vol = BigDecimal.valueOf(md.getVolume());

                if (price.compareTo(prev) > 0) {
                    obv = obv.add(vol);
                } else if (price.compareTo(prev) < 0) {
                    obv = obv.subtract(vol);
                }
            }
        }

        if (obv.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(Constants.KPI_RETURNED_ZERO_WARN, history.stream().max(Comparator.comparing(MarketData::getDate)).orElseThrow());
        }
        return obv;
    }
}
