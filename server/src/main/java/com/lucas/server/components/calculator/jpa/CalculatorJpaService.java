package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CalculatorJpaService implements JpaService<Calculator> {

    @Delegate
    private final GenericJpaServiceDelegate<Calculator, CalculatorRepository> delegate;
    private final CalculatorRepository repository;
    private final CalculatorSolver solver;

    public CalculatorJpaService(CalculatorRepository repository, CalculatorSolver solver) {
        this.delegate = new GenericJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.solver = solver;
    }

    public Optional<Calculator> find() {
        return this.repository.findAll().stream().findFirst();
    }

    @Transactional
    public String computeAndSave(String number) {
        String result = solver.solveExpression(number);
        if (!Constants.INVALID_EXPRESSION.equals(result)) {
            this.find().orElseThrow().setAns(result).setTextMode(false);
        } else {
            this.find().orElseThrow().setText(number).setTextMode(true);
        }
        return result;
    }
}
