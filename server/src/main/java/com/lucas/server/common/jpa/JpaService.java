package com.lucas.server.common.jpa;

import java.util.Set;

/**
 * Generic CRUD service for domain elements of JPA entities.
 *
 * @param <D> element type
 */
public interface JpaService<D> {

    /**
     * Saves all elements
     *
     * @param elements elements
     * @return the saved elements
     */
    Set<D> saveAll(Set<D> elements);

    /**
     * @return all entities
     */
    Set<D> findAll();

    void deleteAll(Set<D> elements);
}
