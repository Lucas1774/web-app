package com.lucas.server.components.calculator.service;

import org.junit.jupiter.api.Test;

import static com.lucas.server.common.Constants.INVALID_EXPRESSION;
import static org.assertj.core.api.Assertions.assertThat;

class CalculatorSolverTest {

    private final CalculatorSolver solver = new CalculatorSolver();

    @Test
    void solveNumber() {
        assertThat(solver.solveExpression("42")).isEqualTo("42");
        assertThat(solver.solveExpression("0")).isEqualTo("0");
        assertThat(solver.solveExpression("3.14")).isEqualTo("3.14");
    }

    @Test
    void solveSimpleExpression() {
        assertThat(solver.solveExpression("1+1")).isEqualTo("2");
        assertThat(solver.solveExpression("10-5")).isEqualTo("5");
        assertThat(solver.solveExpression("3*4")).isEqualTo("12");
        assertThat(solver.solveExpression("8/2")).isEqualTo("4");
    }

    @Test
    void solveExpressionWithSpaces() {
        assertThat(solver.solveExpression("1 + 1")).isEqualTo("2");
        assertThat(solver.solveExpression("10 - 5")).isEqualTo("5");
    }

    @Test
    void solveComplexExpression() {
        assertThat(solver.solveExpression("2+3*4")).isEqualTo("14");
        assertThat(solver.solveExpression("(2+3)*4")).isEqualTo("20");
    }

    @Test
    void solveInvalidExpression() {
        assertThat(solver.solveExpression("hello")).isEqualTo(INVALID_EXPRESSION);
        assertThat(solver.solveExpression("1+")).isEqualTo(INVALID_EXPRESSION);
        assertThat(solver.solveExpression("1+hello")).isEqualTo(INVALID_EXPRESSION);
    }

    @Test
    void stripTrailingZeros() {
        assertThat(solver.solveExpression("10/4")).isEqualTo("2.5");
        assertThat(solver.solveExpression("1.0")).isEqualTo("1");
    }
}
