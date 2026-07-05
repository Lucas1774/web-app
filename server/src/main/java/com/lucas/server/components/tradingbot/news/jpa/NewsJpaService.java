package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NewsJpaService extends GenericJpaServiceDelegate<News, NewsDomain, NewsRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<News> delegate;
    private final NewsSentimentClient sentimentClient;

    public NewsJpaService(NewsRepository repository,
                          EntityMapper<News, NewsDomain> mapper,
                          NewsSentimentClient sentimentClient) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.sentimentClient = sentimentClient;
    }

    // TODO: batch
    @Transactional(readOnly = true)
    public OrderedIndexedSet<NewsDomain> getTopForSymbolId(Long symbolId, int limit) {
        return repository.findBySymbols_IdAndSentimentNotOrSymbols_IdAndSentimentIsNull(symbolId,
                        "neutral",
                        symbolId,
                        PageRequest.of(0, limit, Sort.by("date").descending()))
                .stream()
                .map(mapper::toDto)
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    // Commit on finish so that nested threads that lost transactional context can see changes
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<NewsDomain> createOrUpdate(Set<NewsDomain> entities) {
        Set<News> entitySet = entities.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return delegate.createOrUpdate(allEntities -> repository.findByExternalIdIn(allEntities.stream()
                .map(News::getExternalId)
                .collect(Collectors.toUnmodifiableSet())), (oldEntity, newEntity) -> {
            for (Symbol symbol : newEntity.getSymbols()) {
                oldEntity.getSymbols().add(symbol);
            }
            return oldEntity;
        }, entitySet).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public Set<NewsDomain> generateSentiment(Set<Long> symbolIds, LocalDateTime from, LocalDateTime to) {
        return repository.findAllBySymbols_IdInAndDateBetween(symbolIds, from, to)
                .stream()
                .filter(news -> null == news.getSentiment() || null == news.getSentimentConfidence())
                .flatMap(newsEntity -> {
                    try {
                        NewsDomain newsDto = mapper.toDto(newsEntity);
                        return Stream.of(sentimentClient.generateSentiment(newsDto));
                    } catch (Exception _) {
                        log.warn(RETRIEVAL_FAILED_WARN, SENTIMENT, newsEntity);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<NewsDomain> findBySymbolId(Long symbolId) {
        return repository.findBySymbols_Id(symbolId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Set<NewsDomain> findOrphanedNews() {
        return repository.findBySymbolsIsEmpty().stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }
}
