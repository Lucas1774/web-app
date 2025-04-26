package com.lucas.server.components.sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Sudoku {

    public static final int SIZE = 9;
    public static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static final int NUMBER_OF_CELLS = 81;
    private int[] rawData;

    private Sudoku(int[] rawData) {
        this.rawData = rawData.clone();
    }

    public static Sudoku withValues(int[] values) {
        return new Sudoku(values);
    }

    public static Sudoku withZeros() {
        int[] zeros = new int[NUMBER_OF_CELLS];
        return new Sudoku(zeros);
    }

    public static Sudoku withDefaultValues() {
        int[] rawData = new int[NUMBER_OF_CELLS];
        for (int i = 0; i < NUMBER_OF_CELLS; i++) {
            rawData[i] = ((i % SIZE) + 3 * (i / SIZE) + (i / 27)) % SIZE + 1;
        }
        return new Sudoku(rawData);
    }

    public int[] get() {
        return this.rawData;
    }

    public void set(int place, int digit) {
        this.rawData[place] = digit;
    }

    public boolean isSolved() {
        for (int value : this.rawData) {
            if (value == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SIZE; i++) {
            if (i % 3 == 0)
                sb.append("+-------+-------+-------+\n");
            for (int j = 0; j < SIZE; j++) {
                if (j % 3 == 0)
                    sb.append("| ");
                sb.append(this.rawData[i * SIZE + j]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("+-------+-------+-------+\n");
        return sb.toString();
    }

    public void solve() {
        int maxRisk = 3;
        while (!this.isSolved()) {
            this.doSolve(maxRisk);
            maxRisk++;
        }
    }

    public boolean solveWithTimeout() {
        int maxRisk = 3;
        long now = System.nanoTime();
        while (!this.isSolved() && System.nanoTime() - now < 100000000) {
            this.doSolve(maxRisk);
            maxRisk++;
        }
        return this.isSolved() && this.isSolvable();
    }

    /**
     * Using a copy of the sudoku is more efficient than editing and rolling back
     * even trivially-placed numbers
     * Likewise, keeping count of digits attempted to place is slower than checking
     * all 9
     *
     * @param maxRisk forces the program to come back if filling a number doesn't
     *                clear others
     */
    private boolean doSolve(int maxRisk) {
        if (this.isSolved()) {
            return true;
        }
        if (!this.isSolvable()) {
            return false;
        }
        List<Integer> trivialCells = this.getTrivial();
        if (!trivialCells.isEmpty()) {
            for (int trivialCell : trivialCells) {
                for (int digit : DIGITS) {
                    if (this.acceptsNumberInPlace(trivialCell, digit)) {
                        this.set(trivialCell, digit);
                        break;
                    }
                }
            }
            return this.doSolve(maxRisk);
        }
        for (int i = 2; i <= SIZE; i++) {
            int promisingCell = this.getNextPromisingCell(i);
            if (-1 != promisingCell) {
                int count = 0;
                for (int digit : DIGITS) {
                    if (this.acceptsNumberInPlace(promisingCell, digit)) {
                        Sudoku sudoku = withValues(this.rawData);
                        sudoku.set(promisingCell, digit);
                        count++;
                        if (sudoku.doSolve(maxRisk--)) {
                            this.rawData = sudoku.rawData;
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

    private boolean isSolvable() {
        for (int place = 0; place < NUMBER_OF_CELLS; place++) {
            if (0 == this.rawData[place]) {
                int count = 0;
                for (int digit : DIGITS) {
                    if (this.acceptsNumberInPlace(place, digit)) {
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

    private List<Integer> getTrivial() {
        List<Integer> promisingCells = new ArrayList<>();
        for (int place = 0; place < NUMBER_OF_CELLS; place++) {
            if (0 == this.rawData[place]) {
                int count = 0;
                for (int digit : DIGITS) {
                    if (this.acceptsNumberInPlace(place, digit)) {
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

    private int getNextPromisingCell(int i) {
        for (int place = 0; place < NUMBER_OF_CELLS; place++) {
            if (0 == this.rawData[place]) {
                int count = 0;
                for (int digit : DIGITS) {
                    if (this.acceptsNumberInPlace(place, digit)) {
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
    public boolean acceptsNumberInPlace(int place, int digit) {
        int rowIndexOffset = place / SIZE * SIZE;
        int columnIndex = place % SIZE;
        for (int i = 0; i < SIZE; i++) {
            if (this.rawData[rowIndexOffset + i] == digit || this.rawData[columnIndex + i * SIZE] == digit) {
                return false;
            }
        }
        int blockFirstRow = place / (3 * SIZE) * 3;
        int blockFirstColumn = columnIndex / 3 * 3;
        for (int i = 0; i < 3; i++) {
            int rowInBlockOffset = (blockFirstRow + i) * SIZE;
            for (int j = 0; j < 3; j++) {
                if (this.rawData[rowInBlockOffset + blockFirstColumn + j] == digit) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValid(int difficulty) {
        long clueCount = Arrays.stream(this.get())
                .filter(cell -> cell != 0)
                .count();
        boolean hasEightOrMoreUniqueDigits = IntStream.rangeClosed(1, 9)
                .filter(digit -> Arrays.stream(this.get()).anyMatch(cell -> cell == digit))
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

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int value : this.rawData) {
            sb.append(value);
        }
        sb.append("\"");
        return sb.toString();
    }

    public static int[] deserialize(String sudoku) {
        if (sudoku.matches("\\d{81}")) {
            int[] rawData = new int[NUMBER_OF_CELLS];
            for (int i = 0; i < NUMBER_OF_CELLS; i++) {
                rawData[i] = Character.getNumericValue(sudoku.charAt(i));
            }
            return rawData;
        } else {
            return new int[0];
        }
    }
}
