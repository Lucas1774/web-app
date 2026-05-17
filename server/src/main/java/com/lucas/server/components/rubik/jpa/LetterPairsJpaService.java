package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LetterPairsJpaService implements JpaService<LetterPairs> {

    private final GenericJpaServiceDelegate<LetterPairs, LetterPairs, LetterPairsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<LetterPairs> uniqueConstraintDelegate;
    private final LetterPairsRepository repository;

    public LetterPairsJpaService(LetterPairsRepository repository, IdentityEntityMapper<LetterPairs> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<LetterPairs> findByLetterPair(String letterPair) {
        return repository.findByLetterPair(letterPair);
    }

    @Transactional
    public Set<LetterPairs> createOrUpdate(Set<LetterPairs> entities) {
        return uniqueConstraintDelegate.createOrUpdate(
                all -> repository.findByLetterPairIn(
                        entities.stream().map(LetterPairs::getLetterPair).collect(Collectors.toSet())
                ),
                (existing, incoming) -> existing
                        .setPerson(incoming.getPerson())
                        .setAction(incoming.getAction())
                        .setObject(incoming.getObject()),
                entities);
    }

    @Override
    @Transactional
    public Set<LetterPairs> saveAll(Set<LetterPairs> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<LetterPairs> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<LetterPairs> elements) {
        delegate.deleteAll(elements);
    }
}
