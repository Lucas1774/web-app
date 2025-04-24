package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NewsJpaService implements JpaService<News> {

    private final NewsRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<NewsRepository, News> delegate;

    public NewsJpaService(NewsRepository repository, UniqueConstraintWearyJpaServiceDelegate<NewsRepository, News> delegate) {
        this.repository = repository;
        this.delegate = delegate;
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
    public Optional<News> findById(String id) {
        return this.repository.findById(Long.valueOf(id));
    }
}
