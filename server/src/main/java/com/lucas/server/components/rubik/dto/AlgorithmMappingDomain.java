package com.lucas.server.components.rubik.dto;

import com.lucas.server.common.dto.DomainEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class AlgorithmMappingDomain implements DomainEntity {
    @ToString.Include
    private Long id;
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer firstSticker;
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer secondSticker;
    private String edgeAlgorithm;
    private String cornerAlgorithm;
    private String parityAlgorithm;
    private String edgeType;
    private String edgeTechnique;
    private String cornerType;
    private String cornerTechnique;
    private String parityType;
    private String parityTechnique;
}
