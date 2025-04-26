package com.lucas.server.components.calculator.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import com.lucas.server.connection.DAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/calculator")
public class CalculatorController {

    private final ControllerUtil controllerUtil;
    private final DAO dao;
    private final CalculatorSolver solver;

    public CalculatorController(ControllerUtil controllerUtil, DAO dao, CalculatorSolver solver) {
        this.controllerUtil = controllerUtil;
        this.dao = dao;
        this.solver = solver;
    }

    @PostMapping("/ans")
    public ResponseEntity<String> post(@RequestBody String number) {
        if (number.length() > 200) {
            return ResponseEntity.ok().body("Stop");
        } else {
            return this.controllerUtil.handleRequest(() -> {
                String result = solver.solveExpression(number);
                if (!"Invalid expression".equals(result)) {
                    dao.insert(Double.parseDouble(result));
                } else {
                    dao.insertString(number);
                }
                return result;
            });
        }
    }

    @GetMapping("/ans")
    public ResponseEntity<String> get() {
        return this.controllerUtil.handleRequest(dao::get);
    }
}
