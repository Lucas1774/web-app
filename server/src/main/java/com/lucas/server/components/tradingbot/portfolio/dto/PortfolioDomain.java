package com.lucas.server.components.tradingbot.portfolio.dto;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
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
public class PortfolioDomain {
    @ToString.Include
    private Long id;
    @ToString.Include
    @EqualsAndHashCode.Include
    private SymbolDomain symbol;
    @ToString.Include
    private BigDecimal quantity;
    @ToString.Include
    private BigDecimal averageCost;
    @ToString.Include
    private BigDecimal averageCommission;
    @ToString.Include
    @EqualsAndHashCode.Include
    private LocalDateTime effectiveTimestamp;
}
