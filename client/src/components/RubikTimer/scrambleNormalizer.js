import PropTypes from "prop-types";

const FACES = ["U", "R", "F", "D", "L", "B"];

const OPPOSITE_FACE = { U: "D", D: "U", R: "L", L: "R", F: "B", B: "F" };
const ROTATION_X = { U: "F", F: "D", D: "B", B: "U", R: "R", L: "L" };
const ROTATION_Y = { F: "R", R: "B", B: "L", L: "F", U: "U", D: "D" };
const ROTATION_Z = { U: "L", L: "D", D: "R", R: "U", F: "F", B: "B" };

const parseMove = (token) => ({
    face: token[0],
    suffix: token.endsWith("2") || token.endsWith("'") ? token[token.length - 1] : "",
});

const scrambleNormalizer = (scramble) => {
    const tokens = scramble.trim().split(/\s+/);

    const wideIndices = tokens.reduce((arr, token, index) => {
        if (token.includes("w")) arr.push(index);
        return arr;
    }, []);

    if (wideIndices.length === 0) { // no wide moves
        return scramble;
    }

    const out = [...tokens];
    if (wideIndices.length === 1) { // one wide move
        const wideMoveIndex = wideIndices[0];
        const move = parseMove(tokens[wideMoveIndex]);
        out[wideMoveIndex] = `${OPPOSITE_FACE[move.face]}${move.suffix}`;
    } else { // two wide moves
        const [wideMoveIndex1, wideMoveIndex2] = wideIndices;
        const move1 = parseMove(tokens[wideMoveIndex1]);
        const move2 = parseMove(tokens[wideMoveIndex2]);

        const axis =
            move1.face === "U" || move1.face === "D"
                ? ROTATION_Y
                : move1.face === "R" || move1.face === "L"
                    ? ROTATION_X
                    : ROTATION_Z;

        const turns = move1.suffix === "2" ? 2 : move1.suffix === "'" ? 3 : 1;
        const power = move1.face === "U" || move1.face === "R" || move1.face === "F"
            ? turns
            : (4 - turns) % 4;

        let orientation = { U: "U", R: "R", F: "F", D: "D", L: "L", B: "B" };
        for (let i = 0; i < power; i++) {
            const temp = {};
            for (const face of FACES) {
                temp[face] = axis[orientation[face]];
            }
            orientation = temp;
        }
        out[wideMoveIndex1] = `${OPPOSITE_FACE[move1.face]}${move1.suffix}`;
        out[wideMoveIndex2] = `${orientation[OPPOSITE_FACE[move2.face]]}${move2.suffix}`;
    }

    return out.join(" ");
};

scrambleNormalizer.propTypes = {
    scramble: PropTypes.string.isRequired
};

export default scrambleNormalizer;
