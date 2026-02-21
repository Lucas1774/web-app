package com.lucas.server.components.calculator.jpa;

import com.lucas.server.ConfiguredTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.lucas.server.common.Constants.INVALID_EXPRESSION;
import static org.assertj.core.api.Assertions.assertThat;

class CalculatorJpaServiceTest extends ConfiguredTest {

    @Autowired
    private CalculatorJpaService calculatorService;

    @Test
    void computeAndSaveNumber() {
        // given
        String number = "1+2";

        // when
        String result = calculatorService.computeAndSave(number);

        // then
        assertThat(result).isEqualTo("3");
        assertThat(calculatorService.find()).isPresent()
                .get()
                .extracting(Calculator::getAns, Calculator::isTextMode)
                .containsExactly("3", false);
    }

    @Test
    void computeAndSaveText() {
        // given
        String text = "Hello World";

        // when
        String result = calculatorService.computeAndSave(text);

        // then
        assertThat(result).isEqualTo(INVALID_EXPRESSION);
        assertThat(calculatorService.find()).isPresent()
                .get()
                .extracting(Calculator::getText, Calculator::isTextMode)
                .containsExactly("Hello World", true);
    }
}
