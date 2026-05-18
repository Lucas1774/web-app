package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.Constants.AlgorithmKind;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlgorithmMappingJpaService extends GenericJpaServiceDelegate<AlgorithmMapping, AlgorithmMapping, AlgorithmMappingRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<AlgorithmMapping> delegate;

    public AlgorithmMappingJpaService(AlgorithmMappingRepository repository, EntityMapper<AlgorithmMapping, AlgorithmMapping> mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional(readOnly = true)
    public Optional<AlgorithmMapping> findByStickers(int firstSticker, int secondSticker, AlgorithmKind kind) {
        return repository.findByFirstStickerAndSecondSticker(firstSticker, secondSticker)
                .filter(m -> switch (kind) {
                    case EDGE -> null != m.getEdgeAlgorithm();
                    case CORNER -> null != m.getCornerAlgorithm();
                    case PARITY -> null != m.getParityAlgorithm();
                });
    }

    @Transactional
    public Set<AlgorithmMapping> createOrUpdate(Set<AlgorithmMapping> entities) {
        Set<Integer> firstStickers = entities.stream().map(AlgorithmMapping::getFirstSticker).collect(Collectors.toSet());
        Set<Integer> secondStickers = entities.stream().map(AlgorithmMapping::getSecondSticker).collect(Collectors.toSet());
        return delegate.createOrUpdate(
                all -> repository.findByFirstStickerInAndSecondStickerIn(firstStickers, secondStickers).stream()
                        .filter(existing -> all.stream().anyMatch(e ->
                                Objects.equals(e.getFirstSticker(), existing.getFirstSticker()) &&
                                        Objects.equals(e.getSecondSticker(), existing.getSecondSticker())))
                        .collect(Collectors.toSet()),
                (existing, incoming) -> existing
                        .setEdgeAlgorithm(incoming.getEdgeAlgorithm())
                        .setCornerAlgorithm(incoming.getCornerAlgorithm())
                        .setParityAlgorithm(incoming.getParityAlgorithm())
                        .setEdgeType(incoming.getEdgeType())
                        .setEdgeTechnique(incoming.getEdgeTechnique())
                        .setCornerType(incoming.getCornerType())
                        .setCornerTechnique(incoming.getCornerTechnique())
                        .setParityType(incoming.getParityType())
                        .setParityTechnique(incoming.getParityTechnique()),
                entities);
    }
}
