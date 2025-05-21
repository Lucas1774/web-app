package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@EntityListeners(MarketDataListener.class)
@Table(name = "market_data", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol_id", "trade_date"}))
public class MarketData implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Symbol symbol;

    @Column(precision = 15, scale = 4)
    private BigDecimal open;

    @Column(precision = 15, scale = 4)
    private BigDecimal high;

    @Column(precision = 15, scale = 4)
    private BigDecimal low;

    @Column(precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    private Long volume;

    @Column(name = "trade_date", nullable = false)
    private LocalDate date;

    @Column(name = "previous_close", precision = 15, scale = 4)
    private BigDecimal previousClose;

    @Column(precision = 15, scale = 4)
    private BigDecimal change;

    @Column(name = "change_percent", precision = 15, scale = 4)
    private BigDecimal changePercent;

    @Override
    public String toString() {
        return "MarketData{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", date=" + date +
                '}';
    }
}
