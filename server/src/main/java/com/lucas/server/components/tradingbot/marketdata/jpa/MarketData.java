package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
    private Symbol symbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "marketData", fetch = FetchType.EAGER)
    private Set<Recommendation> recommendations = new HashSet<>();

    @Column(precision = 15, scale = 4)
    private BigDecimal open;

    @Column(precision = 15, scale = 4)
    private BigDecimal high;

    @Column(precision = 15, scale = 4)
    private BigDecimal low;

    @Column(precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    private Long volume;

    @Column(name = "previous_close", precision = 15, scale = 4)
    private BigDecimal previousClose;

    @Column(precision = 15, scale = 4)
    private BigDecimal change;

    @Column(name = "change_percent", precision = 15, scale = 4)
    private BigDecimal changePercent;

    @Column(precision = 15, scale = 4)
    private BigDecimal atr;

    @Column(precision = 15, scale = 4)
    private BigDecimal averageGain;

    @Column(precision = 15, scale = 4)
    private BigDecimal averageLoss;

    @Column(name = "previous_atr", precision = 15, scale = 4)
    private BigDecimal previousAtr;

    @Column(name = "previous_average_gain", precision = 15, scale = 4)
    private BigDecimal previousAverageGain;

    @Column(name = "previous_average_loss", precision = 15, scale = 4)
    private BigDecimal previousAverageLoss;

    public static MarketData from(MarketSnapshot snapshot) {
        return new MarketData()
                .setSymbol(snapshot.getSymbol())
                .setOpen(snapshot.getOpen())
                .setHigh(snapshot.getHigh())
                .setLow(snapshot.getLow())
                .setPrice(snapshot.getPrice())
                .setVolume(snapshot.getVolume())
                .setDate(null == snapshot.getDate() ? null : snapshot.getDate().toLocalDate())
                .setPrice(snapshot.getPrice());
    }

    public void addRecommendation(Recommendation r) {
        recommendations.add(r);
    }

    @Override
    public String toString() {
        return "MarketData{" +
                "id=" + id +
                ", symbol=" + symbol +
                ", date=" + date +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", price=" + price +
                ", volume=" + volume +
                ", previousClose=" + previousClose +
                ", change=" + change +
                ", changePercent=" + changePercent +
                ", atr=" + atr +
                ", averageGain=" + averageGain +
                ", averageLoss=" + averageLoss +
                ", previousAtr=" + previousAtr +
                ", previousAverageGain=" + previousAverageGain +
                ", previousAverageLoss=" + previousAverageLoss +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        MarketData that = (MarketData) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, date);
    }
}
