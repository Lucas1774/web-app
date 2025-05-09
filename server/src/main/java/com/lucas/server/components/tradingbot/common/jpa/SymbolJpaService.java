package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SymbolJpaService implements JpaService<Symbol> {

    private final SymbolRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<Symbol> delegate;

    public SymbolJpaService(SymbolRepository repository, UniqueConstraintWearyJpaServiceDelegate<Symbol> delegate) {
        this.repository = repository;
        this.delegate = delegate;
    }

    @Override
    public Optional<Symbol> save(Symbol entity) {
        return this.delegate.save(repository, entity);
    }

    @Override
    public List<Symbol> saveAll(Iterable<Symbol> entities) {
        return this.delegate.saveAllIgnoringDuplicates(repository, entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Symbol> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Symbol> findById(Long id) {
        return this.repository.findById(id);
    }

    public Optional<Symbol> findByName(String name) {
        return this.repository.findByName(name);
    }
}
