package com.lucas.server.components.tradingbot.portfolio.service;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.portfolio.jpa.Portfolio;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
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
        Symbol symbol = new Symbol().setId(1L).setName("AAPL");
        PortfolioBase portfolio = new Portfolio()
                .setSymbol(symbol)
                .setQuantity(new BigDecimal("10"))
                .setAverageCost(new BigDecimal("100"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketData last = new MarketData()
                .setSymbol(symbol)
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
        Symbol symbol = new Symbol().setId(1L).setName("IBM");
        PortfolioBase portfolio = new Portfolio()
                .setSymbol(symbol)
                .setQuantity(new BigDecimal("5"))
                .setAverageCost(new BigDecimal("200"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketData last = new MarketData()
                .setSymbol(symbol)
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
        Symbol symbol = new Symbol().setId(1L).setName("GOOG");
        PortfolioBase portfolio = new Portfolio()
                .setSymbol(symbol)
                .setQuantity(BigDecimal.ZERO)
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketData last = new MarketData()
                .setSymbol(symbol)
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
        Symbol symbol = new Symbol().setId(1L).setName("MSFT");
        PortfolioBase portfolio = new Portfolio()
                .setSymbol(symbol)
                .setQuantity(new BigDecimal("10"))
                .setAverageCost(new BigDecimal("100"))
                .setAverageCommission(new BigDecimal("0.01"))
                .setEffectiveTimestamp(LocalDateTime.of(2024, 1, 1, 0, 0));

        MarketData last = new MarketData()
                .setSymbol(symbol)
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
