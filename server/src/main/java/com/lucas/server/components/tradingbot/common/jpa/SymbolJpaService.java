package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.mapper.SymbolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SymbolJpaService implements JpaService<SymbolDomain> {

    private final GenericJpaServiceDelegate<Symbol, SymbolDomain, SymbolRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Symbol> uniqueConstraintDelegate;
    private final SymbolRepository repository;
    private final SymbolMapper symbolMapper;

    public SymbolJpaService(SymbolRepository repository, SymbolMapper symbolMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, symbolMapper);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.symbolMapper = symbolMapper;
    }

    @Transactional
    public Set<SymbolDomain> getOrCreateByName(Set<String> names) {
        Set<Symbol> entities = names.stream()
                .map(name -> new Symbol().setName(name).computeSector())
                .collect(Collectors.toSet());
        return uniqueConstraintDelegate.createOrUpdate(this::findUnique,
                        (oldEntity, newEntity) -> {
                            oldEntity.computeSector();
                            return oldEntity;
                        },
                        entities).stream()
                .map(symbolMapper::toDto)
                .collect(Collectors.toSet());
    }

    private Set<Symbol> findUnique(Set<Symbol> symbols) {
        return repository.findByNameIn(symbols.stream().map(Symbol::getName).collect(Collectors.toSet()));
    }

    @Transactional(readOnly = true)
    public Optional<SymbolDomain> findById(Long id) {
        return repository.findById(id).map(symbolMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Set<SymbolDomain> findAllById(Set<Long> symbolIds) {
        return repository.findAllById(symbolIds).stream()
                .map(symbolMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<SymbolDomain> saveAll(Set<SymbolDomain> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<SymbolDomain> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<SymbolDomain> elements) {
        delegate.deleteAll(elements);
    }
}
