package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LetterPairsJpaService implements JpaService<LetterPairs> {

    @Delegate
    private final GenericJpaServiceDelegate<LetterPairs, LetterPairsRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<LetterPairs> uniqueConstraintDelegate;
    private final LetterPairsRepository repository;

    public LetterPairsJpaService(LetterPairsRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public Optional<LetterPairs> findByLetterPair(String letterPair) {
        return repository.findByLetterPair(letterPair);
    }

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
}
