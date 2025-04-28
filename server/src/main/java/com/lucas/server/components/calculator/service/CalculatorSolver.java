package com.lucas.server.components.calculator.service;

import com.lucas.server.common.Constants;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CalculatorSolver {

    public String solveExpression(String expression) {
        try {
            return new BigDecimal(expression).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e1) {
            try {
                return BigDecimal.valueOf(new ExpressionBuilder(expression.replaceAll("\\s+", ""))
                                .build()
                                .evaluate())
                        .stripTrailingZeros()
                        .toPlainString();
            } catch (IllegalArgumentException e2) {
                return Constants.INVALID_EXPRESSION;
            }
        }
    }
}
