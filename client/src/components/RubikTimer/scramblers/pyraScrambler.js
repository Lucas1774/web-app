import { generateRandomBetweenZeroAndX } from "../../../constants";
import { Scramble as skewbScramble } from "./skewbScrambler";

export { SCRAMBLE_LENGTH } from "./skewbScrambler";

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
