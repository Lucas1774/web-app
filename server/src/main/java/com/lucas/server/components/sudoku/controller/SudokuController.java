package com.lucas.server.components.sudoku.controller;

import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.sudoku.jpa.Sudoku;
import com.lucas.server.components.sudoku.jpa.SudokuJpaService;
import com.lucas.server.components.sudoku.mapper.SudokuFileToSudokuMapper;
import com.lucas.server.components.sudoku.service.SudokuGenerator;
import com.lucas.server.components.sudoku.service.SudokuSolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

import static com.lucas.server.common.Constants.EMPTY_STRING;
import static com.lucas.server.common.Constants.SUDOKU_NUMBER_OF_CELLS;

@RestController
@RequestMapping("/sudoku")
public class SudokuController {

    private final SudokuJpaService sudokuService;
    private final SudokuFileToSudokuMapper fromFileMapper;
    private final SudokuGenerator generator;
    private final SudokuSolver solver;
    private final Random random;

    public SudokuController(SudokuJpaService sudokuService, SudokuGenerator generator, SudokuSolver solver,
                            SudokuFileToSudokuMapper fromFileMapper, Random random) {
        this.sudokuService = sudokuService;
        this.generator = generator;
        this.fromFileMapper = fromFileMapper;
        this.solver = solver;
        this.random = random;
    }

    @PostMapping("/upload/sudokus")
    public ResponseEntity<List<Sudoku>> handleFileUpload(@RequestBody String file) {
        if (file.length() > 10000) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Sudoku> sudoku;
        try {
            sudoku = fromFileMapper.map(file.replace("\"", EMPTY_STRING)).stream()
                    .filter(s -> {
                        Sudoku copy = Sudoku.withValues(s.getState());
                        return solver.isValid(s, -1) && solver.solveWithTimeout(copy);
                    })
                    .toList();
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok(sudokuService.createIgnoringDuplicates(sudoku));
    }

    @GetMapping("fetch/sudoku")
    public ResponseEntity<Sudoku> getRandom() {
        List<Sudoku> sudoku = sudokuService.findAll();
        if (sudoku.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.ok(sudoku.get(random.nextInt(sudoku.size())));
        }
    }

    @GetMapping("generate/sudoku")
    public ResponseEntity<Sudoku> generateRandom(@RequestParam("difficulty") int difficulty) {
        return ResponseEntity.ok(generator.generate(difficulty));
    }

    @PostMapping("/solve/sudoku")
    public ResponseEntity<Sudoku> solveSudoku(@RequestBody Sudoku sudoku) {
        Sudoku s = Sudoku.withValues(sudoku.getState());
        if (!solver.isValid(s, -1) || !solver.solveWithTimeout(s)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        return ResponseEntity.ok(s);
    }

    @PostMapping("/check/sudoku")
    public ResponseEntity<Boolean> checkSudoku(@RequestBody List<Sudoku> sudoku) {
        if (2 != sudoku.size()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int[] initialValues = sudoku.getFirst().getState();
        Sudoku s = Sudoku.withValues(initialValues);
        if (!solver.isValid(s, -1) || !solver.solveWithTimeout(s)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        int[] currentState = s.getState();
        int[] initialState = sudoku.get(1).getState();
        for (int i = 0; i < SUDOKU_NUMBER_OF_CELLS; i++) {
            if (initialState[i] != 0 && currentState[i] != initialState[i]) {
                return ResponseEntity.ok(false);
            }
        }

        return ResponseEntity.ok(true);
    }
}
