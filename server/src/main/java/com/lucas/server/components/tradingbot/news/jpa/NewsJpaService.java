package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

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
    public Optional<News> save(News entity) {
        return this.delegate.save(repository, entity);
    }

    @Override
    public List<News> saveAll(Iterable<News> entities) {
        return this.delegate.saveAllIgnoringDuplicates(repository, entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<News> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<News> findById(Long id) {
        return this.repository.findById(id);
    }

    public List<News> getTopForSymbolId(Long symbolId, int limit) {
        return this.repository.findBySymbol_Id(symbolId, PageRequest.of(0, limit, Sort.by("date").descending())).getContent();
    }

    public News generateEmbeddingsByNewsId(Long id) throws IllegalStateException, ClientException {
        Optional<News> news = this.findById(id);
        if (news.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Constants.ENTITY_NOT_FOUND_ERROR, "news"));
        }
        NewsListener.setActive(false);
        return this.save(this.embeddingsClient.embed(news.get())).orElseThrow();
    }
}
