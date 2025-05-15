package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class PortfolioJpaServiceDelegate<T extends PortfolioBase, R extends JpaRepository<T, Long>> {

    private final R repository;
    private final Function<Symbol, Optional<T>> findLatestBySymbol;
    private final Supplier<T> builder;

    public PortfolioJpaServiceDelegate(R repository, Function<Symbol, Optional<T>> findLatestBySymbol, Supplier<T> builder) {
        this.repository = repository;
        this.findLatestBySymbol = findLatestBySymbol;
        this.builder = builder;
    }

    public List<T> createAll(List<T> entities) {
        return this.repository.saveAll(entities);
    }

    public void deleteAll() {
        this.repository.deleteAll();
    }

    public List<T> findAll() {
        return this.repository.findAll();
    }

    public T save(T entity) {
        return this.repository.save(entity);
    }

    public Optional<T> findBySymbol(Symbol symbol) {
        return this.findLatestBySymbol.apply(symbol);
    }

    public T executePortfolioAction(Symbol symbol, BigDecimal price, BigDecimal quantity, LocalDateTime timestamp,
                                    boolean isBuy) throws IllegalStateException {

        T last = this.findBySymbol(symbol)
                .orElseGet(() -> {
                    T res = this.builder.get();
                    res.setSymbol(symbol)
                            .setQuantity(BigDecimal.ZERO)
                            .setAverageCost(BigDecimal.ZERO)
                            .setEffectiveTimestamp(timestamp);
                    return this.save(res);
                });
        BigDecimal oldQuantity = last.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(isBuy ? quantity : quantity.negate());
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(MessageFormat.format(Constants.INSUFFICIENT_STOCK_ERROR, symbol.getName()));
        }

        BigDecimal newAverage = last.getAverageCost();
        if (isBuy) {
            BigDecimal totalCost = last.getAverageCost().multiply(oldQuantity)
                    .add(price.multiply(quantity));
            newAverage = newQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(newQuantity, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        T res = this.builder.get();
        res.setSymbol(symbol)
                .setQuantity(newQuantity)
                .setAverageCost(newAverage)
                .setEffectiveTimestamp(timestamp);
        return this.save(res);
    }
}
