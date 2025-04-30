package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SudokuJpaService implements JpaService<Sudoku> {

    private final SudokuRepository repository;
    private final UniqueConstraintWearyJpaServiceDelegate<SudokuRepository, Sudoku> delegate;


    public SudokuJpaService(SudokuRepository repository, UniqueConstraintWearyJpaServiceDelegate<SudokuRepository, Sudoku> delegate) {
        this.repository = repository;
        this.delegate = delegate;
    }

    @Override
    public Optional<Sudoku> save(Sudoku entity) {
        return delegate.save(this.repository, entity);
    }

    @Override
    public List<Sudoku> saveAll(Iterable<Sudoku> entities) {
        return this.delegate.saveAllIgnoringDuplicates(this.repository, entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Sudoku> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<Sudoku> findById(String id) {
        return this.repository.findById(Long.parseLong(id));
    }
}
