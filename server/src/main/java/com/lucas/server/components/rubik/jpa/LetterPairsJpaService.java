package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.rubik.dto.LetterPairsDomain;
import com.lucas.server.components.rubik.mapper.LetterPairsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LetterPairsJpaService
        extends GenericJpaServiceDelegate<LetterPairs, LetterPairsDomain, LetterPairsRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<LetterPairs> delegate;

    public LetterPairsJpaService(LetterPairsRepository repository, LetterPairsMapper mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional(readOnly = true)
    public Optional<LetterPairsDomain> findByLetterPair(String letterPair) {
        return repository.findByLetterPair(letterPair).map(mapper::toDto);
    }

    @Transactional
    public Set<LetterPairsDomain> createOrUpdate(Set<LetterPairsDomain> entities) {
        Set<LetterPairs> entitySet = entities.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return delegate.createOrUpdate(_ -> repository.findByLetterPairIn(entities.stream()
                        .map(LetterPairsDomain::getLetterPair)
                        .collect(Collectors.toUnmodifiableSet())),
                (existing, incoming) -> existing.setPerson(incoming.getPerson())
                        .setAction(incoming.getAction())
                        .setObject(incoming.getObject()),
                entitySet).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }
}
