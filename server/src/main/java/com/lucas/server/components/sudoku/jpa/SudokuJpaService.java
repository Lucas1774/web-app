package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SudokuJpaService implements JpaService<Sudoku> {

    @Delegate
    private final GenericJpaServiceDelegate<Sudoku, SudokuRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> uniqueConstraintDelegate;
    private final SudokuRepository repository;


    public SudokuJpaService(SudokuRepository repository) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public List<Sudoku> createIgnoringDuplicates(Iterable<Sudoku> entities) {
        return this.uniqueConstraintDelegate.createIgnoringDuplicates(entity -> this.repository.findByState(entity.getState()), entities);
    }
}
