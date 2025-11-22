package com.lucas.server.components.sudoku.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface SudokuRepository extends JpaRepository<Sudoku, Long> {

    Set<Sudoku> findByStateIn(Set<int[]> states);
}
