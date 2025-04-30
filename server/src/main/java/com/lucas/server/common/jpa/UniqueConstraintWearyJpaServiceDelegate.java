package com.lucas.server.common.jpa;

import com.lucas.server.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
public class UniqueConstraintWearyJpaServiceDelegate<T extends JpaEntity> {

    private static final Logger logger = LoggerFactory.getLogger(UniqueConstraintWearyJpaServiceDelegate.class);

    public Optional<T> save(JpaRepository<T, ?> repository, T entity) {
        try {
            return Optional.of(repository.save(entity));
        } catch (DataIntegrityViolationException e) {
            logger.warn(Constants.RECORD_IGNORED_BREAKS_UNIQUENESS_CONSTRAIN_WARN, entity, e);
            return Optional.empty();
        }
    }

    public List<T> saveAllIgnoringDuplicates(JpaRepository<T, ?> repository, Iterable<T> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(entity -> this.save(repository, entity))
                .flatMap(Optional::stream)
                .toList();
    }
}
