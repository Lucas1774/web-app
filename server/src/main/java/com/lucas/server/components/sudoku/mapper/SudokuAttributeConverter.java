package com.lucas.server.components.sudoku.mapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;

@Converter
@Component
public class SudokuAttributeConverter implements AttributeConverter<int[], String> {

    @Override
    public String convertToDatabaseColumn(int[] ints) {
        StringBuilder sb = new StringBuilder();
        for (int value : ints) {
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public int[] convertToEntityAttribute(String sudoku) {
        int[] rawData = new int[SUDOKU_NUMBER_OF_CELLS];
        for (int i = 0; i < sudoku.length(); i++) {
            char c = sudoku.charAt(i);
            if (c < '0' || c > '9') {
                throw new NumberFormatException();
            }
            rawData[i] = Character.getNumericValue(sudoku.charAt(i));
        }
        return rawData;
    }
}
