package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.INSUFFICIENT_STOCK_ERROR;

public class PortfolioJpaServiceDelegate<T extends PortfolioBase, R extends JpaRepository<T, Long>> {

    private final R repository;
    private final Function<Symbol, Optional<T>> findLatestBySymbol;
    private final Supplier<T> builder;

    public PortfolioJpaServiceDelegate(R repository, Function<Symbol, Optional<T>> findLatestBySymbol, Supplier<T> builder) {
        this.repository = repository;
        this.findLatestBySymbol = findLatestBySymbol;
        this.builder = builder;
    }

    public T save(T entity) {
        return repository.save(entity);
    }

    // TODO: batch
    public Optional<T> findBySymbol(Symbol symbol) {
        return findLatestBySymbol.apply(symbol);
    }

    public T executePortfolioAction(Symbol symbol, BigDecimal price, BigDecimal quantity, BigDecimal commission,
                                    LocalDateTime timestamp, boolean isBuy) throws IllegalStateException {
        T last = findBySymbol(symbol)
                .orElseGet(() -> {
                    T res = builder.get();
                    res.setSymbol(symbol)
                            .setQuantity(BigDecimal.ZERO)
                            .setAverageCost(BigDecimal.ZERO)
                            .setAverageCommission(BigDecimal.ZERO)
                            .setEffectiveTimestamp(timestamp);
                    return res;
                });
        BigDecimal oldQuantity = last.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(isBuy ? quantity : quantity.negate());
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(MessageFormat.format(INSUFFICIENT_STOCK_ERROR, symbol.getName()));
        }

        BigDecimal newAverageCost = last.getAverageCost();
        BigDecimal newAverageCommission = last.getAverageCommission();
        if (isBuy) {
            BigDecimal totalCost = last.getAverageCost().multiply(oldQuantity)
                    .add(price.multiply(quantity));
            newAverageCost = newQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(newQuantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal totalCommissionWeighted = last.getAverageCommission().multiply(oldQuantity)
                    .add(commission.multiply(quantity));
            newAverageCommission = newQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCommissionWeighted.divide(newQuantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        T res = builder.get();
        res.setSymbol(symbol)
                .setQuantity(newQuantity)
                .setAverageCost(newAverageCost)
                .setAverageCommission(newAverageCommission)
                .setEffectiveTimestamp(timestamp);
        return save(res);
    }

    public List<T> findActivePortfolio() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(
                        T::getSymbol,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(T::getEffectiveTimestamp))
                ))
                .values()
                .stream()
                .filter(p -> p.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }
}
