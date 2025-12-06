package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Set<Symbol> getOrCreateByName(Set<String> names) {
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                (oldEntity, newEntity) -> oldEntity.computeSector(),
                names.stream()
                        .map(name -> new Symbol().setName(name).computeSector())
                        .collect(Collectors.toSet()));
    }

    private Set<Symbol> findUnique(Set<Symbol> symbols) {
        return repository.findByNameIn(symbols.stream().map(Symbol::getName).collect(Collectors.toSet()));
    }

    public Optional<Symbol> findById(Long id) {
        return repository.findById(id);
    }

    public Set<Symbol> findAllById(Set<Long> symbolIds) {
        return new HashSet<>(repository.findAllById(symbolIds));
    }
}
