import { generateRandomBetweenZeroAndX } from "../../../constants";
import {
    SCRAMBLE_LENGTH as SKEWB_SCRAMBLE_LENGTH,
    Scramble as skewbScramble
} from "./skewbScrambler";

export const SCRAMBLE_LENGTH = SKEWB_SCRAMBLE_LENGTH;
const TIP_SCRAMBLE_MOVES = [
    ["", "u ", "u' "],
    ["", "b ", "b' "],
    ["", "r ", "r' "],
    ["", "l ", "l' "]
];

export const Scramble = () => {
    let scramble = skewbScramble();
    for (const element of TIP_SCRAMBLE_MOVES) {
        scramble += element[generateRandomBetweenZeroAndX(3)];
    }
    return scramble;
};
