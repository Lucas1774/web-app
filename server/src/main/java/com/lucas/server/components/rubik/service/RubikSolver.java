package com.lucas.server.components.rubik.service;

import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import com.lucas.server.components.rubik.jpa.AlgorithmMappingJpaService;
import com.lucas.server.components.rubik.jpa.LetterPairs;
import com.lucas.server.components.rubik.jpa.LetterPairsJpaService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;
import static com.lucas.server.common.Constants.AlgorithmKind.*;

@RequiredArgsConstructor
@Service
public class RubikSolver {

    private static final int[][] PERMS_CORNERS = {
            {0, 3, 9, 6, 1, 4, 10, 7, 2, 5, 11, 8},
            {12, 15, 18, 21, 13, 16, 19, 22, 14, 17, 20, 23},
            {0, 17, 12, 5, 1, 15, 13, 3, 2, 16, 14, 4},
            {6, 10, 21, 19, 7, 11, 22, 20, 8, 9, 23, 18},
            {0, 7, 18, 16, 1, 8, 19, 17, 2, 6, 20, 15},
            {3, 14, 21, 11, 4, 12, 22, 9, 5, 13, 23, 10}
    };
    private static final int[][] PERMS_EDGES = {
            {0, 4, 6, 2, 1, 5, 7, 3},
            {12, 14, 18, 16, 13, 15, 19, 17},
            {0, 9, 12, 11, 1, 8, 13, 10},
            {6, 23, 18, 21, 7, 22, 19, 20},
            {2, 20, 14, 8, 3, 21, 15, 9},
            {4, 10, 16, 22, 5, 11, 17, 23}
    };

    private static final String[][] MOVE_NAMES = {
            {"U", "U2", "U'"},
            {"D", "D2", "D'"},
            {"F", "F2", "F'"},
            {"B", "B2", "B'"},
            {"R", "R2", "R'"},
            {"L", "L2", "L'"}
    };

    private static final Map<Character, Character> LETTER_AUDIO = Map.ofEntries(
            Map.entry('A', 'á'),
            Map.entry('B', 'b'),
            Map.entry('C', 'k'),
            Map.entry('D', 'd'),
            Map.entry('E', 'é'),
            Map.entry('F', 'f'),
            Map.entry('J', 'j'),
            Map.entry('H', 'e'),
            Map.entry('I', 'í'),
            Map.entry('K', 'o'),
            Map.entry('L', 'l'),
            Map.entry('M', 'm'),
            Map.entry('N', 'n'),
            Map.entry('O', 'ó'),
            Map.entry('P', 'p'),
            Map.entry('R', 'r'),
            Map.entry('S', 's'),
            Map.entry('T', 'a'),
            Map.entry('U', 'ú'),
            Map.entry('V', 'u'),
            Map.entry('Y', 'y'),
            Map.entry('Z', 'z'),
            Map.entry('X', 'x')
    );

    private final AlgorithmMappingJpaService algorithmMappingJpaService;
    private final LetterPairsJpaService letterPairsJpaService;

