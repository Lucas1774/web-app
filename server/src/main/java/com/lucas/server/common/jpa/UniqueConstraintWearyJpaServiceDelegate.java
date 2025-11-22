package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Delegate for JPA services with unique constraints.
 * Contains find-then-create utility methods.
 *
 * @param <T> entity type
 */
public class UniqueConstraintWearyJpaServiceDelegate<T extends JpaEntity> {

    private final JpaRepository<T, ?> repository;

    public UniqueConstraintWearyJpaServiceDelegate(JpaRepository<T, ?> repository) {
        this.repository = repository;
    }

    /**
     * @param existingFinder unique entity finder function. Usually findByPrimaryKey
     * @param entities       entities
     * @return the newly saved entities
     */
    public Set<T> createIgnoringDuplicates(UnaryOperator<Set<T>> existingFinder, Set<T> entities) {
        Set<T> existing = existingFinder.apply(entities);
        return new HashSet<>(repository.saveAll(entities.stream()
                .filter(e -> !existing.contains(e))
                .toList()));
    }

    /**
     * @param existingFinder  unique entity finder function. Usually findByPrimaryKey
     * @param existingUpdater entity updater function
     * @param entities        entities
     * @return the updated entities as well as the newly saved ones
     */
    public Set<T> createOrUpdate(UnaryOperator<Set<T>> existingFinder,
                                 BinaryOperator<T> existingUpdater, Set<T> entities) {
        Set<T> existing = existingFinder.apply(entities);
        Map<T, T> existingMap = existing.stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity()));
        return new HashSet<>(repository.saveAll(entities.stream()
                .map(incoming -> existingMap.containsKey(incoming)
                        ? existingUpdater.apply(existingMap.get(incoming), incoming)
                        : incoming)
                .toList()));
    }
}
