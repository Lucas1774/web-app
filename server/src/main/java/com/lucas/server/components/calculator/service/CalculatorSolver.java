package com.lucas.server.components.calculator.service;

import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class CalculatorSolver {
    public String solveExpression(String expression) {
        try {
            return String.valueOf(Double.parseDouble(expression));
        } catch (NumberFormatException e1) {
            try {
                return String.valueOf(new ExpressionBuilder(expression.substring(1, expression.length() - 1)
                        .replaceAll("\\s+", ""))
                        .build()
                        .evaluate());
            } catch (IllegalArgumentException e2) {
                return "Invalid expression";
            }
        }
    }
}
