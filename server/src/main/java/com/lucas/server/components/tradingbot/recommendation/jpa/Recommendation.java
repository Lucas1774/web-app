package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity
@Table(name = "recommendation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol_id", "recommendation_date"}))
public class Recommendation implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Symbol symbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_data_id")
    private MarketData marketData;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "recommendation_news",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            inverseJoinColumns = @JoinColumn(name = "news_id"))
    private Set<News> news = new HashSet<>();

    @Column(nullable = false, length = 4)
    @ToString.Include
    private String action;

    @Column(nullable = false, precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal confidence;

    @Column(nullable = false, length = 1024)
    private String rationale;

    @Column(name = "recommendation_date", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDate date;

    @Column(nullable = false, length = 50)
    @ToString.Include
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
        marketData.getRecommendations().add(this);
        return this;
    }
}
