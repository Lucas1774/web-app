package com.lucas.server.components.sudoku.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.jpa.UniqueConstraintWearyJpaServiceDelegate;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SudokuJpaService implements JpaService<Sudoku> {

    private final GenericJpaServiceDelegate<Sudoku, Sudoku, SudokuRepository> delegate;
    private final UniqueConstraintWearyJpaServiceDelegate<Sudoku> uniqueConstraintDelegate;
    private final SudokuRepository repository;

    public SudokuJpaService(SudokuRepository repository, IdentityEntityMapper<Sudoku> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        uniqueConstraintDelegate = new UniqueConstraintWearyJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    @Transactional
    public Set<Sudoku> createIgnoringDuplicates(Set<Sudoku> entities) {
        return uniqueConstraintDelegate.createIgnoringDuplicates(allEntities -> repository.findByStateIn(
                allEntities.stream().map(Sudoku::getState).collect(Collectors.toSet())
        ), entities);
    }

    @Override
    @Transactional
    public Set<Sudoku> saveAll(Set<Sudoku> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Sudoku> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<Sudoku> elements) {
        delegate.deleteAll(elements);
    }
}
