package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SymbolJpaService extends GenericJpaServiceDelegate<Symbol, SymbolDomain, SymbolRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<Symbol> delegate;

    public SymbolJpaService(SymbolRepository repository, EntityMapper<Symbol, SymbolDomain> mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional
    public Set<SymbolDomain> getOrCreateByName(Set<String> names) {
        Set<Symbol> entities =
                names.stream().map(name -> new Symbol().setName(name).computeSector()).collect(Collectors.toSet());
        return delegate.createOrUpdate(this::findUnique, (oldEntity, newEntity) -> {
            oldEntity.computeSector();
            return oldEntity;
        }, entities).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Optional<SymbolDomain> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Set<SymbolDomain> findAllById(Set<Long> symbolIds) {
        return repository.findAllById(symbolIds).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Symbol> findUnique(Set<Symbol> symbols) {
        return repository.findByNameIn(symbols.stream().map(Symbol::getName).collect(Collectors.toSet()));
    }
}
