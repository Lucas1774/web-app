package com.lucas.server.components.sudoku.service;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static com.lucas.server.common.Constants.*;
import static com.lucas.server.components.sudoku.jpa.Sudoku.withValues;

@Component
public class SudokuSolver {

    public boolean isSolved(Sudoku sudoku) {
        for (int value : sudoku.getState()) {
            if (value == 0) {
                return false;
            }
        }
        return true;
    }

    public void solve(Sudoku sudoku) {
        int maxRisk = 3;
        while (!isSolved(sudoku)) {
            doSolve(sudoku, maxRisk);
            maxRisk++;
        }
    }

    public boolean solveWithTimeout(Sudoku sudoku) {
        int maxRisk = 3;
        long now = System.nanoTime();
        while (!isSolved(sudoku) && System.nanoTime() - now < 100000000) {
            doSolve(sudoku, maxRisk);
            maxRisk++;
        }
        return isSolved(sudoku) && isSolvable(sudoku);
    }

    /**
     * Using a copy of the sudoku is more efficient than editing and rolling back
     * even trivially-placed numbers
     * Likewise, keeping count of digits attempted to place is slower than checking
     * all 9
     *
     * @param sudoku  sudoku
     * @param maxRisk forces the program to come back if filling a number doesn't
     *                clear others
     */
    private boolean doSolve(Sudoku sudoku, int maxRisk) {
        if (isSolved(sudoku)) {
            return true;
        }
        if (!isSolvable(sudoku)) {
            return false;
        }
        List<Integer> trivialCells = getTrivial(sudoku);
        if (!trivialCells.isEmpty()) {
            for (int trivialCell : trivialCells) {
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, trivialCell, digit)) {
                        sudoku.set(trivialCell, digit);
                        break;
                    }
                }
            }
            return doSolve(sudoku, maxRisk);
        }
        for (int i = 2; i <= SUDOKU_SIZE; i++) {
            int promisingCell = getNextPromisingCell(sudoku, i);
            if (-1 != promisingCell) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, promisingCell, digit)) {
                        Sudoku copy = withValues(sudoku.getState());
                        copy.set(promisingCell, digit);
                        count++;
                        if (doSolve(copy, maxRisk--)) {
                            sudoku.setState(copy.getState());
                            return true;
                        }
                        if (count == i || maxRisk == 0) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSolvable(Sudoku sudoku) {
        for (int place = 0; place < SUDOKU_NUMBER_OF_CELLS; place++) {
            if (0 == sudoku.getState()[place]) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, place, digit)) {
                        count++;
                        break;
                    }
                }
                if (count == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<Integer> getTrivial(Sudoku sudoku) {
        List<Integer> promisingCells = new ArrayList<>();
        for (int place = 0; place < SUDOKU_NUMBER_OF_CELLS; place++) {
            if (0 == sudoku.getState()[place]) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, place, digit)) {
                        count++;
                        if (count > 1) {
                            break;
                        }
                    }
                }
                if (count == 1) {
                    promisingCells.add(place);
                }
            }
        }
        return promisingCells;
    }

    private int getNextPromisingCell(Sudoku sudoku, int i) {
        for (int place = 0; place < SUDOKU_NUMBER_OF_CELLS; place++) {
            if (0 == sudoku.getState()[place]) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, place, digit)) {
                        count++;
                        if (count > i) {
                            break;
                        }
                    }
                }
                if (count == i) {
                    return place;
                }
            }
        }
        return -1;
    }

    /**
     * Check block acceptance only after checking row and column acceptance since it
     * is considerably slower
     */
    public boolean acceptsNumberInPlace(Sudoku sudoku, int place, int digit) {
        int rowIndexOffset = place / SUDOKU_SIZE * SUDOKU_SIZE;
        int columnIndex = place % SUDOKU_SIZE;
        for (int i = 0; i < SUDOKU_SIZE; i++) {
            if (sudoku.getState()[rowIndexOffset + i] == digit || sudoku.getState()[columnIndex + i * SUDOKU_SIZE] == digit) {
                return false;
            }
        }
        int blockFirstRow = place / (3 * SUDOKU_SIZE) * 3;
        int blockFirstColumn = columnIndex / 3 * 3;
        for (int i = 0; i < 3; i++) {
            int rowInBlockOffset = (blockFirstRow + i) * SUDOKU_SIZE;
            for (int j = 0; j < 3; j++) {
                if (sudoku.getState()[rowInBlockOffset + blockFirstColumn + j] == digit) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValid(Sudoku sudoku, int difficulty) {
        long clueCount = Arrays.stream(sudoku.getState())
                .filter(cell -> cell != 0)
                .count();
        boolean hasEightOrMoreUniqueDigits = IntStream.rangeClosed(1, 9)
                .filter(digit -> Arrays.stream(sudoku.getState()).anyMatch(cell -> cell == digit))
                .count() >= 8;
        if (difficulty == -1) {
            return clueCount >= 17
                    && hasEightOrMoreUniqueDigits;
        } else {
            int expectedClues = 17 + ((9 - difficulty) * 6);
            return clueCount == expectedClues
                    && hasEightOrMoreUniqueDigits;
        }
    }
}
