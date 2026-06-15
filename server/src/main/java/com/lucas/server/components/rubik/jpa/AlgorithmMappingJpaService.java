package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.Constants.AlgorithmKind;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.rubik.dto.AlgorithmMappingDomain;
import com.lucas.server.components.rubik.mapper.AlgorithmMappingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlgorithmMappingJpaService
        extends GenericJpaServiceDelegate<AlgorithmMapping, AlgorithmMappingDomain, AlgorithmMappingRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<AlgorithmMapping> delegate;

    public AlgorithmMappingJpaService(AlgorithmMappingRepository repository, AlgorithmMappingMapper mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional(readOnly = true)
    public Optional<AlgorithmMappingDomain> findByStickers(int firstSticker, int secondSticker, AlgorithmKind kind) {
        return repository.findByFirstStickerAndSecondSticker(firstSticker, secondSticker).filter(m -> switch (kind) {
            case EDGE -> null != m.getEdgeAlgorithm();
            case CORNER -> null != m.getCornerAlgorithm();
            case PARITY -> null != m.getParityAlgorithm();
        }).map(mapper::toDto);
    }

    @Transactional
    public Set<AlgorithmMappingDomain> createOrUpdate(Set<AlgorithmMappingDomain> dtos) {
        Set<AlgorithmMapping> entitySet = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        Set<Integer> firstStickers =
                entitySet.stream().map(AlgorithmMapping::getFirstSticker).collect(Collectors.toUnmodifiableSet());
        Set<Integer> secondStickers =
                entitySet.stream().map(AlgorithmMapping::getSecondSticker).collect(Collectors.toUnmodifiableSet());
        return delegate.createOrUpdate(all -> repository.findByFirstStickerInAndSecondStickerIn(
                                firstStickers,
                                secondStickers)
                        .stream()
                        .filter(existing -> all.stream()
                                .anyMatch(e -> Objects.equals(e.getFirstSticker(), existing.getFirstSticker())
                                               && Objects.equals(e.getSecondSticker(), existing.getSecondSticker())))
                        .collect(Collectors.toSet()),
                (existing, incoming) -> existing.setEdgeAlgorithm(incoming.getEdgeAlgorithm())
                        .setCornerAlgorithm(incoming.getCornerAlgorithm())
                        .setParityAlgorithm(incoming.getParityAlgorithm())
                        .setEdgeType(incoming.getEdgeType())
                        .setEdgeTechnique(incoming.getEdgeTechnique())
                        .setCornerType(incoming.getCornerType())
                        .setCornerTechnique(incoming.getCornerTechnique())
                        .setParityType(incoming.getParityType())
                        .setParityTechnique(incoming.getParityTechnique()),
                entitySet).stream().map(mapper::toDto).collect(Collectors.toUnmodifiableSet());
    }
}
