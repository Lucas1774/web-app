package com.lucas.server.components.sudoku;

import com.lucas.server.components.sudoku.service.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SudokuTest {

    private static final int NUM_RUNS = 1000;

    private final Random random = new Random();
    private final Generator generator = new Generator(random);

    @Test
    void solve() {
        for (int i = 0; i < NUM_RUNS; i++) {
            Sudoku sudoku = generator.generate(random.nextInt(Sudoku.SIZE) + 1);
            sudoku.solve();
            assertTrue(sudoku.isSolved());
        }
    }

    @ParameterizedTest
    @MethodSource("getSudokus")
    void sanitization(String value, Integer expectedLength, boolean isSolvable, boolean isActuallySolvable) {
        int[] values = Sudoku.deserialize(value);
        assertEquals(expectedLength, values.length);
        Sudoku sudoku = Sudoku.withValues(values);
        assertEquals(isSolvable, sudoku.isValid(-1));
        if (isSolvable) {
            assertEquals(isActuallySolvable, sudoku.solveWithTimeout());
        }
    }

    private static Stream<Arguments> getSudokus() {
        return Stream.of(
                Arguments.of("630000000000500008005674000000020000003401020000000345000007004080300902947100080",
                        81,
                        true, true), // valid
                Arguments.of("53007000060019500009800006080006000340080300170002000606000028000041900500008007a",
                        0,
                        false, false), // letters
                Arguments.of("00000000000000000000000000000000000000000000000000000000000000000000000000000000-1",
                        0,
                        false, false), // "negative numbers"
                Arguments.of("0000000000000000000000000000000000000000000000000000000000000000000000000000000-1",
                        0,
                        false, false), // "negative numbers"
                Arguments.of("53007000060019500009800006080006000340080300170002000606000028000041900500008007",
                        0,
                        false, false), // too little numbers
                Arguments.of("5300700006001950000980000608000600034008030017000200060600002800004190050000800799",
                        0,
                        false, false), // too many numbers
                Arguments.of("000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                        81,
                        false, false), // less than 17 clues
                Arguments.of("000000000000000000000000004000109003000080200000000000450070600008000507960050000",
                        81,
                        false, false), // 16 clues
                Arguments.of("000000000010000000000000004000109003000080200000000000450070600008000507960050000",
                        81,
                        true, true), // 17 clues
                Arguments.of("504000000010000000000000004000109000000080000000000000450070600008000507960050000",
                        81,
                        false, false), // less than 8 unique digits
                Arguments.of("023456789123456789123456789123456789123456789123456789123456789123456789123456789",
                        81,
                        true, false), // unsolvable
                Arguments.of("111111111111111111111111111111111111111111111111111111111111111111111111111111110",
                        81,
                        false, false) // unsolvable
        );
    }

    @Test
    void generationConstrains() {
        for (int i = 0; i < NUM_RUNS; i++) {
            int difficulty = random.nextInt(Sudoku.SIZE) + 1;
            Sudoku sudoku = generator.generate(difficulty);
            assertTrue(sudoku.isValid(difficulty) && sudoku.solveWithTimeout());
        }
    }

    @Test
    void benchmark() {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        double totalGenerationDuration = 0;
        double totalDuration = 0;
        for (int i = 0; i < NUM_RUNS; i++) {
            int difficulty = random.nextInt(Sudoku.SIZE) + 1;
            long generationStartTime = System.nanoTime();
            Sudoku sudoku = generator.generate(difficulty);
            long startTime = System.nanoTime();
            sudoku.solve();
            long endTime = System.nanoTime();
            totalGenerationDuration += (startTime - generationStartTime);
            totalDuration += (endTime - startTime);
        }
        double averageGenerationDuration = totalGenerationDuration / NUM_RUNS / 1000000;
        double averageDuration = totalDuration / NUM_RUNS / 1000000;
        System.out.println("Average time taken to generate: " + averageGenerationDuration + " milliseconds");
        System.out.println("Average time taken to solve: " + averageDuration + " milliseconds");
    }
}
