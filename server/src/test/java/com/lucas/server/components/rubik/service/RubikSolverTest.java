package com.lucas.server.components.rubik.service;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.jpa.show-sql=false")
@Sql(scripts = "/seed__algorithms.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RubikSolverTest extends ConfiguredTest {

    private static final int NUM_RUNS = 100;
    private static final int SCRAMBLE_LENGTH = 20;
    private static final String[][] MOVE_NAMES;

    static {
        try {
            Field field = RubikSolver.class.getDeclaredField("MOVE_NAMES");
            field.setAccessible(true);
            MOVE_NAMES = (String[][]) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private Random random;

    @Autowired
    private RubikSolver solver;

    private String generateScramble() {
        return IntStream.range(0, SCRAMBLE_LENGTH)
                .mapToObj(i -> MOVE_NAMES[random.nextInt(MOVE_NAMES.length)][random.nextInt(3)])
                .collect(Collectors.joining(" "));
    }

    @Test
    void solveProducesValidSolution() {
        for (int i = 0; NUM_RUNS > i; i++) {
            String scramble = generateScramble();
            List<AlgorithmMapping> solution = solver.solve(scramble);
            System.out.println(scramble + "\n");
            solution.forEach(System.out::println);
            System.out.println("\n");
            assertThat(solution).isNotEmpty();
            assertThat(solution).allMatch(s -> 0 <= s.getFirstSticker() && 0 <= s.getSecondSticker());
        }
    }
}
