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
    @SuppressWarnings("unused")
    Set<D> saveAll(Set<D> elements);

    /**
     * @return all entities
     */
    @SuppressWarnings("unused")
    Set<D> findAll();

    /**
     * @param elements elements to delete
     */
    @SuppressWarnings("unused")
    void deleteAll(Set<D> elements);
}
