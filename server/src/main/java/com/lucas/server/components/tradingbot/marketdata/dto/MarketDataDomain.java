package com.lucas.server.components.tradingbot.marketdata.dto;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class MarketDataDomain {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private SymbolDomain symbol;
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDate date;
    private Set<RecommendationDomain> recommendations = new HashSet<>();
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
    private BigDecimal previousClose;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal atr;
    private BigDecimal averageGain;
    private BigDecimal averageLoss;
    private BigDecimal previousAtr;
    private BigDecimal previousAverageGain;
    private BigDecimal previousAverageLoss;

    public static MarketDataDomain from(MarketSnapshotDomain snapshot) {
        return new MarketDataDomain().setId(snapshot.getId())
                .setSymbol(snapshot.getSymbol())
                .setOpen(snapshot.getOpen())
                .setHigh(snapshot.getHigh())
                .setLow(snapshot.getLow())
                .setPrice(snapshot.getPrice())
                .setVolume(snapshot.getVolume())
                .setDate(null == snapshot.getDate() ? null : snapshot.getDate().toLocalDate())
                .setPrice(snapshot.getPrice());
    }
}
