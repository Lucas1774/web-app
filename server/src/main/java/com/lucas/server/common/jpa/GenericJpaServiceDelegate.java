package com.lucas.server.common.jpa;

import com.lucas.server.common.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class GenericJpaServiceDelegate<T extends JpaEntity, D, R extends JpaRepository<T, ?>>
        implements JpaService<D> {

    protected final R repository;
    protected final EntityMapper<T, D> mapper;

    @Override
    @Transactional
    public Set<D> saveAll(Set<D> dtos) {
        Set<T> entities = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return repository.saveAll(entities).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<D> findAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void deleteAll(Set<D> dtos) {
        Set<T> entities = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        repository.deleteAllInBatch(entities);
    }
}
