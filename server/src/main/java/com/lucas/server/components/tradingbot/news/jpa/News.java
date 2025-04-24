package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 10)
    private String symbol;

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

    public News(Long externalId, String symbol, LocalDateTime date, String headline,
                String summary, String url, String source, String category, String image) {
        this.externalId = externalId;
        this.symbol = symbol;
        this.date = date;
        this.headline = headline;
        this.summary = summary;
        this.url = url;
        this.source = source;
        this.category = category;
        this.image = image;
    }

    @Override
    public String toString() {
        return "News{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", symbol='" + symbol + '\'' +
                ", date=" + date +
                ", headline='" + headline + '\'' +
                '}';
    }
}
