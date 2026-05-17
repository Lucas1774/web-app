package com.lucas.server.components.tradingbot.portfolio.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import jakarta.persistence.*;
import lombok.*;
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
@MappedSuperclass
public abstract class PortfolioBase implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symbol_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Symbol symbol;

    @Column(nullable = false, precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal averageCost;

    @Column(name = "average_commission", nullable = false, precision = 15, scale = 4)
    @ToString.Include
    private BigDecimal averageCommission;

    @Column(name = "effective_timestamp", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDateTime effectiveTimestamp;
}
