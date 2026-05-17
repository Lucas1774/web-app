package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.mapper.NewsMapper;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.RETRIEVAL_FAILED_WARN;
import static com.lucas.server.common.Constants.SENTIMENT;

@Service
public class NewsJpaService implements JpaService<NewsDomain> {

    private static final Logger logger = LoggerFactory.getLogger(NewsJpaService.class);
    private final GenericJpaServiceDelegate<News, NewsDomain, NewsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<News> uniqueConstraintDelegate;
    private final NewsRepository repository;
    private final NewsSentimentClient sentimentClient;
    private final NewsMapper newsMapper;

    public NewsJpaService(NewsRepository repository, NewsMapper newsMapper, NewsSentimentClient sentimentClient) {
        delegate = new GenericJpaServiceDelegate<>(repository, newsMapper);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.sentimentClient = sentimentClient;
        this.newsMapper = newsMapper;
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public OrderedIndexedSet<NewsDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(
                        symbolId, "neutral", symbolId, PageRequest.of(
                                0, limit, Sort.by("date").descending()
                        )).stream()
                .map(newsMapper::toDto)
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    // Commit on finish so that nested threads that lost transactional context can see changes
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<NewsDomain> createOrUpdate(Set<NewsDomain> entities) {
        Set<News> newsEntities = entities.stream()
                .map(newsMapper::toEntity)
                .collect(Collectors.toSet());
        return uniqueConstraintDelegate.createOrUpdate(allEntities -> repository.findByExternalIdIn(
                                allEntities.stream().map(News::getExternalId).collect(Collectors.toSet())
                        ),
                        (oldEntity, newEntity) -> {
                            for (Symbol symbol : newEntity.getSymbols()) {
                                oldEntity.getSymbols().add(symbol);
                                symbol.getNews().add(oldEntity);
                            }
                            return oldEntity;
                        },
                        newsEntities).stream()
                .map(newsMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<NewsDomain> generateSentiment(Set<Long> symbolIds, LocalDateTime from, LocalDateTime to) {
        return repository.findAllBySymbols_IdInAndDateBetween(symbolIds, from, to).stream()
                .filter(news -> null == news.getSentiment() || null == news.getSentimentConfidence())
                .flatMap(newsEntity -> {
                    try {
                        NewsDomain newsDto = newsMapper.toDto(newsEntity);
                        return Stream.of(sentimentClient.generateSentiment(newsDto));
                    } catch (Exception e) {
                        logger.warn(RETRIEVAL_FAILED_WARN, SENTIMENT, newsEntity);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<NewsDomain> findOrphanedNews() {
        return repository.findBySymbolsIsEmpty().stream()
                .map(newsMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<NewsDomain> findByIdIn(Set<Long> newsIds) {
        return repository.findByIdIn(newsIds).stream()
                .map(newsMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<NewsDomain> saveAll(Set<NewsDomain> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<NewsDomain> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<NewsDomain> elements) {
        delegate.deleteAll(elements);
    }
}
