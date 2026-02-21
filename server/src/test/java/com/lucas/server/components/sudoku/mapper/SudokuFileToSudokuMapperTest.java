package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.components.sudoku.jpa.Sudoku;
import com.lucas.utils.exception.MappingException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SudokuFileToSudokuMapperTest {

    private final StringToSudokuMapper stringMapper = new StringToSudokuMapper(new SudokuAttributeConverter());
    private final SudokuFileToSudokuMapper mapper = new SudokuFileToSudokuMapper(stringMapper);

    @Test
    void mapValidFile() throws MappingException {
        // given
        String content = "Grid 01\\r\\n003020600\\r\\n900305001\\r\\n001806400\\r\\n008102900\\r\\n700000008\\r\\n006708200\\r\\n002609500\\r\\n800203009\\r\\n005010300\\r\\nGrid 02\\r\\n200080300\\r\\n060070084\\r\\n030500209\\r\\n000105408\\r\\n000000000\\r\\n402706000\\r\\n301007040\\r\\n720040060\\r\\n004010003";

        // when
        Set<Sudoku> result = mapper.map(content);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void mapEmptyFile() throws MappingException {
        // when
        Set<Sudoku> result = mapper.map("");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void mapFileWithMalformedSudoku() throws MappingException {
        // given
        String content = "Grid 01\\r\\n00302060a\\r\\n900305001\\r\\n001806400\\r\\n008102900\\r\\n700000008\\r\\n006708200\\r\\n002609500\\r\\n800203009\\r\\n005010300";

        // when
        Set<Sudoku> result = mapper.map(content);

        // then
        assertThat(result).isEmpty();
    }
}
