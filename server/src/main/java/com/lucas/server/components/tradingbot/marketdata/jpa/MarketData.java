package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "market_data")
public class MarketData implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(precision = 15, scale = 4)
    private BigDecimal open;

    @Column(precision = 15, scale = 4)
    private BigDecimal high;

    @Column(precision = 15, scale = 4)
    private BigDecimal low;

    @Column(precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    private Long volume;

    @Column(name = "last_trading_day")
    private LocalDate lastTradingDay;

    @Column(name = "previous_close", precision = 15, scale = 4)
    private BigDecimal previousClose;

    @Column(precision = 15, scale = 4)
    private BigDecimal change;


    @Column(name = "change_percent", length = 8)
    private String changePercent;

    public MarketData(String symbol, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal price, Long volume,
                      LocalDate lastTradingDay, BigDecimal previousClose, BigDecimal change, String changePercent) {
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.price = price;
        this.volume = volume;
        this.lastTradingDay = lastTradingDay;
        this.previousClose = previousClose;
        this.change = change;
        this.changePercent = changePercent;
    }
}
