import { generateRandomBetweenZeroAndX } from "../../../constants";

export const SCRAMBLE_LENGTH = 15;
const INITIAL_STATE = [[0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2], [1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0]];

export const Scramble = () => {

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

    const mapIdxToMove = (idx, isSecond = false) => {
        return idx < 7 ? isSecond ? " ".concat(idx.toString()) : idx.toString() : (idx - 12).toString();
    };

    let scramble = "";
    let upIdx;
    let downIdx;
    let availableIndexes;
    let state = INITIAL_STATE.map(row => row.slice());
    for (let i = 0; i < SCRAMBLE_LENGTH - 1; i++) {
        availableIndexes = getAvailableIndexes(state);
        upIdx = availableIndexes[0][generateRandomBetweenZeroAndX(availableIndexes[0].length)];
        if (upIdx === 0) {
            availableIndexes[1].shift();
        }
        downIdx = availableIndexes[1][generateRandomBetweenZeroAndX(availableIndexes[1].length)];
        state[0] = state[0].slice(upIdx).concat(state[0].slice(0, upIdx));
        state[1] = state[1].slice(downIdx).concat(state[1].slice(0, downIdx));
        slice(state);
        scramble += "(".concat(mapIdxToMove(upIdx))
            .concat(",")
            .concat(mapIdxToMove(downIdx, true))
            .concat(")/ ");
        if (0 === (i + 1) % 3) {
            scramble += "\n";
        }
    }
    availableIndexes = getAvailableIndexes(state);
    upIdx = availableIndexes[0][generateRandomBetweenZeroAndX(availableIndexes[0].length)];
    downIdx = availableIndexes[1][generateRandomBetweenZeroAndX(availableIndexes[1].length)];
    scramble += "(".concat(mapIdxToMove(upIdx))
        .concat(",")
        .concat(mapIdxToMove(downIdx, true))
        .concat(")");
    if (0 === generateRandomBetweenZeroAndX(3)) {
        scramble = scramble.concat("/ ");
    }
    return scramble;
}