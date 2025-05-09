package com.lucas.server.components.calculator.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.components.calculator.service.CalculatorSolver;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CalculatorJpaService implements JpaService<Calculator> {

    private final CalculatorRepository repository;
    private final CalculatorSolver solver;

    public CalculatorJpaService(CalculatorRepository repository, CalculatorSolver solver) {
        this.repository = repository;
        this.solver = solver;
    }

    @Override
    public Optional<Calculator> save(Calculator entity) {
        return Optional.of(this.repository.save(entity));
    }

    @Override
    public List<Calculator> saveAll(Iterable<Calculator> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<Calculator> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<Calculator> findById(Long id) {
        return this.repository.findById(id);
    }

    public Optional<Calculator> find() {
        return this.findAll().stream().findFirst();
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
