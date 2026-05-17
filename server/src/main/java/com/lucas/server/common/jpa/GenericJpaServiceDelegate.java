package com.lucas.server.common.jpa;

import com.lucas.server.common.mapper.EntityMapper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.stream.Collectors;

public class GenericJpaServiceDelegate<T extends JpaEntity, D, R extends JpaRepository<T, ?>> {

    private final R repository;
    private final EntityMapper<T, D> mapper;

    public GenericJpaServiceDelegate(R repository, EntityMapper<T, D> mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Set<D> saveAll(Set<D> dtos) {
        Set<T> entities = dtos.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toSet());
        return repository.saveAll(entities).stream()
                .map(mapper::toDto)
                .collect(Collectors.toSet());
    }

    public Set<D> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toSet());
    }

    public void deleteAll(Set<D> dtos) {
        Set<T> entities = dtos.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toSet());
        repository.deleteAllInBatch(entities);
    }
}
