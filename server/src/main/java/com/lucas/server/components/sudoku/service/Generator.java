package com.lucas.server.components.sudoku.service;

import com.lucas.server.components.sudoku.Sudoku;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@Component
public class Generator {

    private final Random random;

    public Generator(Random random) {
        this.random = random;
    }

    public Sudoku generateDefault(int difficulty) {
        Sudoku sudoku = Sudoku.withDefaultValues();
        this.setDifficulty(sudoku, difficulty);
        return sudoku;
    }

    public Sudoku generate(int difficulty) {
        Sudoku sudoku = Sudoku.withZeros();
        this.doGenerate(sudoku);
        this.setDifficulty(sudoku, difficulty);
        return sudoku;
    }

    public boolean doGenerate(Sudoku sudoku) {
        List<Integer> digits = new ArrayList<>();
        for (int digit : Sudoku.DIGITS) {
            digits.add(digit);
        }
        for (int place = 0; place < Sudoku.NUMBER_OF_CELLS; place++) {
            if (0 == sudoku.get()[place]) {
                Collections.shuffle(digits, random);
                for (int digit : digits) {
                    if (sudoku.acceptsNumberInPlace(place, digit)) {
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
        int cellsToSetToZero = (Sudoku.NUMBER_OF_CELLS - (17 + ((9 - difficulty) * 6)));
        List<Integer> possibleCells = new ArrayList<>();
        for (int i = 0; i < Sudoku.NUMBER_OF_CELLS; i++) {
            possibleCells.add(i);
        }
        int[] digits = Sudoku.DIGITS.clone();
        for (int i = 0; i < digits.length - 1; i++) {
            int digit = digits[i];
            possibleCells.remove(possibleCells.get(IntStream.range(0, possibleCells.size())
                    .filter(cellIndex -> digit == sudoku.get()[possibleCells.get(cellIndex)])
                    .boxed()
                    .toList().get(random.nextInt(Sudoku.SIZE))));
        }
        for (int i = 0; i < cellsToSetToZero; i++) {
            int randomCellIndex = random.nextInt(possibleCells.size());
            sudoku.set(possibleCells.get(randomCellIndex), 0);
            possibleCells.remove(randomCellIndex);
        }
    }
}
