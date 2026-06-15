package com.lucas.server.components.sudoku.dto;

import com.lucas.server.common.dto.DomainEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;
import static com.lucas.server.common.Constants.SUDOKU_SIZE;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class SudokuDomain implements DomainEntity {

    private static final int[] defaultData = new int[SUDOKU_NUMBER_OF_CELLS];

    static {
        for (int i = 0; SUDOKU_NUMBER_OF_CELLS > i; i++) {
            defaultData[i] = ((i % SUDOKU_SIZE) + 3 * (i / SUDOKU_SIZE) + (i / 27)) % SUDOKU_SIZE + 1;
        }
    }

    private Long id;

    @EqualsAndHashCode.Include
    private int[] state;

    public static SudokuDomain withValues(int[] values) {
        return new SudokuDomain().setState(values.clone());
    }

    public static SudokuDomain withZeros() {
        return new SudokuDomain().setState(new int[SUDOKU_NUMBER_OF_CELLS]);
    }

    public static SudokuDomain withDefaultValues() {
        return new SudokuDomain().setState(defaultData.clone());
    }

    public SudokuDomain set(int place, int digit) {
        this.state[place] = digit;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; SUDOKU_SIZE > i; i++) {
            if (0 == i % 3) {
                sb.append("+-------+-------+-------+\n");
            }
            for (int j = 0; SUDOKU_SIZE > j; j++) {
                if (0 == j % 3) {
                    sb.append("| ");
                }
                sb.append(state[i * SUDOKU_SIZE + j]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("+-------+-------+-------+\n");
        return sb.toString();
    }
}
