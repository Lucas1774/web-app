package com.lucas.server.common.jpa;

import java.util.List;

public interface JpaService<E extends JpaEntity> {

    E save(E entity);

    List<E> saveAll(Iterable<E> entities);

    void deleteAll();

    List<E> findAll();
}
