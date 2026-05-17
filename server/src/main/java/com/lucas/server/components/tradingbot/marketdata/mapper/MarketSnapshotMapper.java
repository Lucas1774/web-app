package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketSnapshotMapper implements EntityMapper<com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot, MarketSnapshotDomain> {

    private final SymbolMapper symbolMapper;

    @Override
    public MarketSnapshotDomain toDto(com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot entity) {
        if (null == entity) return null;
        return new MarketSnapshotDomain(
                entity.getId(),
                null != entity.getSymbol() ? symbolMapper.toDto(entity.getSymbol()) : null,
                entity.getDate(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getPrice(),
                entity.getVolume()
        );
    }

    @Override
    public com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot toEntity(MarketSnapshotDomain dto) {
        if (null == dto) return null;
        return new com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot()
                .setId(dto.getId())
                .setSymbol(null != dto.getSymbol() ? symbolMapper.toEntity(dto.getSymbol()) : null)
                .setDate(dto.getDate())
                .setOpen(dto.getOpen())
                .setHigh(dto.getHigh())
                .setLow(dto.getLow())
                .setPrice(dto.getPrice())
                .setVolume(dto.getVolume());
    }
}
