package com.lucas.server.components.sudoku.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lucas.server.common.jpa.JpaEntity;
import com.lucas.server.components.sudoku.mapper.SudokuAttributeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;
import static com.lucas.server.common.Constants.SUDOKU_SIZE;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "sudokus")
public class Sudoku implements JpaEntity {

    private Sudoku(int[] state) {
        this.state = state.clone();
    }

    public static Sudoku withValues(int[] values) {
        return new Sudoku(values);
    }

    public static Sudoku withZeros() {
        int[] zeros = new int[SUDOKU_NUMBER_OF_CELLS];
        return new Sudoku(zeros);
    }

    public static Sudoku withDefaultValues() {
        int[] rawData = new int[SUDOKU_NUMBER_OF_CELLS];
        for (int i = 0; i < SUDOKU_NUMBER_OF_CELLS; i++) {
            rawData[i] = ((i % SUDOKU_SIZE) + 3 * (i / SUDOKU_SIZE) + (i / 27)) % SUDOKU_SIZE + 1;
        }
        return new Sudoku(rawData);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = false, unique = true, length = 81)
    @Convert(converter = SudokuAttributeConverter.class)
    private int[] state;

    public void set(int place, int digit) {
        state[place] = digit;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SUDOKU_SIZE; i++) {
            if (i % 3 == 0)
                sb.append("+-------+-------+-------+\n");
            for (int j = 0; j < SUDOKU_SIZE; j++) {
                if (j % 3 == 0)
                    sb.append("| ");
                sb.append(state[i * SUDOKU_SIZE + j]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("+-------+-------+-------+\n");
        return sb.toString();
    }
}
