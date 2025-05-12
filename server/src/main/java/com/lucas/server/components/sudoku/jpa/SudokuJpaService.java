package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SudokuJpaService implements JpaService<Sudoku> {

    private final SudokuRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> delegate;


    public SudokuJpaService(SudokuRepository repository, UniqueConstraintWearyJpaServiceDelegate<Sudoku> delegate) {
        this.repository = repository;
        this.delegate = delegate;
    }

    @Override
    public List<Sudoku> createAll(List<Sudoku> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Sudoku> findAll() {
        return this.repository.findAll();
    }

    public List<Sudoku> createIgnoringDuplicates(Iterable<Sudoku> entities) {
        return this.delegate.createIgnoringDuplicates(this.repository,
                entity -> this.repository.findByState(entity.getState()), entities);
    }
}
