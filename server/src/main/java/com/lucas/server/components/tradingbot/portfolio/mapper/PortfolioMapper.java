package com.lucas.server.components.tradingbot.portfolio.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.portfolio.jpa.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.PortfolioType;

@Component
@RequiredArgsConstructor
public class PortfolioMapper implements EntityMapper<Portfolio, PortfolioDomain> {

    private final PortfolioMapperDelegate portfolioMapperDelegate;

    @Override
    public PortfolioDomain toDto(Portfolio entity) {
        return portfolioMapperDelegate.toDto(entity);
    }

    @Override
    public Portfolio toEntity(PortfolioDomain dto) {
        return (Portfolio) portfolioMapperDelegate.toEntity(dto, PortfolioType.REAL);
    }
}
