import { PropTypes } from "prop-types";
import React, { useEffect, useState } from "react";
import * as constants from "../../../constants";
import { generateRandomBetweenZeroAndX } from "../../../constants";
import { Scramble as bldScrambler } from "./bldScrambler";
import { Scramble as clockScrambler } from "./clockScrambler";
import { Scramble as fiveBldScrambler } from "./fiveBldScrambler";
import { Scramble as fiveScrambler } from "./fiveScrambler";
import { Scramble as fmcScrambler } from "./fmcScrambler";
import { Scramble as fourBldScrambler } from "./fourBldScrambler";
import { Scramble as fourScrambler } from "./fourScrambler";
import { Scramble as megaScrambler } from "./megaScrambler";
import { Scramble as pyraminxScrambler } from "./pyraScrambler";
import { Scramble as sevenScrambler } from "./sevenScrambler";
import { Scramble as sixScrambler } from "./sixScrambler";
import { Scramble as skewbScrambler } from "./skewbScrambler";
import { Scramble as sqoneScrambler } from "./sqoneScrambler";
import { Scramble as threeScrambler } from "./threeScrambler";
import { Scramble as twoScrambler } from "./twoScrambler";

const scramblerMap = {
    [constants.THREE]: threeScrambler,
    [constants.TWO]: twoScrambler,
    [constants.FOUR]: fourScrambler,
    [constants.FIVE]: fiveScrambler,
    [constants.SIX]: sixScrambler,
    [constants.SEVEN]: sevenScrambler,
    [constants.BLD]: bldScrambler,
    [constants.FMC]: fmcScrambler,
    [constants.OH]: threeScrambler,
    [constants.CLOCK]: clockScrambler,
    [constants.MEGAMINX]: megaScrambler,
    [constants.PYRAMINX]: pyraminxScrambler,
    [constants.SKEWB]: skewbScrambler,
    [constants.SQUARE]: sqoneScrambler,
    [constants.FOUR_BLD]: fourBldScrambler,
    [constants.FIVE_BLD]: fiveBldScrambler,
};

const Scramble = ({ isNewScramble, onScrambleChange, puzzle, display, quantity, isHorizontal }) => {
    const [scramble, setScramble] = useState("");

    useEffect(() => {
        if (isNewScramble && display === "block") {
            let newScramble;
            if (puzzle === constants.MULTI) {
                const scrambles = [];
                for (let i = 0; i < quantity; i++) {
                    scrambles.push(bldScrambler().trim());
                }
                newScramble = scrambles;
            } else {
                const scrambler = scramblerMap[puzzle];
                if (scrambler) {
                    newScramble = puzzle === constants.SQUARE ? scrambler(isHorizontal).trim() : scrambler().trim();
                }
            }
            setScramble(newScramble);
            onScrambleChange(newScramble);
        }
    }, [display, isHorizontal, isNewScramble, onScrambleChange, puzzle, quantity]);

    useEffect(() => {
        if (!isNewScramble && puzzle === constants.SQUARE) {
            const movesPerLine = isHorizontal ? 5 : 3;
            const moves = scramble.replace(/(\r\n|\n|\r)/g, "").split("/ ");
            for (let i = 0; i < moves.length - 1; i++) {
                moves[i] += "/";
                if (0 === (i + 1) % movesPerLine) {
                    moves[i] += "\n";
                }
            }
            setScramble(moves.join(" "));
        }
    }, [isHorizontal, isNewScramble, puzzle, scramble])

    return (
        <h2 data-testid="scramble" className={puzzle} style={{ display: display }}>
            {Array.isArray(scramble)
                ? scramble.map((s, index) => <p key={index}>{index + 1}{")"} {s}</p>)
                : scramble}
        </h2>
    );
};

export const SCRAMBLE_MOVES = [[
    ["D ", "D2 ", "D' "],
    ["U ", "U2 ", "U' "],
    ["Dw ", "Dw2 ", "Dw' "],
    ["Uw ", "Uw2 ", "Uw' "],
    ["3Dw ", "3Dw2 ", "3Dw' "],
    ["3Uw ", "3Uw2 ", "3Uw' "],
], [
    ["B ", "B2 ", "B' "],
    ["F ", "F2 ", "F' "],
    ["Bw ", "Bw2 ", "Bw' "],
    ["Fw ", "Fw2 ", "Fw' "],
    ["3Bw ", "3Bw2 ", "3Bw' "],
    ["3Fw ", "3Fw2 ", "3Fw' "],
], [
    ["L ", "L2 ", "L' "],
    ["R ", "R2 ", "R' "],
    ["Lw ", "Lw2 ", "Lw' "],
    ["Rw ", "Rw2 ", "Rw' "],
    ["3Lw ", "3Lw2 ", "3Lw' "],
    ["3Rw ", "3Rw2 ", "3Rw' "],
]];

export const generateMove = (availabilityMatrix, width = null) => {
    const validMoves = [];
    const [layerStart, layerEnd] = width !== null
        ? [width * 2, width * 2 + 2]
        : [0, availabilityMatrix[0].length];
    for (let axis = 0; axis < availabilityMatrix.length; axis++) {
        for (let layer = layerStart; layer < layerEnd; layer++) {
            if (availabilityMatrix[axis][layer]) {
                validMoves.push({ axis, layer });
            }
        }
    }
    if (validMoves.length === 0) {
        console.log("ERROR: No valid moves available");
        return null;
    }
    return validMoves[generateRandomBetweenZeroAndX(validMoves.length)];
};

export const updateAvailabilityMatrix = (matrix, move, previous) => {
    matrix[move.axis][move.layer] = false;
    if (previous) {
        const previousAxis = previous.axis;
        if (previousAxis !== move.axis) {
            for (let i = 0; i < matrix[previousAxis].length; i++) {
                matrix[previousAxis][i] = true;
            }
        }
    }
};

Scramble.propTypes = {
    isNewScramble: PropTypes.bool,
    onScrambleChange: PropTypes.func,
    puzzle: PropTypes.string,
    display: PropTypes.string,
    quantity: PropTypes.number,
    isHorizontal: PropTypes.bool
};

export default Scramble;
