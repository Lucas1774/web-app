package com.lucas.server.components.sudoku.service;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.lucas.server.common.Constants.SUDOKU_SIZE;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SudokuGeneratorTest {

    private static final int NUM_RUNS = 1000;

    private final Random random = new Random();
    private final SudokuSolver solver = new SudokuSolver();
    private final SudokuGenerator generator = new SudokuGenerator(solver, random);


    @Test
    void generationConstraints() {
        for (int i = 0; NUM_RUNS > i; i++) {
            int difficulty = random.nextInt(SUDOKU_SIZE) + 1;
            Sudoku sudoku = generator.generate(difficulty);
            assertTrue(solver.isValid(sudoku, difficulty) && solver.solveWithTimeout(sudoku));
        }
    }
}
