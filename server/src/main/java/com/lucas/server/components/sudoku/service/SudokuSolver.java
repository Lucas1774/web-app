package com.lucas.server.components.sudoku.service;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static com.lucas.server.common.Constants.*;
import static com.lucas.server.components.sudoku.jpa.Sudoku.withValues;

@Component
public class SudokuSolver {

    public boolean isSolved(Sudoku sudoku) {
        for (int value : sudoku.getState()) {
            if (0 == value) {
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
        while (!isSolved(sudoku) && 100000000 > System.nanoTime() - now) {
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
        Set<Integer> trivialCells = getTrivial(sudoku);
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
        for (int i = 2; SUDOKU_SIZE >= i; i++) {
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
                        if (count == i || 0 == maxRisk) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSolvable(Sudoku sudoku) {
        for (int place = 0; SUDOKU_NUMBER_OF_CELLS > place; place++) {
            if (0 == sudoku.getState()[place]) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, place, digit)) {
                        count++;
                        break;
                    }
                }
                if (0 == count) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<Integer> getTrivial(Sudoku sudoku) {
        Set<Integer> promisingCells = new HashSet<>();
        for (int place = 0; SUDOKU_NUMBER_OF_CELLS > place; place++) {
            if (0 == sudoku.getState()[place]) {
                int count = 0;
                for (int digit : getDigits()) {
                    if (acceptsNumberInPlace(sudoku, place, digit)) {
                        count++;
                        if (1 < count) {
                            break;
                        }
                    }
                }
                if (1 == count) {
                    promisingCells.add(place);
                }
            }
        }
        return promisingCells;
    }

    private int getNextPromisingCell(Sudoku sudoku, int i) {
        for (int place = 0; SUDOKU_NUMBER_OF_CELLS > place; place++) {
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
        for (int i = 0; SUDOKU_SIZE > i; i++) {
            if (sudoku.getState()[rowIndexOffset + i] == digit || sudoku.getState()[columnIndex + i * SUDOKU_SIZE] == digit) {
                return false;
            }
        }
        int blockFirstRow = place / (3 * SUDOKU_SIZE) * 3;
        int blockFirstColumn = columnIndex / 3 * 3;
        for (int i = 0; 3 > i; i++) {
            int rowInBlockOffset = (blockFirstRow + i) * SUDOKU_SIZE;
            for (int j = 0; 3 > j; j++) {
                if (sudoku.getState()[rowInBlockOffset + blockFirstColumn + j] == digit) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValid(Sudoku sudoku, int difficulty) {
        long clueCount = Arrays.stream(sudoku.getState())
                .filter(cell -> 0 != cell)
                .count();
        boolean hasEightOrMoreUniqueDigits = 8 <= IntStream.rangeClosed(1, 9)
                .filter(digit -> Arrays.stream(sudoku.getState()).anyMatch(cell -> cell == digit))
                .count();
        if (-1 == difficulty) {
            return 17 <= clueCount && hasEightOrMoreUniqueDigits;
        } else {
            return hasEightOrMoreUniqueDigits && clueCount == 17 + ((9 - difficulty) * 6L);
        }
    }
}
