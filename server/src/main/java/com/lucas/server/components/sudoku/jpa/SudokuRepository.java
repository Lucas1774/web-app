package com.lucas.server.components.sudoku.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SudokuRepository extends JpaRepository<Sudoku, Long> {

    Collection<Sudoku> findByStateIn(Collection<int[]> states);
}
