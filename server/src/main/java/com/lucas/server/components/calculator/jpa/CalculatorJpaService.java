package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.lucas.server.common.Constants.INVALID_EXPRESSION;

@Service
public class CalculatorJpaService implements JpaService<Calculator> {

    @Delegate
    private final GenericJpaServiceDelegate<Calculator, CalculatorRepository> delegate;
    private final CalculatorRepository repository;
    private final CalculatorSolver solver;

    public CalculatorJpaService(CalculatorRepository repository, CalculatorSolver solver) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        this.repository = repository;
        this.solver = solver;
    }

    public Optional<Calculator> find() {
        return repository.findAll().stream().findFirst();
    }

    @Transactional
    public String computeAndSave(String number) {
        String result = solver.solveExpression(number);
        if (!INVALID_EXPRESSION.equals(result)) {
            find().orElseThrow().setAns(result).setTextMode(false);
        } else {
            find().orElseThrow().setText(number).setTextMode(true);
        }
        return result;
    }
}
