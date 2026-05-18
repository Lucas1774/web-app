package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.INSUFFICIENT_STOCK_ERROR;

public abstract class PortfolioJpaServiceDelegate<T extends PortfolioBase, R extends JpaRepository<T, Long>> extends GenericJpaServiceDelegate<T, PortfolioDomain, R> implements IPortfolioJpaService {

    private final Function<Long, Optional<T>> findLatestBySymbol;
    private final Supplier<T> builder;

    public PortfolioJpaServiceDelegate(R repository,
                                       EntityMapper<T, PortfolioDomain> mapper,
                                       Function<Long, Optional<T>> findLatestBySymbol,
                                       Supplier<T> builder) {
        super(repository, mapper);
        this.findLatestBySymbol = findLatestBySymbol;
        this.builder = builder;
    }

    @Override
    @Transactional(readOnly = true)
    // TODO: batch
    public Optional<PortfolioDomain> findBySymbol(SymbolDomain symbol) {
        return findLatestBySymbol.apply(symbol.getId()).map(mapper::toDto);
    }

    @Override
    @Transactional
    public PortfolioDomain executePortfolioAction(SymbolDomain symbol, BigDecimal price, BigDecimal quantity, BigDecimal commission,
                                                  LocalDateTime timestamp, boolean isBuy) throws IllegalStateException {
        PortfolioDomain last = findLatestBySymbol.apply(symbol.getId()).map(mapper::toDto)
                .orElseGet(() -> {
                    T res = builder.get();
                    res.setQuantity(BigDecimal.ZERO)
                            .setAverageCost(BigDecimal.ZERO)
                            .setAverageCommission(BigDecimal.ZERO)
                            .setEffectiveTimestamp(timestamp);
                    return mapper.toDto(res);
                });
        BigDecimal oldQuantity = last.getQuantity();
        BigDecimal newQuantity = oldQuantity.add(isBuy ? quantity : quantity.negate());
        if (0 > newQuantity.compareTo(BigDecimal.ZERO)) {
            throw new IllegalStateException(MessageFormat.format(INSUFFICIENT_STOCK_ERROR, symbol.getName()));
        }

        BigDecimal newAverageCost = last.getAverageCost();
        BigDecimal newAverageCommission = last.getAverageCommission();
        if (isBuy) {
            BigDecimal totalCost = last.getAverageCost().multiply(oldQuantity)
                    .add(price.multiply(quantity));
            newAverageCost = 0 < newQuantity.compareTo(BigDecimal.ZERO)
                    ? totalCost.divide(newQuantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal totalCommissionWeighted = last.getAverageCommission().multiply(oldQuantity)
                    .add(commission.multiply(quantity));
            newAverageCommission = 0 < newQuantity.compareTo(BigDecimal.ZERO)
                    ? totalCommissionWeighted.divide(newQuantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        T res = builder.get();
        res.setSymbol(new Symbol().setId(symbol.getId()))
                .setQuantity(newQuantity)
                .setAverageCost(newAverageCost)
                .setAverageCommission(newAverageCommission)
                .setEffectiveTimestamp(timestamp);
        return mapper.toDto(repository.save(res)).setSymbol(symbol);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PortfolioDomain> findActivePortfolio() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(
                        T::getSymbol,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(T::getEffectiveTimestamp))
                ))
                .values()
                .stream()
                .filter(p -> 0 < p.getQuantity().compareTo(BigDecimal.ZERO))
                .map(mapper::toDto)
                .collect(Collectors.toSet());
    }
}
