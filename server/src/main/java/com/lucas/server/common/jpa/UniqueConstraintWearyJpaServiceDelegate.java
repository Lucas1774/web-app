package com.lucas.server.common.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    public List<T> createIgnoringDuplicates(UnaryOperator<Collection<T>> existingFinder, Collection<T> entities) {
        Collection<T> existing = existingFinder.apply(entities);
        return repository.saveAll(entities.stream()
                .filter(e -> !existing.contains(e))
                .toList());
    }

    /**
     * @param existingFinder  unique entity finder function. Usually findByPrimaryKey
     * @param existingUpdater entity updater function
     * @param entities        entities
     * @return the updated entities as well as the newly saved ones
     */
    public List<T> createOrUpdate(UnaryOperator<Collection<T>> existingFinder,
                                  BinaryOperator<T> existingUpdater, Collection<T> entities) {
        Collection<T> existing = existingFinder.apply(entities);
        Map<T, T> existingMap = existing.stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity()));
        return repository.saveAll(entities.stream()
                .map(incoming -> existingMap.containsKey(incoming)
                        ? existingUpdater.apply(existingMap.get(incoming), incoming)
                        : incoming)
                .toList());
    }
}
