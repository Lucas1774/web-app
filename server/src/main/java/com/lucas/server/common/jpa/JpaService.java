package com.lucas.server.common.jpa;

import java.util.List;
import java.util.Optional;

public interface JpaService<E extends JpaEntity> {

    Optional<E> save(E entity);

    List<E> saveAll(Iterable<E> entities);

    void deleteAll();

    List<E> findAll();
}
