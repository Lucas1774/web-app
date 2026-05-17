package com.lucas.server.components.tradingbot.news.dto;

import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class NewsDomain {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long externalId;
    @ToString.Include
    private Set<SymbolDomain> symbols = new HashSet<>();
    @ToString.Include
    private LocalDateTime date;
    private String headline;
    private String summary;
    private String url;
    private String source;
    private String category;
    private String image;
    private String sentiment;
    private BigDecimal sentimentConfidence;
    private float[] embeddings;

    public NewsDomain addSymbol(SymbolDomain symbol) {
        symbols.add(symbol);
        return this;
    }
}
