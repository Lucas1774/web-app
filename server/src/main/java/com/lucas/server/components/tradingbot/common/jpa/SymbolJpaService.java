package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Collection;
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

    public List<Symbol> getOrCreateByName(Collection<String> names) {
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity,
                names.stream().map(name -> new Symbol().setName(name)).toList());
    }

    public Optional<Symbol> findById(Long id) {
        return repository.findById(id);
    }

    private Collection<Symbol> findUnique(Collection<Symbol> symbols) {
        return repository.findByNameIn(symbols.stream().map(Symbol::getName).toList());
    }

    public List<Symbol> findAllById(List<Long> symbolIds) {
        return repository.findAllById(symbolIds);
    }
}
