package com.lucas.server.components.tradingbot.portfolio.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioMock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.PortfolioType;

@Component
@RequiredArgsConstructor
public class PortfolioMockMapper implements EntityMapper<PortfolioMock, PortfolioDomain> {

    private final PortfolioMapperDelegate portfolioMapperDelegate;

    @Override
    public PortfolioDomain toDto(PortfolioMock entity) {
        return portfolioMapperDelegate.toDto(entity);
    }

    @Override
    public PortfolioMock toEntity(PortfolioDomain dto) {
        return (PortfolioMock) portfolioMapperDelegate.toEntity(dto, PortfolioType.MOCK);
    }
}
