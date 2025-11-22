package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SudokuJpaService implements JpaService<Sudoku> {

    @Delegate
    private final GenericJpaServiceDelegate<Sudoku, SudokuRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> uniqueConstraintDelegate;
    private final SudokuRepository repository;


    public SudokuJpaService(SudokuRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public Set<Sudoku> createIgnoringDuplicates(Set<Sudoku> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(allEntities -> repository.findByStateIn(
                allEntities.stream().map(Sudoku::getState).collect(Collectors.toSet())
        ), entities);
    }
}
