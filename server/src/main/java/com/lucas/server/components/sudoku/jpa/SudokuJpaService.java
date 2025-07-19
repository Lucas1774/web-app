package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

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

    public List<Sudoku> createIgnoringDuplicates(Collection<Sudoku> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(allEntities -> repository.findByStateIn(
                allEntities.stream().map(Sudoku::getState).toList()
        ), entities);
    }
}
