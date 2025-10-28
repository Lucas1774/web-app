package com.lucas.server.components.tradingbot.common.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "symbol")
public class Symbol implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String name;

    @ManyToMany(mappedBy = "symbols", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<News> news = new HashSet<>();

    @Override
    public String toString() {
        return "Symbol{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
