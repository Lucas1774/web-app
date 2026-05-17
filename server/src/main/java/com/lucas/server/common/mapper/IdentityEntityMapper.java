package com.lucas.server.common.mapper;

import org.springframework.stereotype.Component;

/**
 * Identity mapper that performs no transformation.
 * <p>
 * Use this mapper in JPA services for entities that don't have a separate domain model/DTO yet.
 * The entity type is used as both the entity and DTO type in the generic service.
 * <p>
 *
 * @param <T> the entity type (also used as the DTO type)
 */
@Component
public class IdentityEntityMapper<T> implements EntityMapper<T, T> {

    @Override
    public T toDto(T entity) {
        return entity;
    }

    @Override
    public T toEntity(T dto) {
        return dto;
    }
}
