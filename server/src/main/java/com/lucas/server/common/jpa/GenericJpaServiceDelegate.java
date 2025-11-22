package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.Set;

public class GenericJpaServiceDelegate<T extends JpaEntity, R extends JpaRepository<T, ?>> {

    private final R repository;

    public GenericJpaServiceDelegate(R repository) {
        this.repository = repository;
    }

    public Set<T> createAll(Set<T> entities) {
        return new HashSet<>(repository.saveAll(entities));
    }

    public Set<T> findAll() {
        return new HashSet<>(repository.findAll());
    }

    public void deleteAll(Set<T> entities) {
        repository.deleteAllInBatch(entities);
    }
}
