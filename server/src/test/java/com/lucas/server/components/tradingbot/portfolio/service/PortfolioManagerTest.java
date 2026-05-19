package com.lucas.server.components.tradingbot.portfolio.service;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioManagerTest {

    private final PortfolioManager manager = new PortfolioManager();

    @Test
    void computeStandWithProfit() {
        // given
        SymbolDomain symbol = new SymbolDomain().setId(1L).setName("AAPL");
        PortfolioDomain portfolio = new PortfolioDomain().setSymbol(symbol)
                .setQuantity(new BigDecimal("10"))
                .setAverageCost(new BigDecimal("100"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketDataDomain last = new MarketDataDomain().setSymbol(symbol)
                .setPrice(new BigDecimal("110"))
                .setOpen(new BigDecimal("108"))
                .setHigh(new BigDecimal("112"))
                .setLow(new BigDecimal("107"))
                .setPreviousClose(new BigDecimal("105"))
                .setVolume(1000000L)
                .setDate(LocalDate.now())
                .setRecommendations(new HashSet<>());

        // when
        PortfolioManager.SymbolStand stand = manager.computeStand(portfolio, last);

        // then
        assertThat(stand.quantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(stand.averageCost()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(stand.positionValue()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(stand.pnL()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(stand.percentPnl()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void computeStandWithLoss() {
        // given
        SymbolDomain symbol = new SymbolDomain().setId(1L).setName("IBM");
        PortfolioDomain portfolio = new PortfolioDomain().setSymbol(symbol)
                .setQuantity(new BigDecimal("5"))
                .setAverageCost(new BigDecimal("200"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketDataDomain last = new MarketDataDomain().setSymbol(symbol)
                .setPrice(new BigDecimal("180"))
                .setOpen(new BigDecimal("185"))
                .setHigh(new BigDecimal("190"))
                .setLow(new BigDecimal("178"))
                .setPreviousClose(new BigDecimal("190"))
                .setVolume(500000L)
                .setDate(LocalDate.now())
                .setRecommendations(new HashSet<>());

        // when
        PortfolioManager.SymbolStand stand = manager.computeStand(portfolio, last);

        // then
        assertThat(stand.pnL()).isEqualByComparingTo(new BigDecimal("-100.0000"));
        assertThat(stand.percentPnl()).isEqualByComparingTo(new BigDecimal("-10.0000"));
    }

    @Test
    void computeStandWithZeroQuantity() {
        // given
        SymbolDomain symbol = new SymbolDomain().setId(1L).setName("GOOG");
        PortfolioDomain portfolio = new PortfolioDomain().setSymbol(symbol)
                .setQuantity(BigDecimal.ZERO)
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketDataDomain last = new MarketDataDomain().setSymbol(symbol)
                .setPrice(new BigDecimal("150"))
                .setPreviousClose(new BigDecimal("145"))
                .setOpen(new BigDecimal("146"))
                .setHigh(new BigDecimal("152"))
                .setLow(new BigDecimal("144"))
                .setDate(LocalDate.now())
                .setRecommendations(new HashSet<>());

        // when
        PortfolioManager.SymbolStand stand = manager.computeStand(portfolio, last);

        // then
        assertThat(stand.positionValue()).isNull();
        assertThat(stand.pnL()).isNull();
        assertThat(stand.percentPnl()).isNull();
    }

    @Test
    void computeStandWithCommission() {
        // given
        SymbolDomain symbol = new SymbolDomain().setId(1L).setName("MSFT");
        PortfolioDomain portfolio = new PortfolioDomain().setSymbol(symbol)
                .setQuantity(new BigDecimal("10"))
                .setAverageCost(new BigDecimal("100"))
                .setAverageCommission(new BigDecimal("0.01"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketDataDomain last = new MarketDataDomain().setSymbol(symbol)
                .setPrice(new BigDecimal("110"))
                .setPreviousClose(new BigDecimal("105"))
                .setOpen(new BigDecimal("106"))
                .setHigh(new BigDecimal("112"))
                .setLow(new BigDecimal("105"))
                .setDate(LocalDate.now())
                .setRecommendations(new HashSet<>());

        // when
        PortfolioManager.SymbolStand stand = manager.computeStand(portfolio, last);

        // then
        assertThat(stand.netRelativePosition()).isNotNull();
    }
}
