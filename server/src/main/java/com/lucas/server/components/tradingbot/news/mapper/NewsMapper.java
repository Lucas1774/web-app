package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.jpa.News;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NewsMapper implements EntityMapper<News, NewsDomain> {

    private final SymbolMapper symbolMapper;

    @Override
    public NewsDomain toDto(News entity) {
        if (null == entity) {
            return null;
        }
        return new NewsDomain(entity.getId(),
                entity.getExternalId(),
                entity.getSymbols().stream().map(symbolMapper::toDto).collect(Collectors.toSet()),
                entity.getDate(),
                entity.getHeadline(),
                entity.getSummary(),
                entity.getUrl(),
                entity.getSource(),
                entity.getCategory(),
                entity.getImage(),
                entity.getSentiment(),
                entity.getSentimentConfidence(),
                entity.getEmbeddings());
    }

    @Override
    public News toEntity(NewsDomain dto) {
        if (null == dto) {
            return null;
        }
        News news = new News().setId(dto.getId())
                .setExternalId(dto.getExternalId())
                .setDate(dto.getDate())
                .setHeadline(dto.getHeadline())
                .setSummary(dto.getSummary())
                .setUrl(dto.getUrl())
                .setSource(dto.getSource())
                .setCategory(dto.getCategory())
                .setImage(dto.getImage())
                .setSentiment(dto.getSentiment())
                .setSentimentConfidence(dto.getSentimentConfidence())
                .setEmbeddings(dto.getEmbeddings());

        if (null != dto.getSymbols()) {
            dto.getSymbols().stream().map(symbolMapper::toEntity).forEach(news::addSymbol);
        }
        return news;
    }
}
