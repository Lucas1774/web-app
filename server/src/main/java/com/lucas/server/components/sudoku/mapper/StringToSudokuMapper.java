package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

import static com.lucas.server.common.Constants.JSON_MAPPING_ERROR;
import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;

@Component
public class StringToSudokuMapper implements Mapper<String, Sudoku> {

    private final SudokuAttributeConverter attributeConverter;

    public StringToSudokuMapper(SudokuAttributeConverter attributeConverter) {
        this.attributeConverter = attributeConverter;
    }

    @Override
    public Sudoku map(String sudoku) throws JsonProcessingException {
        try {
            if (SUDOKU_NUMBER_OF_CELLS != sudoku.length()) {
                throw new NumberFormatException();
            }
            return Sudoku.withValues(attributeConverter.convertToEntityAttribute(sudoku));
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, "sudoku"), e);
        }
    }
}
