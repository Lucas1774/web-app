package com.lucas.server.components.sudoku.service;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static com.lucas.server.common.Constants.*;

@Component
public class SudokuGenerator {

    private final SudokuSolver solver;
    private final Random random;

    public SudokuGenerator(SudokuSolver solver, Random random) {
        this.solver = solver;
        this.random = random;
    }

    public Sudoku generateDefault(int difficulty) {
        Sudoku sudoku = Sudoku.withDefaultValues();
        setDifficulty(sudoku, difficulty);
        return sudoku;
    }

    public Sudoku generate(int difficulty) {
        Sudoku sudoku = Sudoku.withZeros();
        doGenerate(sudoku);
        setDifficulty(sudoku, difficulty);
        return sudoku;
    }

    public boolean doGenerate(Sudoku sudoku) {
        List<Integer> digits = new ArrayList<>();
        for (int digit : DIGITS) {
            digits.add(digit);
        }
        for (int place = 0; place < SUDOKU_NUMBER_OF_CELLS; place++) {
            if (0 == sudoku.getState()[place]) {
                Collections.shuffle(digits, random);
                for (int digit : digits) {
                    if (solver.acceptsNumberInPlace(sudoku, place, digit)) {
                        sudoku.set(place, digit);
                        if (doGenerate(sudoku)) {
                            return true;
                        }
                        sudoku.set(place, 0);
                    }
                }
                return false;
            }
        }
        return true;
    }

    private void setDifficulty(Sudoku sudoku, int difficulty) {
        int cellsToSetToZero = (SUDOKU_NUMBER_OF_CELLS - (17 + ((9 - difficulty) * 6)));
        List<Integer> possibleCells = new ArrayList<>();
        for (int i = 0; i < SUDOKU_NUMBER_OF_CELLS; i++) {
            possibleCells.add(i);
        }
        int[] digits = DIGITS.clone();
        for (int i = 0; i < digits.length - 1; i++) {
            int digit = digits[i];
            possibleCells.remove(possibleCells.get(IntStream.range(0, possibleCells.size())
                    .filter(cellIndex -> digit == sudoku.getState()[possibleCells.get(cellIndex)])
                    .boxed()
                    .toList().get(random.nextInt(SUDOKU_SIZE))));
        }
        for (int i = 0; i < cellsToSetToZero; i++) {
            int randomCellIndex = random.nextInt(possibleCells.size());
            sudoku.set(possibleCells.get(randomCellIndex), 0);
            possibleCells.remove(randomCellIndex);
        }
    }
}
