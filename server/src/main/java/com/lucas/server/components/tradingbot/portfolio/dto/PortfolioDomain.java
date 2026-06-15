package com.lucas.server.components.tradingbot.portfolio.dto;

import com.lucas.server.common.dto.DomainEntity;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
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
public class PortfolioDomain implements DomainEntity {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private SymbolDomain symbol;
    @ToString.Include
    private BigDecimal quantity;
    @ToString.Include
    private BigDecimal averageCost;
    @ToString.Include
    private BigDecimal averageCommission;
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDateTime effectiveTimestamp;
}
