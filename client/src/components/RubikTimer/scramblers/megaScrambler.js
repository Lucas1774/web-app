import { generateRandomBetweenZeroAndX } from "../../../constants";
export const SCRAMBLE_LENGTH = 77;
const SCRAMBLE_MOVES = [
    ["R++ ", "R-- "],
    ["D++ ", "D-- "],
];
const ELEVENTH = ["U \n", "U' \n"];

export const Scramble = () => {
    let scramble = "";
    let turn = 0;
    let turnIterator;
    for (let i = 0; i < SCRAMBLE_LENGTH; i++) {
        turnIterator = generateRandomBetweenZeroAndX(2);
        if ((i + 1) % 11 === 0) {
            scramble += ELEVENTH[turnIterator];
        } else {
            scramble += SCRAMBLE_MOVES[turn][turnIterator];
            turn = (turn + 1) % 2;
        }
    }
    return scramble;
};
