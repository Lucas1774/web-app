import { generateRandomBetweenZeroAndX } from "../../../constants";
export const SCRAMBLE_LENGTH = 15;
const SCRAMBLE_MOVES = ["5- ", "4- ", "3- ", "2- ", "1- ", "0+ ", "1+ ", "2+ ", "3+ ", "4+ ", "5+ ", "6+ "];
const SCRAMBLE_STEPS = ["UR", "DR", "DL", "UL", "U", "R", "D", "L", "ALL", "y2", "U", "R", "D", "L", "ALL"];

export const Scramble = () => {
    let scramble = "";
    let turnIterator;
    for (let i = 0; i < SCRAMBLE_LENGTH; i++) {
        turnIterator = generateRandomBetweenZeroAndX(12);
        scramble += SCRAMBLE_STEPS[i].concat(i === 9 ? " " : SCRAMBLE_MOVES[turnIterator]);
    }
    return scramble;
};
