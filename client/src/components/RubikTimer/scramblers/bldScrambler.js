import { appendBlindfoldScrambleMoves } from "./bldScrambleHelper";
import {
    Scramble as threeScramble
} from "./threeScrambler";

const LAST_TWO = [[
    ["Uw ", "Uw2 ", "Uw' "],
], [
    ["Fw ", "Fw2 ", "Fw' "],
], [
    ["Rw ", "Rw2 ", "Rw' "],
]];

export const Scramble = () => {
    let availabilityMatrix = [[true, true], [true, true], [true, true]];
    let scramble = threeScramble(availabilityMatrix);
    availabilityMatrix.forEach(tuple => {
        tuple.pop();
    });

    return appendBlindfoldScrambleMoves(
        scramble,
        availabilityMatrix,
        LAST_TWO,
        (matrix, moveAxis) => {
            for (let i = 0; i < matrix.length; i++) {
                matrix[i][0] = i !== moveAxis;
            }
        },
    );
};
