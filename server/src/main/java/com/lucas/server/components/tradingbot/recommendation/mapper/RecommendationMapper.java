package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.NewsMapper;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecommendationMapper implements EntityMapper<Recommendation, RecommendationDomain> {

    private final SymbolMapper symbolMapper;
    private final NewsMapper newsMapper;

    @Override
    public RecommendationDomain toDto(Recommendation entity) {
        if (null == entity) {
            return null;
        }
        return new RecommendationDomain(entity.getId(),
                null != entity.getSymbol() ? symbolMapper.toDto(entity.getSymbol()) : null,
                null != entity.getMarketData() ? entity.getMarketData().getId() : null,
                entity.getNews().stream().map(newsMapper::toDto).collect(Collectors.toSet()),
                entity.getAction(),
                entity.getConfidence(),
                entity.getRationale(),
                entity.getDate(),
                entity.getModel(),
                entity.getInput(),
                entity.getErrors());
    }

    @Override
    public Recommendation toEntity(RecommendationDomain dto) {
        if (null == dto) {
            return null;
        }
        Recommendation recommendation = new Recommendation().setId(dto.getId())
                .setSymbol(null != dto.getSymbol() ? symbolMapper.toEntity(dto.getSymbol()) : null)
                .setAction(dto.getAction())
                .setConfidence(dto.getConfidence())
                .setRationale(dto.getRationale())
                .setDate(dto.getDate())
                .setModel(dto.getModel())
                .setInput(dto.getInput())
                .setErrors(dto.getErrors());

        if (null != dto.getMarketDataId()) {
            recommendation.setMarketData(new MarketData().setId(dto.getMarketDataId()));
        }

        if (null != dto.getNews() && !dto.getNews().isEmpty()) {
            Set<News> newsSet = dto.getNews().stream().map(newsMapper::toEntity).collect(Collectors.toSet());
            recommendation.addNews(newsSet);
        }

        return recommendation;
    }
}
