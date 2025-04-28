package com.lucas.server.components.calculator.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CalculatorJpaServiceTest {

    @Autowired
    CalculatorJpaService calculatorService;

    @Test
    @Transactional
    void testComputeAndSaveNumber() {
        // given
        String number = "1+2";

        // when
        String result = this.calculatorService.computeAndSave(number);

        // then
        assertThat(result).isEqualTo("3");
        assertThat(this.calculatorService.find()).isPresent()
                .get()
                .extracting(Calculator::getAns, Calculator::isTextMode)
                .containsExactly("3", false);
    }

    @Test
    @Transactional
    void testComputeAndSaveText() {
        // given
        String text = "Hello World";

        // when
        String result = this.calculatorService.computeAndSave(text);

        // then
        assertThat(result).isEqualTo(Constants.INVALID_EXPRESSION);
        assertThat(this.calculatorService.find()).isPresent()
                .get()
                .extracting(Calculator::getText, Calculator::isTextMode)
                .containsExactly("Hello World", true);
    }
}
