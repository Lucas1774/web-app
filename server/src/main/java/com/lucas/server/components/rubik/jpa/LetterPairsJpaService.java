package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LetterPairsJpaService extends GenericJpaServiceDelegate<LetterPairs, LetterPairs, LetterPairsRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<LetterPairs> delegate;

    public LetterPairsJpaService(LetterPairsRepository repository, EntityMapper<LetterPairs, LetterPairs> mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional(readOnly = true)
    public Optional<LetterPairs> findByLetterPair(String letterPair) {
        return repository.findByLetterPair(letterPair);
    }

    @Transactional
    public Set<LetterPairs> createOrUpdate(Set<LetterPairs> entities) {
        return delegate.createOrUpdate(all -> repository.findByLetterPairIn(entities.stream()
                        .map(LetterPairs::getLetterPair)
                        .collect(Collectors.toUnmodifiableSet())),
                (existing, incoming) -> existing.setPerson(incoming.getPerson())
                        .setAction(incoming.getAction())
                        .setObject(incoming.getObject()),
                entities);
    }
}
