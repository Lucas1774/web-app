package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.*;

@Component
public class MarketDataKpiGenerator {

    private final MarketDataJpaService service;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataKpiGenerator.class);

    public MarketDataKpiGenerator(MarketDataJpaService service) {
        this.service = service;
    }

    public MarketData computeDerivedFields(MarketData md) {
        List<MarketData> previous14 = service.findTop14BySymbolIdAndDateBeforeOrderByDateDesc(md.getSymbol().getId(), md.getDate());
        if (previous14.isEmpty()) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "anything", md);
            return md;
        }
        MarketData previous = previous14.getFirst();
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

        computeIfAbsent(md::getPreviousAtr, md::setPreviousAtr, previous.getAtr());
        computeIfAbsent(md::getPreviousAverageGain, md::setPreviousAverageGain, previous.getAverageGain());
        computeIfAbsent(md::getPreviousAverageLoss, md::setPreviousAverageLoss, previous.getAverageLoss());
        if (previous14.size() < 14) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "RSI, ATR", previous14);
            return md;
        }
        previous14.removeLast();
        previous14.addFirst(md);
        if (null == md.getAverageGain() || null == md.getAverageLoss()) {
            computeGainsAndLoses(previous14);
        }
        computeIfAbsent(md::getAtr, md::setAtr, computeAtr(previous14).orElse(null));
        return md;
    }

    private static <T> void computeIfAbsent(Supplier<T> getter, Consumer<T> setter, T value) {
        if (getter.get() == null) {
            setter.accept(value);
        }
    }

    /**
     * @param history list of n consecutive MarketData entries
     * @param n       size
     * @return simple moving n-average of closing prices
     */
    public Optional<BigDecimal> computeMovingAverage(List<MarketData> history, int n) {
        if (history.size() < n) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "moving average", history);
            return Optional.empty();
        }
        List<MarketData> cropped = history.subList(0, n);
        BigDecimal average = computeMean(cropped.stream().map(MarketData::getPrice).toList());

        if (average.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(KPI_RETURNED_ZERO_WARN, "moving average", cropped);
        }
        return Optional.of(average);
    }

    /**
     * @param history list of n consecutive MarketData entries
     * @param n       size
     * @return exponential moving n‑average of closing prices (EMA)
     */
    public Optional<BigDecimal> computeEma(List<MarketData> history, int n) {
        if (history.size() < n) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "exponential moving average", history);
            return Optional.empty();
        }
        List<MarketData> cropped = history.subList(0, n).reversed();
        BigDecimal ema = computeEma(cropped.stream().map(MarketData::getPrice).toList());

        if (ema.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(KPI_RETURNED_ZERO_WARN, "exponential moving average", cropped);
        }
        return Optional.of(ema.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * @param history     list of n consecutive MarketData entries
     * @param fastEmaSize fast ema size
     * @param slowEmaSize slow ema size
     * @return MACD line = EMA(fastEmaSize) − EMA(slowEmaSize) of closing prices
     */
    public Optional<BigDecimal> computeMacdLine(List<MarketData> history, int fastEmaSize, int slowEmaSize) {
        if (history.size() < slowEmaSize) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "MACD", history);
            return Optional.empty();
        }
        return computeEma(history, fastEmaSize)
                .flatMap(fast -> computeEma(history, slowEmaSize)
                        .map(slow -> fast.subtract(slow)
                                .setScale(4, RoundingMode.HALF_UP)));
    }

    /**
     * @param history     list of m consecutive MarketData entries.
     * @param n           signal line size
     * @param fastEmaSize fast ema size
     * @param slowEmaSize slow ema size
     * @return signal line = n‑day EMA of the MACD line
     */
    public Optional<BigDecimal> computeSignalLine(List<MarketData> history, int n, int fastEmaSize, int slowEmaSize) {
        if (history.size() < slowEmaSize + (n - 1)) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, "signal line", history);
            return Optional.empty();
        }

        List<BigDecimal> macdHistory = IntStream.range(0, n)
                .mapToObj(i -> computeMacdLine(history.subList(i, history.size()), fastEmaSize, slowEmaSize).orElseThrow())
                .toList()
                .reversed();

        BigDecimal ema = computeEma(macdHistory);

        return Optional.of(ema.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * @param history list of 14 consecutive MarketData entries. The previousClose param allows for not 14 + 1 needed.
     */
    private void computeGainsAndLoses(List<MarketData> history) {
        MarketData current = history.getFirst();
        BigDecimal previousAvgGain = current.getPreviousAverageGain();
        BigDecimal previousAvgLoss = current.getPreviousAverageLoss();
        BigDecimal averageGain;
        BigDecimal averageLoss;

        if (previousAvgGain != null && previousAvgLoss != null) {
            BigDecimal change = current.getChange();
            BigDecimal gain = change.signum() > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.signum() < 0 ? change.abs() : BigDecimal.ZERO;

            averageGain = previousAvgGain.multiply(BigDecimal.valueOf(13))
                    .add(gain)
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
            averageLoss = previousAvgLoss.multiply(BigDecimal.valueOf(13))
                    .add(loss)
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
        } else {
            List<BigDecimal> changes = history.reversed().stream().map(MarketData::getChange).toList();
            BigDecimal totalGains = BigDecimal.ZERO;
            BigDecimal totalLosses = BigDecimal.ZERO;

            for (BigDecimal change : changes) {
                if (change.signum() > 0) {
                    totalGains = totalGains.add(change);
                } else {
                    totalLosses = totalLosses.add(change.abs());
                }
            }
            averageGain = totalGains.divide(BigDecimal.valueOf(history.size()), 8, RoundingMode.HALF_UP);
            averageLoss = totalLosses.divide(BigDecimal.valueOf(history.size()), 8, RoundingMode.HALF_UP);
        }
        current.setAverageGain(averageGain);
        current.setAverageLoss(averageLoss);
    }

    public BigDecimal computeRsi(MarketData md) {
        BigDecimal averageGain = md.getAverageGain();
        BigDecimal averageLoss = md.getAverageLoss();

        if (averageGain == null || averageLoss == null) {
            return null;
        }

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            if (averageGain.compareTo(BigDecimal.ZERO) == 0) {
                logger.warn(KPI_RETURNED_ZERO_WARN, "RSI", md);
            }
            return BigDecimal.valueOf(100).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal relativeStrength = averageGain.divide(averageLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100)
                .divide(relativeStrength.add(BigDecimal.ONE), 4, RoundingMode.HALF_UP));
    }

    /**
     * @param history list of 14 consecutive MarketData entries. The previousClose param allows for not 14 + 1 needed.
     * @return Wilder-smoothed average true range over 14 periods (ATR14)
     */
    private Optional<BigDecimal> computeAtr(List<MarketData> history) {
        MarketData current = history.getFirst();
        BigDecimal previousAtr = current.getPreviousAtr();
        BigDecimal atr;

        if (previousAtr != null) {
            BigDecimal highLow = current.getHigh().subtract(current.getLow());
            BigDecimal highPrevClose = current.getHigh().subtract(current.getPreviousClose());
            BigDecimal lowPrevClose = current.getLow().subtract(current.getPreviousClose());
            BigDecimal trueRange = Stream.of(highLow, highPrevClose, lowPrevClose)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            atr = previousAtr.multiply(BigDecimal.valueOf(13))
                    .add(trueRange)
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
        } else {
            atr = history.reversed().stream()
                    .map(md -> {
                        BigDecimal highLow = md.getHigh().subtract(md.getLow());
                        BigDecimal highPrevClose = md.getHigh().subtract(md.getPreviousClose());
                        BigDecimal lowPrevClose = md.getLow().subtract(md.getPreviousClose());
                        return Stream.of(highLow, highPrevClose, lowPrevClose)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(14), 8, RoundingMode.HALF_UP);
        }

        if (atr.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(KPI_RETURNED_ZERO_WARN, "ATR", history);
        }
        return Optional.of(atr.setScale(4, RoundingMode.HALF_UP));
    }

    public BigDecimal computeRelativeAtr(MarketData md) {
        BigDecimal atr = md.getAtr();
        if (null == atr) {
            return null;
        }
        return atr
                .divide(md.getPrice(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @param n       size
     * @return annualized volatility in percent (std dev of daily returns)
     */
    public Optional<BigDecimal> computeVolatility(List<MarketData> history, int n) {
        if (history.size() < n) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, VOLATILITY, history);
            return Optional.empty();
        }
        List<MarketData> cropped = history.subList(0, n).reversed();
        if (!cropped.stream().allMatch(md -> md.getPreviousClose() != null)) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, VOLATILITY, cropped);
            return Optional.empty();
        }

        List<BigDecimal> dailyReturns = new java.util.ArrayList<>();
        for (MarketData md : cropped) {
            dailyReturns.add(
                    md.getPrice().subtract(md.getPreviousClose())
                            .divide(md.getPreviousClose(), 8, RoundingMode.HALF_UP)
            );
        }

        BigDecimal totalSquaredDeviation = dailyReturns.stream()
                .map(r -> r.subtract(computeMean(dailyReturns)).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = totalSquaredDeviation
                .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (standardDeviation.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn(KPI_RETURNED_ZERO_WARN, VOLATILITY, cropped);
        }
        return Optional.of(standardDeviation.multiply(BigDecimal.valueOf(Math.sqrt(252)))
                .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * @param history list of n consecutive MarketData entries. The previousClose param allows for not n + 1 needed.
     * @param n       size
     * @return On‑Balance Volume (OBV) accumulated over n periods
     */
    public Optional<BigDecimal> computeObv(List<MarketData> history, int n) {
        if (history.size() < n) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, OBV, history);
            return Optional.empty();
        }
        List<MarketData> cropped = history.subList(0, n).reversed();
        if (!cropped.stream().allMatch(md -> md.getPreviousClose() != null)) {
            logger.warn(NON_COMPUTABLE_KPI_WARN, OBV, cropped);
            return Optional.empty();
        }

        BigDecimal obv = BigDecimal.ZERO;
        for (MarketData md : cropped) {
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
            logger.warn(KPI_RETURNED_ZERO_WARN, OBV, cropped);
        }
        return Optional.of(obv);
    }

    private BigDecimal computeMean(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal computeEma(List<BigDecimal> oldestToNewestValues) {
        int n = oldestToNewestValues.size();
        BigDecimal k = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(n + 1L), 8, RoundingMode.HALF_UP);

        BigDecimal average = computeMean(oldestToNewestValues);

        for (int i = 1; i < n; i++) {
            average = oldestToNewestValues.get(i).subtract(average)
                    .multiply(k)
                    .add(average)
                    .setScale(8, RoundingMode.HALF_UP);
        }
        return average;
    }
}
