package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketDataMapper implements EntityMapper<MarketData, MarketDataDomain> {

    private final SymbolMapper symbolMapper;
    private final RecommendationMapper recommendationMapper;

    @Override
    public MarketDataDomain toDto(MarketData entity) {
        if (null == entity) {
            return null;
        }
        return new MarketDataDomain(entity.getId(),
                null != entity.getSymbol() ? symbolMapper.toDto(entity.getSymbol()) : null,
                entity.getDate(),
                entity.getRecommendations().stream().map(recommendationMapper::toDto).collect(Collectors.toSet()),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getPrice(),
                entity.getVolume(),
                entity.getPreviousClose(),
                entity.getChange(),
                entity.getChangePercent(),
                entity.getAtr(),
                entity.getAverageGain(),
                entity.getAverageLoss(),
                entity.getPreviousAtr(),
                entity.getPreviousAverageGain(),
                entity.getPreviousAverageLoss());
    }

    @Override
    public MarketData toEntity(MarketDataDomain dto) {
        if (null == dto) {
            return null;
        }
        MarketData marketData = new MarketData().setId(dto.getId())
                .setSymbol(null != dto.getSymbol() ? symbolMapper.toEntity(dto.getSymbol()) : null)
                .setDate(dto.getDate())
                .setOpen(dto.getOpen())
                .setHigh(dto.getHigh())
                .setLow(dto.getLow())
                .setPrice(dto.getPrice())
                .setVolume(dto.getVolume())
                .setPreviousClose(dto.getPreviousClose())
                .setChange(dto.getChange())
                .setChangePercent(dto.getChangePercent())
                .setAtr(dto.getAtr())
                .setAverageGain(dto.getAverageGain())
                .setAverageLoss(dto.getAverageLoss())
                .setPreviousAtr(dto.getPreviousAtr())
                .setPreviousAverageGain(dto.getPreviousAverageGain())
                .setPreviousAverageLoss(dto.getPreviousAverageLoss());

        if (null != dto.getRecommendations()) {
            dto.getRecommendations()
                    .stream()
                    .map(recommendationMapper::toEntity)
                    .forEach(marketData::addRecommendation);
        }
        return marketData;
    }
}
