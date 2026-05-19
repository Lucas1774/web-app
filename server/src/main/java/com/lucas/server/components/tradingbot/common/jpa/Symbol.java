package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.lucas.server.common.Constants.SYMBOL_TO_SECTOR;
import static com.lucas.server.common.Constants.Sector;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity
@Table(name = "symbol")
public class Symbol implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private Sector sector;

    @ManyToMany(mappedBy = "symbols", fetch = FetchType.LAZY)
    private Set<News> news = new HashSet<>();

    public Symbol computeSector() {
        return setSector(Optional.ofNullable(SYMBOL_TO_SECTOR.get(getName())).orElseThrow());
    }
}
