package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SudokuJpaService extends GenericJpaServiceDelegate<Sudoku, Sudoku, SudokuRepository> {

    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> delegate;

    public SudokuJpaService(SudokuRepository repository, EntityMapper<Sudoku, Sudoku> mapper) {
        super(repository, mapper);
        delegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
    }

    @Transactional
    public Set<Sudoku> createIgnoringDuplicates(Set<Sudoku> entities) {
        return delegate.createIgnoringDuplicates(allEntities -> repository.findByStateIn(allEntities.stream()
                .map(Sudoku::getState)
                .collect(Collectors.toUnmodifiableSet())), entities);
    }
}
