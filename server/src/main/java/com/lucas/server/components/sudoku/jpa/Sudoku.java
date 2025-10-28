package com.lucas.server.components.sudoku.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.sudoku.mapper.SudokuAttributeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Objects;

import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;
import static com.lucas.server.common.Constants.SUDOKU_SIZE;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "sudokus")
public class Sudoku implements JpaEntity {

    private static final int[] defaultData = new int[SUDOKU_NUMBER_OF_CELLS];

    static {
        for (int i = 0; SUDOKU_NUMBER_OF_CELLS > i; i++) {
            defaultData[i] = ((i % SUDOKU_SIZE) + 3 * (i / SUDOKU_SIZE) + (i / 27)) % SUDOKU_SIZE + 1;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    @Column(nullable = false, unique = true, length = 81)
    @Convert(converter = SudokuAttributeConverter.class)
    private int[] state;

    public static Sudoku withValues(int[] values) {
        return new Sudoku().setState(values.clone());
    }

    public static Sudoku withZeros() {
        return new Sudoku().setState(new int[SUDOKU_NUMBER_OF_CELLS]);
    }

    public static Sudoku withDefaultValues() {
        return new Sudoku().setState(defaultData.clone());
    }

    public void set(int place, int digit) {
        state[place] = digit;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; SUDOKU_SIZE > i; i++) {
            if (0 == i % 3)
                sb.append("+-------+-------+-------+\n");
            for (int j = 0; SUDOKU_SIZE > j; j++) {
                if (0 == j % 3)
                    sb.append("| ");
                sb.append(state[i * SUDOKU_SIZE + j]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("+-------+-------+-------+\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || getClass() != o.getClass()) return false;
        Sudoku sudoku = (Sudoku) o;
        return Objects.deepEquals(state, sudoku.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(state));
    }
}
