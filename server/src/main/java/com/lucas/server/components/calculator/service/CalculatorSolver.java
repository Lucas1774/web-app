package com.lucas.server.components.calculator.service;

import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.lucas.server.common.Constants.EMPTY_STRING;
import static com.lucas.server.common.Constants.INVALID_EXPRESSION;

@Component
public class CalculatorSolver {

    public String solveExpression(String expression) {
        try {
            return new BigDecimal(expression).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e1) {
            try {
                return BigDecimal.valueOf(new ExpressionBuilder(expression.replaceAll("\\s+", EMPTY_STRING))
                                .build()
                                .evaluate())
                        .stripTrailingZeros()
                        .toPlainString();
            } catch (IllegalArgumentException e2) {
                return INVALID_EXPRESSION;
            }
        }
    }
}
