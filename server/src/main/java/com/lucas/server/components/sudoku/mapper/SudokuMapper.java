package com.lucas.server.components.sudoku.mapper;

import com.lucas.server.common.mapper.EntityMapper;
import com.lucas.server.components.sudoku.dto.SudokuDomain;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import org.springframework.stereotype.Component;

@Component
public class SudokuMapper implements EntityMapper<Sudoku, SudokuDomain> {

    @Override
    public SudokuDomain toDto(Sudoku entity) {
        if (null == entity) {
            return null;
        }
        return SudokuDomain.withValues(entity.getState()).setId(entity.getId());
    }

    @Override
    public Sudoku toEntity(SudokuDomain dto) {
        if (null == dto) {
            return null;
        }
        return Sudoku.withValues(dto.getState()).setId(dto.getId());
    }
}
