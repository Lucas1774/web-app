import { generateRandomBetweenZeroAndX } from "../../../constants";

export const SCRAMBLE_LENGTH = 15;
const INITIAL_STATE = [[0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2], [1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0]];

export const Scramble = (isHorizontal) => {

    const slice = (state) => {
        const firstRowSix = state[0].slice(0, 6);
        const secondRowSix = state[1].slice(-6);
        for (let i = 0; i < 6; i++) {
            state[0][i] = secondRowSix[i];
            state[1][i + 6] = firstRowSix[i];
        }
    };

    const getAvailableIndexes = (state) => {
        let availableIndexes = [[0], [0]];
        for (let i = 0; i < 2; i++) {
            for (let j = 0; j < 11; j++) {
                if ((state[i][j] !== 1)
                    && (state[i][(j + 6) % 12] !== 1)) {
                    availableIndexes[i].push(j + 1);
                }
            }
        }
        return availableIndexes;
    };

    const mapIdxToMove = (idx) => {
        return idx < 7 ? " ".concat(idx.toString()) : (idx - 12).toString();
    };

    const movesPerLine = isHorizontal ? 5 : 3;
    let scramble = "";
    let upIdx;
    let downIdx;
    let availableIndexes;
    let state = INITIAL_STATE.map(row => row.slice());
    for (let i = 0; i < SCRAMBLE_LENGTH; i++) {
        availableIndexes = getAvailableIndexes(state);
        if (0 !== generateRandomBetweenZeroAndX(4)) { // sixes seem less likely than they should be naturally
            availableIndexes[0] = availableIndexes[0].filter((idx) => idx !== 6);
            availableIndexes[1] = availableIndexes[1].filter((idx) => idx !== 6);
        }
        if (i > 3 && availableIndexes[1].length < 3) { // make sure down can be 0
            availableIndexes[0].shift(1);
        }
        upIdx = availableIndexes[0][generateRandomBetweenZeroAndX(availableIndexes[0].length)];
        state[0] = state[0].slice(upIdx).concat(state[0].slice(0, upIdx));
        if (i > 2) { // no positive numbers down after third move for some reason. Seem converted to 0
            availableIndexes[1] = availableIndexes[1].map((idx) => idx > 6 ? idx : 0);
        }
        if (upIdx === 0) { // remove down 0 if up is 0
            availableIndexes[1] = availableIndexes[1].filter((idx) => idx !== 0);
        }
        if (i < 4) { // prevent shape change for the first four moves
            if (state[0][0] === state[1][0]) {
                availableIndexes[1] = availableIndexes[1].filter((idx) => idx % 3 === 0);
            } else {
                availableIndexes[1] = availableIndexes[1].filter((idx) => idx % 3 !== 0);
            }
        }
        downIdx = availableIndexes[1][generateRandomBetweenZeroAndX(availableIndexes[1].length)];
        state[1] = state[1].slice(downIdx).concat(state[1].slice(0, downIdx));
        slice(state);
        scramble += "(".concat(mapIdxToMove(upIdx))
            .concat(",")
            .concat(mapIdxToMove(downIdx))
            .concat(")");
        if (i !== SCRAMBLE_LENGTH - 1 || 0 === generateRandomBetweenZeroAndX(3)) {
            scramble += "/ ";
        }
        if (0 === (i + 1) % movesPerLine && i !== SCRAMBLE_LENGTH - 1) {
            scramble += "\n";
        }
    }
    return scramble;
}