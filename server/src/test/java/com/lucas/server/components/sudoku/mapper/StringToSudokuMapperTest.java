package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import com.lucas.server.components.sudoku.service.SudokuSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StringToSudokuMapperTest {

    private final StringToSudokuMapper mapper = new StringToSudokuMapper(new SudokuAttributeConverter());
    private final SudokuSolver solver = new SudokuSolver();

    @ParameterizedTest
    @MethodSource("getSudoku")
    void testMap(String value, boolean serializable, boolean isSolvable, boolean isActuallySolvable) throws JsonProcessingException {
        if (!serializable) {
            assertThatThrownBy(() -> mapper.map(value)).isInstanceOf(JsonProcessingException.class);
        } else {
            Sudoku sudoku = mapper.map(value);
            assertEquals(isSolvable, this.solver.isValid(sudoku, -1));
            if (isSolvable) {
                assertEquals(isActuallySolvable, this.solver.solveWithTimeout(sudoku));
            }
        }
    }

    private static Stream<Arguments> getSudoku() {
        return Stream.of(
                Arguments.of("630000000000500008005674000000020000003401020000000345000007004080300902947100080",
                        true, true, true), // valid
                Arguments.of("53007000060019500009800006080006000340080300170002000606000028000041900500008007a",
                        false, false, false), // letters
                Arguments.of("00000000000000000000000000000000000000000000000000000000000000000000000000000000-1",
                        false, false, false), // "negative numbers"
                Arguments.of("0000000000000000000000000000000000000000000000000000000000000000000000000000000-1",
                        false, false, false), // "negative numbers"
                Arguments.of("53007000060019500009800006080006000340080300170002000606000028000041900500008007",
                        false, false, false), // too little numbers
                Arguments.of("5300700006001950000980000608000600034008030017000200060600002800004190050000800799",
                        false, false, false), // too many numbers
                Arguments.of("000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                        true, false, false), // less than 17 clues
                Arguments.of("000000000000000000000000004000109003000080200000000000450070600008000507960050000",
                        true, false, false), // 16 clues
                Arguments.of("000000000010000000000000004000109003000080200000000000450070600008000507960050000",
                        true, true, true), // 17 clues
                Arguments.of("504000000010000000000000004000109000000080000000000000450070600008000507960050000",
                        true, false, false), // less than 8 unique digits
                Arguments.of("023456789123456789123456789123456789123456789123456789123456789123456789123456789",
                        true, true, false), // unsolvable
                Arguments.of("111111111111111111111111111111111111111111111111111111111111111111111111111111110",
                        true, false, false) // unsolvable
        );
    }
}
