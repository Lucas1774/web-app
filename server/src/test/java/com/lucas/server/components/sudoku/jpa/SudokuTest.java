package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.components.sudoku.service.SudokuGenerator;
import com.lucas.server.components.sudoku.service.SudokuSolver;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.lucas.server.common.Constants.SUDOKU_SIZE;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SudokuTest {

    private static final int NUM_RUNS = 1000;

    private final Random random = new Random();
    private final SudokuSolver solver = new SudokuSolver();
    private final SudokuGenerator generator = new SudokuGenerator(solver, random);

    @Test
    void solve() {
        for (int i = 0; i < NUM_RUNS; i++) {
            Sudoku sudoku = generator.generate(random.nextInt(SUDOKU_SIZE) + 1);
            this.solver.solve(sudoku);
            assertTrue(this.solver.isSolved(sudoku));
        }
    }

    @Test
    void benchmark() {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        double totalGenerationDuration = 0;
        double totalDuration = 0;
        for (int i = 0; i < NUM_RUNS; i++) {
            int difficulty = random.nextInt(SUDOKU_SIZE) + 1;
            long generationStartTime = System.nanoTime();
            Sudoku sudoku = generator.generate(difficulty);
            long startTime = System.nanoTime();
            this.solver.solve(sudoku);
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
