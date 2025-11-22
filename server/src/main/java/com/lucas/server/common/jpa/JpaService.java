package com.lucas.server.common.jpa;

import java.util.Set;

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
    Set<E> createAll(Set<E> entities);

    /**
     * @return all entities
     */
    @SuppressWarnings("unused")
    Set<E> findAll();

    @SuppressWarnings("unused")
    void deleteAll(Set<E> entities);
}
