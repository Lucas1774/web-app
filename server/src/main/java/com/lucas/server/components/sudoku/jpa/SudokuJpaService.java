package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.components.sudoku.dto.SudokuDomain;
import com.lucas.server.components.sudoku.mapper.SudokuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SudokuJpaService extends GenericJpaServiceDelegate<Sudoku, SudokuDomain, SudokuRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> delegate;

    public SudokuJpaService(SudokuRepository repository, SudokuMapper mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional
    public Set<SudokuDomain> createIgnoringDuplicates(Set<SudokuDomain> dtos) {
        Set<Sudoku> entitySet = dtos.stream().map(mapper::toEntity).collect(Collectors.toSet());
        return delegate.createIgnoringDuplicates(allEntities -> repository.findByStateIn(allEntities.stream()
                        .map(Sudoku::getState)
                        .collect(Collectors.toUnmodifiableSet())), entitySet)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toUnmodifiableSet());
    }
}
