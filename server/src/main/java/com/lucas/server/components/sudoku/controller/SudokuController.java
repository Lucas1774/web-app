package com.lucas.server.components.sudoku.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.sudoku.service.Generator;
import com.lucas.server.components.sudoku.Sudoku;
import com.lucas.server.connection.DAO;
import com.lucas.server.components.sudoku.mapper.SudokuFileToSudokuMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/sudoku")
public class SudokuController {

    private final ControllerUtil controllerUtil;
    private final DAO dao;
    private final Generator generator;
    private final SudokuFileToSudokuMapper mapper;
    private final Random random;

    public SudokuController(ControllerUtil controllerUtil, DAO dao, Generator generator, SudokuFileToSudokuMapper mapper, Random random) {
        this.controllerUtil = controllerUtil;
        this.dao = dao;
        this.generator = generator;
        this.mapper = mapper;
        this.random = random;
    }

    @PostMapping("/upload/sudokus")
    public ResponseEntity<String> handleFileUpload(@RequestBody String file) {
        if (file.length() > 10000) {
            return ResponseEntity.ok().body("Stop");
        } else {
            return this.controllerUtil.handleRequest(() -> {
                List<Sudoku> sudokus = mapper.fromString(file.replace("\"", "")).stream()
                        .filter(s -> {
                            Sudoku copy = Sudoku.withValues(s.get());
                            return s.isValid(-1) && copy.solveWithTimeout();
                        })
                        .toList();
                if (!sudokus.isEmpty()) {
                    dao.insertSudokus(sudokus);
                    return "1";
                } else {
                    return "No sudokus were inserted";
                }
            });
        }
    }

    @GetMapping("fetch/sudoku")
    public ResponseEntity<String> getRandom() {
        return this.controllerUtil.handleRequest(() -> {
            List<Sudoku> sudokus = dao.getSudokus();
            if (sudokus.isEmpty()) {
                return "No sudokus found";
            } else {
                return sudokus.get(this.random.nextInt(sudokus.size())).serialize();
            }
        });
    }

    @GetMapping("generate/sudoku")
    public ResponseEntity<String> generateRandom(@RequestParam("difficulty") int difficulty) {
        return this.controllerUtil.handleRequest(() -> generator.generate(difficulty).serialize());
    }

    @GetMapping("/solve/sudoku")
    public String solveSudoku(@RequestParam String sudoku) {
        int[] values = Sudoku.deserialize(sudoku);
        if (0 == values.length) {
            return "Invalid sudoku";
        }
        Sudoku s = Sudoku.withValues(values);
        if (!s.isValid(-1)) {
            return "Sudoku might have more than one solution";
        }
        if (!s.solveWithTimeout()) {
            return "Unsolvable sudoku";
        }
        return s.serialize();
    }

    @GetMapping("/check/sudoku")
    public ResponseEntity<String> checkSudoku(@RequestParam String initialSudoku, @RequestParam String currentSudoku) {
        return this.controllerUtil.handleRequest(() -> {
            int[] values = Sudoku.deserialize(initialSudoku);
            Sudoku s = Sudoku.withValues(values);
            if (!s.isValid(-1)) {
                return "Sudoku might have more than one solution";
            }
            if (!s.solveWithTimeout()) {
                return "Unsolvable sudoku";
            }
            String serialized = s.serialize().replace("\"", "");
            String solvable = "1";
            for (int i = 0; i < Sudoku.NUMBER_OF_CELLS; i++) {
                if (currentSudoku.charAt(i) != '0' && serialized.charAt(i) != currentSudoku.charAt(i)) {
                    solvable = "0";
                    break;
                }
            }
            return solvable;
        });
    }
}
