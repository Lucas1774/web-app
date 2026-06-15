package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.components.sudoku.dto.SudokuDomain;
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
        String content = "Grid 01\\r\\n" + "003020600\\r\\n900305001\\r\\n001806400\\r\\n"
                         + "008102900\\r\\n700000008\\r\\n006708200\\r\\n"
                         + "002609500\\r\\n800203009\\r\\n005010300\\r\\n" + "Grid 02\\r\\n"
                         + "200080300\\r\\n060070084\\r\\n030500209\\r\\n"
                         + "000105408\\r\\n000000000\\r\\n402706000\\r\\n" + "301007040\\r\\n720040060\\r\\n004010003";

        // when
        Set<SudokuDomain> result = mapper.map(content);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void mapEmptyFile() throws MappingException {
        // when
        Set<SudokuDomain> result = mapper.map("");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void mapFileWithMalformedSudoku() throws MappingException {
        // given
        String content = "Grid 01\\r\\n" + "00302060a\\r\\n900305001\\r\\n001806400\\r\\n"
                         + "008102900\\r\\n700000008\\r\\n006708200\\r\\n" + "002609500\\r\\n800203009\\r\\n005010300";

        // when
        Set<SudokuDomain> result = mapper.map(content);

        // then
        assertThat(result).isEmpty();
    }
}
