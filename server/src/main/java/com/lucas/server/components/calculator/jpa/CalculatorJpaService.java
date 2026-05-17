package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static com.lucas.server.common.Constants.INVALID_EXPRESSION;

@Service
public class CalculatorJpaService implements JpaService<Calculator> {

    private final GenericJpaServiceDelegate<Calculator, Calculator, CalculatorRepository> delegate;
    private final CalculatorRepository repository;
    private final CalculatorSolver solver;

    public CalculatorJpaService(CalculatorRepository repository, CalculatorSolver solver, IdentityEntityMapper<Calculator> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        this.repository = repository;
        this.solver = solver;
    }

    @Transactional(readOnly = true)
    public Optional<Calculator> find() {
        return repository.findAll().stream().findFirst();
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

    @Override
    @Transactional
    public Set<Calculator> saveAll(Set<Calculator> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Calculator> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<Calculator> elements) {
        delegate.deleteAll(elements);
    }
}
