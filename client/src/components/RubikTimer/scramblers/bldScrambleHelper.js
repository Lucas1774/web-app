import { generateRandomBetweenZeroAndX } from "../../../constants";
import { generateMove } from "./Scramble";

export const appendBlindfoldScrambleMoves = (
    scramble,
    availabilityMatrix,
    lastTwoMoves,
    updateAvailabilityMatrix,
) => {
    let currentScramble = scramble;
    let currentAvailabilityMatrix = availabilityMatrix;
    let move = { axis: null, layer: null };
    let hasFirst = false;

    if (0 !== generateRandomBetweenZeroAndX(6)) {
        move = generateMove(currentAvailabilityMatrix);
        const turnIterator = generateRandomBetweenZeroAndX(3);
        currentScramble += lastTwoMoves[move.axis][move.layer][turnIterator];
        hasFirst = true;
    }

    if (0 !== generateRandomBetweenZeroAndX(4)) {
        if (hasFirst) {
            updateAvailabilityMatrix(currentAvailabilityMatrix, move.axis);
        }
        move = generateMove(currentAvailabilityMatrix);
        const turnIterator = generateRandomBetweenZeroAndX(3);
        currentScramble += lastTwoMoves[move.axis][move.layer][turnIterator];
    }

    return currentScramble;
};
