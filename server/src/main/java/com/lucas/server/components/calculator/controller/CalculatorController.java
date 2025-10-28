package com.lucas.server.components.calculator.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.calculator.jpa.Calculator;
import com.lucas.server.components.calculator.jpa.CalculatorJpaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.lucas.utils.Utils.EMPTY_STRING;

@RestController
@RequestMapping("/calculator")
public class CalculatorController {

    private final ControllerUtil controllerUtil;
    private final CalculatorJpaService calculatorService;

    public CalculatorController(ControllerUtil controllerUtil, CalculatorJpaService calculatorService) {
        this.controllerUtil = controllerUtil;
        this.calculatorService = calculatorService;
    }

    @PostMapping("/ans")
    public ResponseEntity<String> post(@RequestBody String number) {
        if (number.length() > 200) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else {
            return controllerUtil.handleRequest(() -> calculatorService.computeAndSave(
                    number.replace("\"", EMPTY_STRING)));
        }
    }

    @GetMapping("/ans")
    public ResponseEntity<Calculator> get() {
        return calculatorService.find()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
