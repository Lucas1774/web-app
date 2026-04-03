package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "algorithm_mappings",
        indexes = @Index(columnList = "first_sticker, second_sticker"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"first_sticker", "second_sticker"}))
public class AlgorithmMapping implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_sticker", nullable = false)
    private Integer firstSticker;

    @Column(name = "second_sticker", nullable = false)
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

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        AlgorithmMapping that = (AlgorithmMapping) o;
        return Objects.equals(firstSticker, that.firstSticker) && Objects.equals(secondSticker, that.secondSticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstSticker, secondSticker);
    }
}
