package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Delegate for JPA services with unique constraints.
 * Contains find-then-create utility methods.
 *
 * @param <T> entity type
 */
@Component
public class UniqueConstraintWearyJpaServiceDelegate<T extends JpaEntity> {

    /**
     * @param repository repository
     * @param finder     unique entity finder function. Usually findByPrimaryKey
     * @param entity     entity
     * @return the existing entity or a new one
     */
    public T getOrCreate(JpaRepository<T, ?> repository, Function<T, Optional<T>> finder, T entity) {
        return finder.apply(entity).orElseGet(() -> repository.save(entity));
    }

    /**
     * @param repository     repository
     * @param existingFinder unique entity finder function. Usually findByPrimaryKey
     * @param entities       entities
     * @return the newly saved entities
     */
    public List<T> createIgnoringDuplicates(JpaRepository<T, ?> repository, Function<T, Optional<T>> existingFinder, Iterable<T> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(entity -> existingFinder.apply(entity).isEmpty())
                .map(repository::save)
                .toList();
    }

    /**
     * @param repository      repository
     * @param existingFinder  unique entity finder function. Usually findByPrimaryKey
     * @param existingUpdater entity updater function
     * @param entities        entities
     * @return the updated entities as well as the newly saved ones
     */
    public List<T> createOrUpdate(JpaRepository<T, ?> repository, Function<T, Optional<T>> existingFinder,
                                  BinaryOperator<T> existingUpdater, List<T> entities) {
        return entities.stream()
                .map(entity -> existingFinder.apply(entity)
                        .map(existing -> existingUpdater.apply(existing, entity))
                        .orElseGet(() -> repository.save(entity)))
                .toList();
    }
}
