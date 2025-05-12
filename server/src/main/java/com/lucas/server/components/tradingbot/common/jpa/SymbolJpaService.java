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
    public List<Symbol> createAll(List<Symbol> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Symbol> findAll() {
        return repository.findAll();
    }

    public Symbol getOrCreateByName(String name) {
        return this.delegate.getOrCreate(repository,
                entity -> this.findByName(entity.getName()),
                new Symbol().setName(name));
    }

    public Optional<Symbol> findByName(String name) {
        return this.repository.findByName(name);
    }
}
