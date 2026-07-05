import { generateRandomBetweenZeroAndX } from "../../../constants";
import { generateMove, SCRAMBLE_MOVES, updateAvailabilityMatrix } from "./Scramble";
import {
    SCRAMBLE_LENGTH as THREE_SCRAMBLE_LENGTH,
    Scramble as threeScramble
} from "./threeScrambler";
import { SCRAMBLE_LENGTH as TWO_SCRAMBLER_LENGTH } from "./twoScrambler";

export const SCRAMBLE_LENGTH = 48;
const MOVES_TO_REMOVE = 3;
export const Scramble = () => {
    let availabilityMatrix = [[true, true], [true, true], [true, true]];
    let turnIterator;
    let scramble = threeScramble(availabilityMatrix)
        .trim()
        .split(" ")
        .slice(MOVES_TO_REMOVE) // Remove the first three moves
        .join(" ")
        .concat(" ");
    availabilityMatrix.forEach(row => {
        row.push(true);
    });
    let move = { axis: null, layer: null };
    let previous;
    let widths = new Array(SCRAMBLE_LENGTH - ((THREE_SCRAMBLE_LENGTH - MOVES_TO_REMOVE) + TWO_SCRAMBLER_LENGTH)).fill(0).concat(new Array(TWO_SCRAMBLER_LENGTH).fill(1));
    for (let i = widths.length - 1; i > 0; i--) {
        const j = generateRandomBetweenZeroAndX((i + 1));
        [widths[i], widths[j]] = [widths[j], widths[i]];
    }
    for (let width of widths) {
        move = generateMove(availabilityMatrix, width);
        updateAvailabilityMatrix(availabilityMatrix, move, previous);
        turnIterator = generateRandomBetweenZeroAndX(3);
        scramble += SCRAMBLE_MOVES[move.axis][move.layer][turnIterator];
        previous = { ...move };
    }
    return scramble;
};