    public List<RubikStep> solve(String scramble, boolean withLetterPairs) {
        String[] tokens = scramble.trim().split("\\s+");
        int[][] moves = new int[tokens.length][2];
        for (int i = 0; i < tokens.length; i++) {
            boolean found = false;
            for (int f = 0; f < MOVE_NAMES.length && !found; f++) {
                for (int t = 0; t < MOVE_NAMES[f].length && !found; t++) {
                    if (MOVE_NAMES[f][t].equals(tokens[i])) {
                        moves[i][0] = f;
                        moves[i][1] = t;
                        found = true;
                    }
                }
            }
        }
        int[] corners = applyScramble(moves, PERMS_CORNERS);
        int[] edges = applyScramble(moves, PERMS_EDGES);

        // corners
        List<Integer> cornerPath = new ArrayList<>();
        List<Integer> twists = new ArrayList<>();
        boolean[] solved = new boolean[CORNERS];
        int solvedCount = 0;

        for (int i = 0; CORNERS > i; i++) {
            int s0 = corners[3 * i];
            if (s0 == 3 * i) {
                solved[i] = true;
                solvedCount++;
            } else if (s0 == 3 * i + 1) {
                solved[i] = true;
                solvedCount++;
                if (0 != i) {
                    twists.add(corners[3 * i + 1]);
                    twists.add(corners[3 * i + 2]);
                }
            } else if (s0 == 3 * i + 2) {
                solved[i] = true;
                solvedCount++;
                if (0 != i) {
                    twists.add(corners[3 * i + 2]);
                    twists.add(corners[3 * i + 1]);
                }
            }
        }

        while (CORNERS > solvedCount) {
            int target;
            int cycleStart = -1;
            for (int i = 0; CORNERS > i; i++) {
                if (2 < corners[3 * i] && !solved[i]) {
                    cycleStart = i;
                    break;
                }
            }
            target = corners[3 * cycleStart];
            if (0 != cycleStart) cornerPath.add(3 * cycleStart);
            solved[cycleStart] = true;
            solvedCount++;

            while (target / 3 != cycleStart) {
                cornerPath.add(target);
                solved[target / 3] = true;
                target = corners[target];
                solvedCount++;
                if (target / 3 == cycleStart && 0 != cycleStart) cornerPath.add(target);
            }
        }

        // edges
        List<Integer> edgePath = new ArrayList<>();
        List<Integer> flips = new ArrayList<>();
        solved = new boolean[EDGES];
        solvedCount = 0;

        for (int i = 0; EDGES > i; i++) {
            if (edges[2 * i] == 2 * i) {
                solved[i] = true;
                solvedCount++;
            } else if (edges[2 * i] == 2 * i + 1) {
                solved[i] = true;
                solvedCount++;
                if (0 != i) {
                    flips.add(edges[2 * i]);
                    flips.add(edges[2 * i + 1]);
                }
            }
        }

        while (EDGES > solvedCount) {
            int target;
            int cycleStart = -1;
            for (int i = 0; EDGES > i; i++) {
                if (1 < edges[2 * i] && !solved[i]) {
                    cycleStart = i;
                    break;
                }
            }
            target = edges[2 * cycleStart];
            if (0 != cycleStart) edgePath.add(2 * cycleStart);
            solved[cycleStart] = true;
            solvedCount++;

            while (target / 2 != cycleStart) {
                edgePath.add(target);
                solved[target / 2] = true;
                target = edges[target];
                solvedCount++;
                if (target / 2 == cycleStart && 0 != cycleStart) edgePath.add(target);
            }
        }

        // parity
        int[] parity;
        if (0 != cornerPath.size() % 2) {
            int cornerSticker = cornerPath.removeLast();
            int edgeSticker = edgePath.isEmpty() ? -1 : edgePath.removeFirst();
            parity = new int[]{cornerSticker, edgeSticker};
        } else {
            parity = new int[0];
        }

        List<RubikStep> steps = new ArrayList<>();
        for (int i = 0; i + 1 < cornerPath.size(); i += 2) {
            AlgorithmMapping m = algorithmMappingJpaService.findByStickers(cornerPath.get(i), cornerPath.get(i + 1), CORNER).orElseThrow();
            steps.add(createRubikStep(m, CORNER, withLetterPairs));
        }
        if (0 != parity.length) {
            steps.addAll(algorithmMappingJpaService.findByStickers(parity[0], parity[1], PARITY)
                    .map(m -> List.of(createRubikStep(m, PARITY, withLetterPairs)))
                    .orElseGet(() -> List.of(
                            createRubikStep(algorithmMappingJpaService.findByStickers(parity[0], 2, PARITY).orElseThrow(), PARITY, withLetterPairs),
                            createRubikStep(algorithmMappingJpaService.findByStickers(2, parity[1], EDGE).orElseThrow(), EDGE, withLetterPairs)
                    ))
            );
        }
        for (int i = 0; i + 1 < edgePath.size(); i += 2) {
            AlgorithmMapping m = algorithmMappingJpaService.findByStickers(edgePath.get(i), edgePath.get(i + 1), EDGE).orElseThrow();
            steps.add(createRubikStep(m, EDGE, withLetterPairs));
        }
        for (int i = 0; i + 1 < twists.size(); i += 2) {
            AlgorithmMapping m = algorithmMappingJpaService.findByStickers(twists.get(i), twists.get(i + 1), CORNER).orElseThrow();
            steps.add(createRubikStep(m, CORNER, withLetterPairs));
        }
        for (int i = 0; i + 1 < flips.size(); i += 2) {
            AlgorithmMapping m = algorithmMappingJpaService.findByStickers(flips.get(i), flips.get(i + 1), EDGE).orElseThrow();
            steps.add(createRubikStep(m, EDGE, withLetterPairs));
        }

        return steps;
    }

    private int[] applyScramble(int[][] moves, int[][] perms) {
        int[] state = new int[STICKERS];
        for (int i = 0; STICKERS > i; i++) state[i] = i;
        for (int[] move : moves) {
            int[] p = perms[move[0]];
            int reps = move[1] + 1;
            for (int r = 0; r < reps; r++) {
                for (int g = 0; g < p.length; g += 4) {
                    int aux = state[p[g]];
                    state[p[g]] = state[p[g + 3]];
                    state[p[g + 3]] = state[p[g + 2]];
                    state[p[g + 2]] = state[p[g + 1]];
                    state[p[g + 1]] = aux;
                }
            }
        }
        return state;
    }

    private RubikStep createRubikStep(AlgorithmMapping m, AlgorithmKind kind, boolean withLetterPairs) {
        String algorithm = null;
        String algorithmType = null;
        String technique = null;
        char firstLetter = 0;
        char secondLetter = 0;
        switch (kind) {
            case EDGE:
                algorithm = m.getEdgeAlgorithm();
                algorithmType = m.getEdgeType();
                technique = m.getEdgeTechnique();
                firstLetter = getLettersEdges()[m.getFirstSticker()];
                secondLetter = getLettersEdges()[m.getSecondSticker()];
                break;
            case CORNER:
                algorithm = m.getCornerAlgorithm();
                algorithmType = m.getCornerType();
                technique = m.getCornerTechnique();
                firstLetter = getLettersCorners()[m.getFirstSticker()];
                secondLetter = getLettersCorners()[m.getSecondSticker()];
                break;
            case PARITY:
                algorithm = m.getParityAlgorithm();
                firstLetter = getLettersCorners()[m.getFirstSticker()];
                secondLetter = getLettersEdges()[m.getSecondSticker()];
                break;
        }
        String letterPair = String.valueOf(firstLetter) + secondLetter;
        String audioLoop = String.valueOf(LETTER_AUDIO.get(firstLetter)) + LETTER_AUDIO.get(secondLetter);
        String person = null;
        String action = null;
        String object = null;
        if (withLetterPairs) {
            LetterPairs pairs = letterPairsJpaService.findByLetterPair(letterPair).orElseThrow();
            person = pairs.getPerson();
            action = pairs.getAction();
            object = pairs.getObject();
        }

        return new RubikStep(kind, algorithm, algorithmType, technique, letterPair, audioLoop, person, action, object);
    }

    public record RubikStep(
            AlgorithmKind type,
            String algorithm,
            String algorithmType,
            String technique,
            String letterPair,
            String audioLoop,
            String p,
            String a,
            String o
    ) {
        @NonNull
        @Override
        public String toString() {
            return algorithm;
        }
    }
}
