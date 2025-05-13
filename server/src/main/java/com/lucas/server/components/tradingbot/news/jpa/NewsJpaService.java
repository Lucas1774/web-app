package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;

@Service
public class NewsJpaService implements JpaService<News> {

    private final NewsRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<News> delegate;
    private final NewsEmbeddingsClient embeddingsClient;

    public NewsJpaService(NewsRepository repository, UniqueConstraintWearyJpaServiceDelegate<News> delegate, NewsEmbeddingsClient embeddingsClient) {
        this.repository = repository;
        this.delegate = delegate;
        this.embeddingsClient = embeddingsClient;
    }

    @Override
    public List<News> createAll(List<News> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<News> findAll() {
        return repository.findAll();
    }

    public List<News> createIgnoringDuplicates(Iterable<News> entities, boolean triggerEntityCallback) {
        NewsListener.setActive(triggerEntityCallback);
        return this.createIgnoringDuplicates(entities);
    }

    private List<News> createIgnoringDuplicates(Iterable<News> entities) {
        return this.delegate.createIgnoringDuplicates(repository,
                entity -> this.repository.findByExternalId(entity.getExternalId()), entities);
    }

    public List<News> getTopForSymbolId(Long symbolId, int limit) {
        return this.repository.findBySymbols_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending()))
                .getContent();
    }

    @Transactional(rollbackOn = {IllegalStateException.class, ClientException.class})
    public List<News> generateEmbeddingsByNewsId(List<Long> ids) throws IllegalStateException, ClientException {
        List<News> news = this.repository.findAllByIdIn(ids);
        if (news.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.ENTITY_NOT_FOUND_ERROR, "news"));
        }
        NewsListener.setActive(false);
        return this.embeddingsClient.embed(news);
    }

    public List<News> createOrUpdate(List<News> entities, boolean triggerEntityCallback) {
        NewsListener.setActive(triggerEntityCallback);
        return this.createOrUpdate(entities);
    }

    private List<News> createOrUpdate(List<News> entities) {
        return this.delegate.createOrUpdate(repository,
                entity -> this.repository.findByExternalId(entity.getExternalId()),
                (oldEntity, newEntity) -> {
                    oldEntity.getSymbols().addAll(newEntity.getSymbols());
                    return oldEntity;
                },
                entities);
    }
}
