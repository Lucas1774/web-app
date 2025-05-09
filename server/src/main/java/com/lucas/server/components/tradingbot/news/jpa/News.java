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

import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@EntityListeners(NewsListener.class)
@Table(name = "news")
public class News implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Long externalId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

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

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3072)
    private float[] embeddings;

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
