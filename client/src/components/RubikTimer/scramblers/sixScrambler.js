import { generateRandomBetweenZeroAndX } from "../../../constants";
import { generateMove, SCRAMBLE_MOVES, updateAvailabilityMatrix } from "./Scramble";

export const SCRAMBLE_LENGTH = 80;

export const Scramble = () => {
    let availabilityMatrix = [[true, true, true, true, true], [true, true, true, true, true], [true, true, true, true, true]];
    let turnIterator;
    let scramble = "";
    let move = { axis: null, layer: null };
    let previous;
    let width;
    for (let i = 0; i < SCRAMBLE_LENGTH; i++) {
        width = generateRandomBetweenZeroAndX(5);
        if (width === 4) {
            width = 2;
        } else {
            width = Math.floor(width / 2)
        }
        move = generateMove(availabilityMatrix, width);
        updateAvailabilityMatrix(availabilityMatrix, move, previous);
        turnIterator = generateRandomBetweenZeroAndX(3);
        scramble += SCRAMBLE_MOVES[move.axis][move.layer][turnIterator];
        previous = { ...move };
    }
    return scramble;
};
