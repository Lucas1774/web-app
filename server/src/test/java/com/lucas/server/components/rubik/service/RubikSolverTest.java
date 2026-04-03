package com.lucas.server.components.rubik.service;

import com.lucas.server.ConfiguredTest;
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
@Sql(scripts = {"/seed__algorithms.sql", "/seed__letter__pairs.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RubikSolverTest extends ConfiguredTest {

    private static final boolean WITH_LETTER_PAIRS = true; // disable this to easily check solution validity on tools like http://alg.cubing.net
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
            List<RubikSolver.RubikStep> solution = solver.solve(scramble, true);
            System.out.println(scramble);
            solution.forEach(s -> {
                if (WITH_LETTER_PAIRS) {
                    System.out.println(s.letterPair() + " " + s.o() + " " + s.audioLoop() + "(" + s.type() + "): ");
                }
                System.out.println(s);
            });
            System.out.println("\n");
            assertThat(solution).isNotEmpty()
                    .allMatch(s -> !s.letterPair().isBlank() && null != s.type() && !s.algorithm().isBlank()
                            && !s.o().isBlank() && !s.audioLoop().isBlank());
        }
    }
}
