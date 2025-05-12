package com.lucas.server.components.sudoku.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SudokuRepository extends JpaRepository<Sudoku, Long> {

    Optional<Sudoku> findByState(int[] state);
}
