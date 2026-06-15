package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Entity
@Table(name = "algorithm_mappings",
        indexes = @Index(columnList = "first_sticker, second_sticker"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"first_sticker", "second_sticker"}))
public class AlgorithmMapping implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "first_sticker", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer firstSticker;

    @Column(name = "second_sticker", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer secondSticker;

    @Column(name = "edge_algorithm", length = 100)
    private String edgeAlgorithm;

    @Column(name = "corner_algorithm", length = 100)
    private String cornerAlgorithm;

    @Column(name = "parity_algorithm", length = 100)
    private String parityAlgorithm;

    @Column(name = "edge_type", length = 10)
    private String edgeType;

    @Column(name = "edge_technique", length = 10)
    private String edgeTechnique;

    @Column(name = "corner_type", length = 10)
    private String cornerType;

    @Column(name = "corner_technique", length = 10)
    private String cornerTechnique;

    @Column(name = "parity_type", length = 10)
    private String parityType;

    @Column(name = "parity_technique", length = 10)
    private String parityTechnique;
}
