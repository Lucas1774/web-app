package com.lucas.server.common.mapper;

/**
 * Generic mapper interface for converting between JPA entities and DTOs.
 *
 * @param <E> entity type (JPA entity)
 * @param <D> DTO type
 */
public interface EntityMapper<E, D> {

    /**
     * Converts a JPA entity to a DTO.
     *
     * @param entity the entity to convert
     * @return the DTO
     */
    D toDto(E entity);

    /**
     * Converts a DTO to a JPA entity.
     *
     * @param dto the DTO to convert
     * @return the entity
     */
    E toEntity(D dto);
}
