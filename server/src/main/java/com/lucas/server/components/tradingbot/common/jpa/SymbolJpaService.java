package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SymbolJpaService implements JpaService<Symbol> {

    @Delegate
    private final GenericJpaServiceDelegate<Symbol, SymbolRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Symbol> uniqueConstraintDelegate;
    private final SymbolRepository repository;

    public SymbolJpaService(SymbolRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public Symbol getOrCreateByName(String name) {
        return uniqueConstraintDelegate.getOrCreate(entity -> findByName(entity.getName()), new Symbol().setName(name));
    }

    public Optional<Symbol> findByName(String name) {
        return repository.findByName(name);
    }

    public List<Symbol> findAllById(List<Long> symbolIds) {
        return repository.findAllById(symbolIds);
    }
}
