package com.lucas.server.components.tradingbot.portfolio.mapper;

import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.portfolio.jpa.Portfolio;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioMock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.PortfolioType;

@Component
@RequiredArgsConstructor
public class PortfolioMapperDelegate {

    private final SymbolMapper symbolMapper;

    public PortfolioDomain toDto(PortfolioBase entity) {
        if (null == entity) {
            return null;
        }
        return new PortfolioDomain(entity.getId(),
                null != entity.getSymbol() ? symbolMapper.toDto(entity.getSymbol()) : null,
                entity.getQuantity(),
                entity.getAverageCost(),
                entity.getAverageCommission(),
                entity.getEffectiveTimestamp());
    }

    public PortfolioBase toEntity(PortfolioDomain dto, PortfolioType type) {
        if (null == dto) {
            return null;
        }
        PortfolioBase res = switch (type) {
            case REAL -> new Portfolio();
            case MOCK -> new PortfolioMock();
        };
        return res.setId(dto.getId())
                .setSymbol(null != dto.getSymbol() ? symbolMapper.toEntity(dto.getSymbol()) : null)
                .setQuantity(dto.getQuantity())
                .setAverageCost(dto.getAverageCost())
                .setAverageCommission(dto.getAverageCommission())
                .setEffectiveTimestamp(dto.getEffectiveTimestamp());
    }
}
