package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public class GenericJpaServiceDelegate<T extends JpaEntity, R extends JpaRepository<T, ?>> {

    private final R repository;

    public GenericJpaServiceDelegate(R repository) {
        this.repository = repository;
    }

    public List<T> createAll(List<T> entities) {
        return repository.saveAll(entities);
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public void deleteAll(Set<T> entities) {
        repository.deleteAllInBatch(entities);
    }
}
