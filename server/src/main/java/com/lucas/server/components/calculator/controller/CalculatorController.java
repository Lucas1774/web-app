package com.lucas.server.components.calculator.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.calculator.jpa.Calculator;
import com.lucas.server.components.calculator.jpa.CalculatorJpaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.lucas.utils.Utils.EMPTY_STRING;

@RestController
@RequestMapping("/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final ControllerUtil controllerUtil;
    private final CalculatorJpaService calculatorService;

    @PostMapping("/ans")
    public ResponseEntity<String> post(@RequestBody String number) {
        if (200 < number.length()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else {
            return controllerUtil.handleRequest(() -> calculatorService.computeAndSave(number.replace("\"",
                    EMPTY_STRING)));
        }
    }

    @GetMapping("/ans")
    public ResponseEntity<Calculator> get() {
        return calculatorService.find().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
