package com.lucas.server.components.tradingbot.marketdata.dto;

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
public class MarketSnapshotDomain {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private SymbolDomain symbol;
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDateTime date;
    @ToString.Include
    private BigDecimal open;
    @ToString.Include
    private BigDecimal high;
    @ToString.Include
    private BigDecimal low;
    @ToString.Include
    private BigDecimal price;
    @ToString.Include
    private Long volume;
}
