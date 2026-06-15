package com.lucas.server.components.rubik.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.rubik.dto.AlgorithmMappingDomain;
import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import org.springframework.stereotype.Component;

@Component
public class AlgorithmMappingMapper implements EntityMapper<AlgorithmMapping, AlgorithmMappingDomain> {

    @Override
    public AlgorithmMappingDomain toDto(AlgorithmMapping entity) {
        if (null == entity) {
            return null;
        }
        return new AlgorithmMappingDomain(entity.getId(),
                entity.getFirstSticker(),
                entity.getSecondSticker(),
                entity.getEdgeAlgorithm(),
                entity.getCornerAlgorithm(),
                entity.getParityAlgorithm(),
                entity.getEdgeType(),
                entity.getEdgeTechnique(),
                entity.getCornerType(),
                entity.getCornerTechnique(),
                entity.getParityType(),
                entity.getParityTechnique());
    }

    @Override
    public AlgorithmMapping toEntity(AlgorithmMappingDomain dto) {
        if (null == dto) {
            return null;
        }
        return new AlgorithmMapping().setId(dto.getId())
                .setFirstSticker(dto.getFirstSticker())
                .setSecondSticker(dto.getSecondSticker())
                .setEdgeAlgorithm(dto.getEdgeAlgorithm())
                .setCornerAlgorithm(dto.getCornerAlgorithm())
                .setParityAlgorithm(dto.getParityAlgorithm())
                .setEdgeType(dto.getEdgeType())
                .setEdgeTechnique(dto.getEdgeTechnique())
                .setCornerType(dto.getCornerType())
                .setCornerTechnique(dto.getCornerTechnique())
                .setParityType(dto.getParityType())
                .setParityTechnique(dto.getParityTechnique());
    }
}
