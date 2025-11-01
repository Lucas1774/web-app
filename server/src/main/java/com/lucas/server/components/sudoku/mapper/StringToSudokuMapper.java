package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;

@Component
public class StringToSudokuMapper implements Mapper<String, Sudoku> {

    private final SudokuAttributeConverter attributeConverter;

    public StringToSudokuMapper(SudokuAttributeConverter attributeConverter) {
        this.attributeConverter = attributeConverter;
    }

    @Override
    public Sudoku map(String sudoku) throws MappingException {
        try {
            if (SUDOKU_NUMBER_OF_CELLS != sudoku.length()) {
                throw new NumberFormatException();
            }
            return Sudoku.withValues(attributeConverter.convertToEntityAttribute(sudoku));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, "sudoku"), e);
        }
    }
}
