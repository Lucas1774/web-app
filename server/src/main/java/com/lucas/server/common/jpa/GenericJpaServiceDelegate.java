package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public class GenericJpaServiceDelegate<T extends JpaEntity, R extends JpaRepository<T, ?>> {

    private final R repository;

    public GenericJpaServiceDelegate(R repository) {
        this.repository = repository;
    }

    public List<T> createAll(List<T> entities) {
        return repository.saveAll(entities);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public List<T> findAll() {
        return repository.findAll();
    }
}
