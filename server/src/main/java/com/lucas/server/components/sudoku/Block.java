package com.lucas.server.components.sudoku;

import java.util.List;

public class Block extends NineNumberPiece {

    public Block(List<Integer> rawData, int index) {
        super(rawData, index);
        for (int i = 0; i < Sudoku.NUMBER_OF_CELLS; i++) {
            int row = i / 9;
            int column = i % 9;
            int blockRow = row / 3;
            int blockColumn = column / 3;
            if (blockRow * 3 + blockColumn == index) {
                this.rawData.add(rawData.get(i));
            }
        }
    }
}