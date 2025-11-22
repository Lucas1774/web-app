package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
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
@Table(name = "recommendation", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol_id", "recommendation_date"}))
public class Recommendation implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_data_id")
    @JsonIgnore
    private MarketData marketData;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "recommendation_news",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            inverseJoinColumns = @JoinColumn(name = "news_id"))
    private Set<News> news = new HashSet<>();

    @Column(nullable = false, length = 4)
    private String action;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false, length = 1024)
    private String rationale;

    @Column(name = "recommendation_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String errors;

    public Recommendation addNews(Set<News> news) {
        this.news.addAll(news);
        return this;
    }

    public Recommendation setMarketData(MarketData marketData) {
        this.marketData = marketData;
        marketData.addRecommendation(this);
        return this;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "id=" + id +
                ", symbol=" + symbol +
                ", marketData=" + marketData +
                ", action='" + action + '\'' +
                ", confidence=" + confidence +
                ", date=" + date +
                ", model='" + model + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        Recommendation that = (Recommendation) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, date);
    }
}
