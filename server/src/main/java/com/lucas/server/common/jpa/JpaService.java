package com.lucas.server.common.jpa;

import java.util.List;

/**
 * Generic CRUD service for JPA entities.
 *
 * @param <E> entity type
 */
public interface JpaService<E extends JpaEntity> {

    /**
     * Saves all entities
     *
     * @param entities entities
     * @return the saved entities
     */
    @SuppressWarnings("unused")
    List<E> createAll(List<E> entities);

    /**
     * @return all entities
     */
    @SuppressWarnings("unused")
    List<E> findAll();

    @SuppressWarnings("unused")
    void deleteAll(List<E> entities);
}
