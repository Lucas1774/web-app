package com.lucas.server.components.tradingbot.recommendation.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "recommendation", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol_id", "recommendation_date"}))
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
        marketData.addRecommendation(this);
        return this;
    }
}
