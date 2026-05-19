package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity
@Table(name = "market_snapshot")
// TODO: reference this entity in MarketDataDomain entities
public class MarketSnapshot implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Symbol symbol;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDateTime date;

    @Column(precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal open;

    @Column(precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal high;

    @Column(precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal low;

    @Column(precision = 15, scale = 4, nullable = false)
    @ToString.Include
    private BigDecimal price;

    @ToString.Include
    private Long volume;
}
