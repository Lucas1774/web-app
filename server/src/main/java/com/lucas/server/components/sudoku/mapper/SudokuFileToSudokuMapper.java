package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.common.Constants;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class SudokuFileToSudokuMapper implements Mapper<String, List<Sudoku>> {

    private final StringToSudokuMapper sudokuMapper;
    private static final Logger logger = LoggerFactory.getLogger(SudokuFileToSudokuMapper.class);

    public SudokuFileToSudokuMapper(StringToSudokuMapper sudokuMapper) {
        this.sudokuMapper = sudokuMapper;
    }

    /**
     * Dumps a string representing sudoku into a list of sudoku
     * The string is expected to have ten lines per sudoku, with the first one's
     * content being irrelevant
     * It is necessary to escape two backslashes: one for regex one for String
     *
     * @param content the string to parse
     */
    @Override
    public List<Sudoku> map(String content) throws JsonProcessingException {
        List<Sudoku> sudoku = new ArrayList<>();
        String[] lines;
        try {
            lines = content.split("\\\\r\\\\n|\\\\r|\\\\n");
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "sudoku"), e);
        }
        String newRawData = "";
        for (int i = 1; i <= lines.length; i++) {
            if (0 == i % 10) {
                try {
                    sudoku.add(sudokuMapper.map(newRawData));
                } catch (JsonProcessingException e) {
                    logger.warn(Constants.SUDOKU_IGNORED_MALFORMED_JSON_WARN, newRawData, e);
                }
                newRawData = "";
            } else if (i != lines.length) {
                newRawData = newRawData.concat(lines[i]);
            }
        }

        return sudoku;
    }
}
