package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "news")
public class News implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Long externalId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "news_symbol",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "symbol_id"))
    private Set<Symbol> symbols = new HashSet<>();

    @Column(name = "publication_date", nullable = false)
    private LocalDateTime date;

    @Column(nullable = false, length = 512)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(length = 100)
    private String source;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", length = 512)
    private String image;

    @Column(length = 8)
    private String sentiment;

    @Column(name = "sentiment_confidence", precision = 15, scale = 4)
    private BigDecimal sentimentConfidence;

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3072)
    private float[] embeddings;

    public News addSymbol(Symbol symbol) {
        symbols.add(symbol);
        symbol.getNews().add(this);
        return this;
    }

    @Override
    public String toString() {
        return "News{" +
                ", externalId=" + externalId +
                ", date=" + date +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        News news = (News) o;
        return Objects.equals(externalId, news.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId);
    }
}
