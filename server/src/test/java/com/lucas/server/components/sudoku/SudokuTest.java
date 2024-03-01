package com.lucas.server.components.sudoku;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class SudokuTest {
    @Test
    void testgetFromCell() {
        Sudoku sudoku = this.createFillAndPrintSudoku(1);
        int result = sudoku.getFromCell("block", 0, 0).getIndex();
        assertEquals(0, result);
        result = sudoku.getFromCell("block", 2, 3).getIndex();
        assertEquals(1, result);
        result = sudoku.getFromCell("block", 1, 7).getIndex();
        assertEquals(2, result);
        result = sudoku.getFromCell("block", 4, 2).getIndex();
        assertEquals(3, result);
        result = sudoku.getFromCell("block", 4, 3).getIndex();
        assertEquals(4, result);
        result = sudoku.getFromCell("block", 5, 8).getIndex();
        assertEquals(5, result);
        result = sudoku.getFromCell("block", 6, 2).getIndex();
        assertEquals(6, result);
        result = sudoku.getFromCell("block", 6, 5).getIndex();
        assertEquals(7, result);
        result = sudoku.getFromCell("block", 8, 8).getIndex();
        assertEquals(8, result);

    }

    @Test
    void testFillSudokuAndGetRulables() {
        Sudoku sudoku =  this.createFillAndPrintSudoku(1);
        Random random = new Random();
        int randomRow = random.nextInt(9);
        int randomCol = random.nextInt(9);
        System.out.println("Cell " + (randomRow + 1) + ", " + (randomCol + 1));
        System.out.println(sudoku.getFromCell("row", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("column", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("block", randomRow, randomCol));
        
        randomRow = random.nextInt(9);
        randomCol = random.nextInt(9);
        System.out.println("Cell " + (randomRow + 1) + ", " + (randomCol + 1));
        System.out.println(sudoku.getFromCell("row", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("column", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("block", randomRow, randomCol));
        
        randomCol = random.nextInt(9);
        randomRow = random.nextInt(9);
        System.out.println("Cell " + (randomRow + 1) + ", " + (randomCol + 1));
        System.out.println(sudoku.getFromCell("row", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("column", randomRow, randomCol));
        System.out.println(sudoku.getFromCell("block", randomRow, randomCol));
    }

    @Test
    void testPlaceNumber() {
        Sudoku sudoku = this.createFillAndPrintSudoku(0.7);
        boolean placed = false;
        int number = 0;
        for (int i = 1; i <= 9; i++) {
            number = i;
            placed = sudoku.acceptsNumber(number);
            if (placed) {
                break;
            }
        }
        if (placed) {
            System.out.println("Placed number: " + number);
        }
        System.out.println(sudoku);
    }

    @Test
    public void testSolve() {
        Sudoku sudoku = this.createFillAndPrintSudoku(0.3);
            sudoku.solve();
            System.out.println(sudoku);
    }

    private Sudoku createFillAndPrintSudoku(double chanceToFill) {
        Sudoku sudoku = new Sudoku();
        List<Integer> values = new ArrayList<Integer>();
        Random random = new Random();
        boolean fills = false;
        for (int i = 0; i < 81; i++) {
            fills = random.nextInt(100) + 1 <= chanceToFill * 100;
            values.add( fills ? random.nextInt(9) + 1 : 0);
        }
        sudoku.fill(values);
        System.out.print(sudoku);
        return sudoku;
    }
}