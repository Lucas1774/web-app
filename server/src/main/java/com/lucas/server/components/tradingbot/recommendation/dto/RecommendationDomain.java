package com.lucas.server.components.tradingbot.recommendation.dto;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import lombok.*;
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
public class RecommendationDomain {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private SymbolDomain symbol;
    private Long marketDataId;
    private Set<NewsDomain> news = new HashSet<>();
    @ToString.Include
    private String action;
    @ToString.Include
    private BigDecimal confidence;
    private String rationale;
    @EqualsAndHashCode.Include
    @ToString.Include
    private LocalDate date;
    @ToString.Include
    private String model;
    private String input;
    private String errors;

    public RecommendationDomain addNews(Set<NewsDomain> news) {
        this.news.addAll(news);
        return this;
    }
}
