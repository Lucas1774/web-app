package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.sudoku.mapper.SudokuAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity
@Table(name = "sudokus")
public class Sudoku implements JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 81)
    @Convert(converter = SudokuAttributeConverter.class)
    @EqualsAndHashCode.Include
    @ToString.Include
    private int[] state;

    public static Sudoku withValues(int[] values) {
        return new Sudoku().setState(values.clone());
    }
}
