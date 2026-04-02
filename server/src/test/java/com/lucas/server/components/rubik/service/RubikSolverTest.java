package com.lucas.server.components.rubik.service;

import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import com.lucas.server.components.rubik.jpa.AlgorithmMappingJpaService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.lucas.server.common.Constants.AlgorithmKind.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO: make it extend BaseTest and seed AlgorithmMapping table. Remove mocks
class RubikSolverTest {

    private static final int NUM_RUNS = 1000;
    private static final int SCRAMBLE_LENGTH = 20;
    private static final String[][] MOVE_NAMES;

    // TODO: move to importer and use it there probably
    @SuppressWarnings("unused")
    private static final char[] LETTERS_CORNERS = "0SFMXBRVPUNLITCOYJEKDAHZ".toCharArray();
    @SuppressWarnings("unused")
    private static final char[] LETTERS_EDGES = "0XRSMNÚUFJKBÍIÓOÁAÉEDPLZ".toCharArray();

    static {
        try {
            Field field = RubikSolver.class.getDeclaredField("MOVE_NAMES");
            field.setAccessible(true);
            MOVE_NAMES = (String[][]) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final Random random = new Random();
    private final AlgorithmMappingJpaService algorithmMappingJpaService = mock(AlgorithmMappingJpaService.class);
    private final RubikSolver solver = new RubikSolver(algorithmMappingJpaService);

    {
        when(algorithmMappingJpaService.findByStickers(anyInt(), anyInt(), eq(CORNER)))
                .thenAnswer(inv -> Optional.of(new AlgorithmMapping()
                        .setLetterPair(new String(new char[]{LETTERS_CORNERS[(int) inv.getArgument(0)], LETTERS_CORNERS[(int) inv.getArgument(1)]}))
                        .setFirstSticker(inv.getArgument(0))
                        .setSecondSticker(inv.getArgument(1))));
        when(algorithmMappingJpaService.findByStickers(anyInt(), anyInt(), eq(EDGE)))
                .thenAnswer(inv -> Optional.of(new AlgorithmMapping()
                        .setLetterPair(new String(new char[]{LETTERS_EDGES[(int) inv.getArgument(0)], LETTERS_EDGES[(int) inv.getArgument(1)]}))
                        .setFirstSticker(inv.getArgument(0))
                        .setSecondSticker(inv.getArgument(1))));
        when(algorithmMappingJpaService.findByStickers(anyInt(), anyInt(), eq(PARITY)))
                .thenAnswer(inv -> Optional.of(new AlgorithmMapping()
                        .setLetterPair(new String(new char[]{LETTERS_CORNERS[(int) inv.getArgument(0)], LETTERS_EDGES[(int) inv.getArgument(1)]}))
                        .setFirstSticker(inv.getArgument(0))
                        .setSecondSticker(inv.getArgument(1))));

    }

    private String generateScramble() {
        return IntStream.range(0, SCRAMBLE_LENGTH)
                .mapToObj(i -> MOVE_NAMES[random.nextInt(MOVE_NAMES.length)][random.nextInt(3)])
                .collect(Collectors.joining(" "));
    }

    @Test
    void solveProducesValidLetterPairs() {
        for (int i = 0; NUM_RUNS > i; i++) {
            String scramble = generateScramble();
            List<AlgorithmMapping> solution = solver.solve(scramble);
            // TODO: to string and verify in http://alg.cubing.net
            assertThat(solution).isNotEmpty();
            assertThat(solution).allMatch(s -> 0 <= s.getFirstSticker() && 0 <= s.getSecondSticker());
        }
    }
}
