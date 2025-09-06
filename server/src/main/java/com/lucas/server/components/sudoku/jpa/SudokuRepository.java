package com.lucas.server.components.sudoku.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SudokuRepository extends JpaRepository<Sudoku, Long> {

    List<Sudoku> findByStateIn(Collection<int[]> states);
}
