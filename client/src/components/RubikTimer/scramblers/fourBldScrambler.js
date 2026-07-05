import { appendBlindfoldScrambleMoves } from "./bldScrambleHelper";
import {
    Scramble as fourScrambler
} from "./fourScrambler";

const LAST_TWO = [[
    ["x ", "x2 ", "x' "],
], [
    ["y ", "y2 ", "y' "],
], [
    ["z ", "z2 ", "z' "],
]];

export const Scramble = () => {
    let availabilityMatrix = [[true], [true], [true]];
    let scramble = fourScrambler();

    return appendBlindfoldScrambleMoves(
        scramble,
        availabilityMatrix,
        LAST_TWO,
        (matrix, moveAxis) => {
            matrix[moveAxis][0] = false;
        },
    );
};
