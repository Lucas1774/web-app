package com.lucas.server.components.rubik.jpa;

import com.lucas.server.common.Constants.AlgorithmKind;
import com.lucas.server.common.jpa.JpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

import static com.lucas.server.common.Constants.AlgorithmKind.*;
import static com.lucas.utils.Utils.EMPTY_STRING;

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

    @Column(name = "letter_pair", length = 10, nullable = false)
    private String letterPair;

    @Column(name = "person", length = 50)
    private String person;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "object", length = 50)
    private String object;

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

    public static AlgorithmMapping withKind(AlgorithmMapping m, AlgorithmKind kind) {
        return new AlgorithmMapping()
                .setId(m.getId())
                .setFirstSticker(m.getFirstSticker())
                .setSecondSticker(m.getSecondSticker())
                .setLetterPair(m.getLetterPair())
                .setPerson(m.getPerson())
                .setAction(m.getAction())
                .setObject(m.getObject())
                .setEdgeAlgorithm(EDGE == kind ? m.getEdgeAlgorithm() : null)
                .setCornerAlgorithm(CORNER == kind ? m.getCornerAlgorithm() : null)
                .setParityAlgorithm(PARITY == kind ? m.getParityAlgorithm() : null)
                .setEdgeType(EDGE == kind ? m.getEdgeType() : null)
                .setEdgeTechnique(EDGE == kind ? m.getEdgeTechnique() : null)
                .setCornerType(CORNER == kind ? m.getCornerType() : null)
                .setCornerTechnique(CORNER == kind ? m.getCornerTechnique() : null)
                .setParityType(PARITY == kind ? m.getParityType() : null)
                .setParityTechnique(PARITY == kind ? m.getParityTechnique() : null);
    }

    @Override
    public String toString() {
        if (null != edgeAlgorithm) {
            return edgeAlgorithm;
        }
        if (null != cornerAlgorithm) {
            return cornerAlgorithm;
        }
        if (null != parityAlgorithm) {
            return parityAlgorithm;
        }
        return EMPTY_STRING;
    }

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
