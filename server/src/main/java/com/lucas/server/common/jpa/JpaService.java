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
    List<E> createAll(List<E> entities);

    /**
     * Deletes all entities
     */
    void deleteAll();

    /**
     * @return all entities
     */
    List<E> findAll();
}
