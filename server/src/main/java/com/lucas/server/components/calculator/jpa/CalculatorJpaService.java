package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.components.calculator.dto.CalculatorDomain;
import com.lucas.server.components.calculator.mapper.CalculatorMapper;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.lucas.server.common.Constants.INVALID_EXPRESSION;

@Service
public class CalculatorJpaService
        extends GenericJpaServiceDelegate<Calculator, CalculatorDomain, CalculatorRepository> {

    private final CalculatorSolver solver;

    public CalculatorJpaService(CalculatorRepository repository, CalculatorMapper mapper, CalculatorSolver solver) {
        super(repository, mapper);
        this.solver = solver;
    }

    @Transactional(readOnly = true)
    public Optional<CalculatorDomain> find() {
        return repository.findAll().stream().findFirst().map(mapper::toDto);
    }

    @Transactional
    public String computeAndSave(String number) {
        String result = solver.solveExpression(number);
        if (!INVALID_EXPRESSION.equals(result)) {
            repository.findAll().stream().findFirst().orElseThrow().setAns(result).setTextMode(false);
        } else {
            repository.findAll().stream().findFirst().orElseThrow().setText(number).setTextMode(true);
        }
        return result;
    }
}
