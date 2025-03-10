import { render, screen, within } from "@testing-library/react";
import * as constants from "../../constants";
import { SCRAMBLE_LENGTH as FIVE_SCRAMBLE_LENGTH } from "./scramblers/fiveScrambler";
import { FMC_PREFIX, SCRAMBLE_LENGTH as FMC_SCRAMBLE_LENGTH } from "./scramblers/fmcScrambler";
import { SCRAMBLE_LENGTH as FOUR_SCRAMBLE_LENGTH } from "./scramblers/fourScrambler";
import { SCRAMBLE_LENGTH as MEGAMINX_SCRAMBLE_LENGTH } from "./scramblers/megaScrambler";
import { SCRAMBLE_LENGTH as PYRAMINX_SCRAMBLE_LENGTH } from "./scramblers/pyraScrambler";
import Scramble from "./scramblers/Scramble";
import { SCRAMBLE_LENGTH as SEVEN_SCRAMBLE_LENGTH } from "./scramblers/sevenScrambler";
import { SCRAMBLE_LENGTH as SIX_SCRAMBLE_LENGTH } from "./scramblers/sixScrambler";
import { SCRAMBLE_LENGTH as SKEWB_SCRAMBLE_LENGTH } from "./scramblers/skewbScrambler";
import { SCRAMBLE_LENGTH as SQUARE_SCRAMBLE_LENGTH } from "./scramblers/sqoneScrambler";
import { SCRAMBLE_LENGTH as THREE_SCRAMBLE_LENGTH } from "./scramblers/threeScrambler";
import { SCRAMBLE_LENGTH as TWO_SCRAMBLE_LENGTH } from "./scramblers/twoScrambler";
import { renderStats } from "./statsHelper";

const NUMBER_OF_RUNS = 1000;
const LIKELINESS_RELATIVE_ERROR = 0.05

const checkThreeScramble = (axisMoves) => {
	expect(axisMoves).not.toContain("UU");
	expect(axisMoves).not.toContain("DD");
	expect(axisMoves).not.toContain("FF");
	expect(axisMoves).not.toContain("BB");
	expect(axisMoves).not.toContain("RR");
	expect(axisMoves).not.toContain("LL");
	expect(axisMoves).not.toContain("UDU");
	expect(axisMoves).not.toContain("DUD");
	expect(axisMoves).not.toContain("FBF");
	expect(axisMoves).not.toContain("BFB");
	expect(axisMoves).not.toContain("RLR");
	expect(axisMoves).not.toContain("LRL");
};

test("3x3", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.THREE} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[2'\s]/g, "");
		expect(axisMoves.length).toBe(THREE_SCRAMBLE_LENGTH);
		checkThreeScramble(axisMoves);
	}
});

test("2x2", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.TWO} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[2'\s]/g, "");
		expect(axisMoves.length).toBe(TWO_SCRAMBLE_LENGTH);
		expect(axisMoves).not.toContain("UU");
		expect(axisMoves).not.toContain("FF");
		expect(axisMoves).not.toContain("RR");
	}
});

const checkFourScramble = (sanitizedMoves) => {
	expect(sanitizedMoves).not.toContain(" U U ");
	expect(sanitizedMoves).not.toContain(" D D ");
	expect(sanitizedMoves).not.toContain(" F F ");
	expect(sanitizedMoves).not.toContain(" B B ");
	expect(sanitizedMoves).not.toContain(" R R ");
	expect(sanitizedMoves).not.toContain(" L L ");
	expect(sanitizedMoves).not.toContain(" U D U ");
	expect(sanitizedMoves).not.toContain(" D U D ");
	expect(sanitizedMoves).not.toContain(" F B F ");
	expect(sanitizedMoves).not.toContain(" B F B ");
	expect(sanitizedMoves).not.toContain(" R L R ");
	expect(sanitizedMoves).not.toContain(" L R L ");
	expect(sanitizedMoves).not.toContain(" Uw Uw ");
	expect(sanitizedMoves).not.toContain(" Dw Dw ");
	expect(sanitizedMoves).not.toContain(" Fw Fw ");
	expect(sanitizedMoves).not.toContain(" Bw Bw ");
	expect(sanitizedMoves).not.toContain(" Rw Rw ");
	expect(sanitizedMoves).not.toContain(" Lw Lw ");
	expect(sanitizedMoves).not.toContain(" U Dw U ");
	expect(sanitizedMoves).not.toContain(" U Uw U ");
	expect(sanitizedMoves).not.toContain(" D Dw D ");
	expect(sanitizedMoves).not.toContain(" D Uw D ");
	expect(sanitizedMoves).not.toContain(" F Bw F ");
	expect(sanitizedMoves).not.toContain(" F Fw F ");
	expect(sanitizedMoves).not.toContain(" B Fw B ");
	expect(sanitizedMoves).not.toContain(" B Bw B ");
	expect(sanitizedMoves).not.toContain(" R Lw R ");
	expect(sanitizedMoves).not.toContain(" R Rw R ");
	expect(sanitizedMoves).not.toContain(" L Rw L ");
	expect(sanitizedMoves).not.toContain(" L Lw L ");
	expect(sanitizedMoves).not.toContain(" Uw D Uw ");
	expect(sanitizedMoves).not.toContain(" Uw U Uw ");
	expect(sanitizedMoves).not.toContain(" Dw D Dw ");
	expect(sanitizedMoves).not.toContain(" Dw U Dw ");
	expect(sanitizedMoves).not.toContain(" Fw B Fw ");
	expect(sanitizedMoves).not.toContain(" Fw F Fw ");
	expect(sanitizedMoves).not.toContain(" Bw F Bw ");
	expect(sanitizedMoves).not.toContain(" Bw B Bw ");
	expect(sanitizedMoves).not.toContain(" Rw L Rw ");
	expect(sanitizedMoves).not.toContain(" Rw R Rw ");
	expect(sanitizedMoves).not.toContain(" Lw R Lw ");
	expect(sanitizedMoves).not.toContain(" Lw L Lw ");
};

test("4x4", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.FOUR} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[2'\sw]/g, "");
		expect(axisMoves.length).toBe(FOUR_SCRAMBLE_LENGTH);
		const numberOfWideMoves = scramble.match(/w/g).length;
		expect(numberOfWideMoves).toBe(TWO_SCRAMBLE_LENGTH);
		const sanitizedMoves = scramble.replace(/[2']/g, "");
		checkFourScramble(sanitizedMoves);
		expect(sanitizedMoves).not.toContain(" Uw Dw ");
		expect(sanitizedMoves).not.toContain(" Dw Uw ");
		expect(sanitizedMoves).not.toContain(" Fw Bw ");
		expect(sanitizedMoves).not.toContain(" Bw Fw ");
		expect(sanitizedMoves).not.toContain(" Rw Lw ");
		expect(sanitizedMoves).not.toContain(" Lw Rw ");
	}
});

test("5x5", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.FIVE} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[2'\sw]/g, "");
		expect(axisMoves.length).toBe(FIVE_SCRAMBLE_LENGTH);
		const sanitizedMoves = scramble.replace(/[2']/g, "");
		checkFourScramble(sanitizedMoves);
	}
});

const checkSixScramble = (sanitizedMoves) => {
	checkFourScramble(sanitizedMoves);
	expect(sanitizedMoves).not.toContain(" 3Uw 3UW ");
	expect(sanitizedMoves).not.toContain(" 3Dw 3DW ");
	expect(sanitizedMoves).not.toContain(" 3Fw 3FW ");
	expect(sanitizedMoves).not.toContain(" 3Bw 3BW ");
	expect(sanitizedMoves).not.toContain(" 3Rw 3RW ");
	expect(sanitizedMoves).not.toContain(" 3Lw 3LW ");
	expect(sanitizedMoves).not.toContain(" 3Lw L 3Lw ");
	expect(sanitizedMoves).not.toContain(" 3Lw Lw 3Lw ");
	expect(sanitizedMoves).not.toContain(" 3Lw R 3Lw ");
	expect(sanitizedMoves).not.toContain(" 3Lw Rw 3Lw ");
	expect(sanitizedMoves).not.toContain(" 3Lw 3Rw 3Lw ");
	expect(sanitizedMoves).not.toContain(" L Lw 3Lw L ");
	expect(sanitizedMoves).not.toContain(" L 3Lw Lw L");
	expect(sanitizedMoves).not.toContain(" L R Rw L ");
	expect(sanitizedMoves).not.toContain(" L R 3Rw L ");
	expect(sanitizedMoves).not.toContain(" L R 3Lw L ");
	expect(sanitizedMoves).not.toContain(" L R Lw L ");
	expect(sanitizedMoves).not.toContain(" L Rw R L ");
	expect(sanitizedMoves).not.toContain(" L Rw 3Rw L ");
	expect(sanitizedMoves).not.toContain(" L Rw 3Lw L ");
	expect(sanitizedMoves).not.toContain(" L Rw Lw L ");
	expect(sanitizedMoves).not.toContain(" L 3Rw R L ");
	expect(sanitizedMoves).not.toContain(" L 3Rw Rw L ");
	expect(sanitizedMoves).not.toContain(" L 3Rw 3Lw L ");
	expect(sanitizedMoves).not.toContain(" L 3Rw Lw L ");
	expect(sanitizedMoves).not.toContain(" L 3Lw R L ");
	expect(sanitizedMoves).not.toContain(" L 3Lw Rw L ");
	expect(sanitizedMoves).not.toContain(" L 3Lw 3Rw L ");
	expect(sanitizedMoves).not.toContain(" L 3Lw Lw L ");
	expect(sanitizedMoves).not.toContain(" L Lw R L ");
	expect(sanitizedMoves).not.toContain(" L Lw Rw L ");
	expect(sanitizedMoves).not.toContain(" L Lw 3Rw L ");
	expect(sanitizedMoves).not.toContain(" L Lw 3Lw L ");
};

test("6x6", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.SIX} display="block" quantity={0} ></Scramble>)
	}
	let simpleMoves = 0;
	let doubleMoves = 0;
	let tripleMoves = 0;
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[32'\sw]/g, "");
		expect(axisMoves.length).toBe(SIX_SCRAMBLE_LENGTH);
		const sanitizedMoves = scramble.replace(/[2']/g, "");
		checkSixScramble(sanitizedMoves);
		expect(sanitizedMoves).not.toContain(" 3Uw 3Dw ");
		expect(sanitizedMoves).not.toContain(" 3Dw 3Uw ");
		expect(sanitizedMoves).not.toContain(" 3Fw 3Bw ");
		expect(sanitizedMoves).not.toContain(" 3Bw 3Fw ");
		expect(sanitizedMoves).not.toContain(" 3Rw 3Lw ");
		expect(sanitizedMoves).not.toContain(" 3Lw 3Rw ");
		const tripleMovesInstance = scramble.match(/3/g).length;
		const doubleMovesInstance = scramble.match(/w/g).length - tripleMovesInstance;
		const simpleMovesInstance = SIX_SCRAMBLE_LENGTH - (tripleMovesInstance + doubleMovesInstance);
		simpleMoves += simpleMovesInstance;
		doubleMoves += doubleMovesInstance;
		tripleMoves += tripleMovesInstance;
	}
	expect(simpleMoves / NUMBER_OF_RUNS).toBeGreaterThan(SIX_SCRAMBLE_LENGTH * (2 / 5) - LIKELINESS_RELATIVE_ERROR * (200 / 5));
	expect(simpleMoves / NUMBER_OF_RUNS).toBeLessThan(SIX_SCRAMBLE_LENGTH * (2 / 5) + LIKELINESS_RELATIVE_ERROR * (200 / 5));
	expect(doubleMoves / NUMBER_OF_RUNS).toBeGreaterThan(SIX_SCRAMBLE_LENGTH * (2 / 5) - LIKELINESS_RELATIVE_ERROR * (200 / 5));
	expect(doubleMoves / NUMBER_OF_RUNS).toBeLessThan(SIX_SCRAMBLE_LENGTH * (2 / 5) + LIKELINESS_RELATIVE_ERROR * (200 / 5));
	expect(tripleMoves / NUMBER_OF_RUNS).toBeGreaterThan(SIX_SCRAMBLE_LENGTH * (1 / 5) - LIKELINESS_RELATIVE_ERROR * (100 / 5));
	expect(tripleMoves / NUMBER_OF_RUNS).toBeLessThan(SIX_SCRAMBLE_LENGTH * (1 / 5) + LIKELINESS_RELATIVE_ERROR * (100 / 5));
});

test("7X7", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.SEVEN} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[32'\sw]/g, "");
		expect(axisMoves.length).toBe(SEVEN_SCRAMBLE_LENGTH);
		const sanitizedMoves = scramble.replace(/[2']/g, "");
		checkSixScramble(sanitizedMoves);
	}
});

const checkBldScramble = (scramble, numberOfWideMoves) => {
	const spinCountLessNormalizedMoves = scramble.replace(/[2'\s]/g, "");
	const axisMoves = spinCountLessNormalizedMoves.replace(/w/g, "");
	const nonWideAxisMoves = axisMoves.slice(0, -(numberOfWideMoves));
	const wideMoves = numberOfWideMoves === 0
		? ""
		: numberOfWideMoves === 1
			? axisMoves.slice(-1) :
			axisMoves.slice(-2);
	const widthSplitMoves = numberOfWideMoves === 0
		? ""
		: numberOfWideMoves === 1
			? axisMoves.slice(-2)
			: axisMoves.slice(-3, -1);
	expect(numberOfWideMoves).not.toBeGreaterThan(2);
	expect(axisMoves.length).toBe((THREE_SCRAMBLE_LENGTH) + numberOfWideMoves);
	checkThreeScramble(nonWideAxisMoves);
	expect(spinCountLessNormalizedMoves).not.toContain("UDDw");
	expect(spinCountLessNormalizedMoves).not.toContain("DUUw");
	expect(spinCountLessNormalizedMoves).not.toContain("FBBw");
	expect(spinCountLessNormalizedMoves).not.toContain("BBFw");
	expect(spinCountLessNormalizedMoves).not.toContain("RLLw");
	expect(spinCountLessNormalizedMoves).not.toContain("LRRw");
	expect(wideMoves).not.toBe("UU");
	expect(wideMoves).not.toBe("UD");
	expect(wideMoves).not.toBe("DU");
	expect(wideMoves).not.toBe("DD");
	expect(wideMoves).not.toBe("FF");
	expect(wideMoves).not.toBe("FB");
	expect(wideMoves).not.toBe("BF");
	expect(wideMoves).not.toBe("BB");
	expect(wideMoves).not.toBe("RR");
	expect(wideMoves).not.toBe("RL");
	expect(wideMoves).not.toBe("LR");
	expect(wideMoves).not.toBe("LL");
	expect(widthSplitMoves).not.toBe("UD");
	expect(widthSplitMoves).not.toBe("DU");
	expect(widthSplitMoves).not.toBe("FB");
	expect(widthSplitMoves).not.toBe("BF");
	expect(widthSplitMoves).not.toBe("RL");
	expect(widthSplitMoves).not.toBe("LR");
};

test("BLD", () => {
	let scramblesWIthNoWideMoves = 0;
	let ScramblesWIthOneWideMove = 0;
	let ScramblesWithTwoWideMoves = 0;
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.BLD} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const numberOfWideMoves = (scramble.match(/w/g) || []).length;
		checkBldScramble(scramble, numberOfWideMoves);
		if (numberOfWideMoves === 0) {
			scramblesWIthNoWideMoves++;
		} else if (numberOfWideMoves === 1) {
			ScramblesWIthOneWideMove++;
		} else if (numberOfWideMoves === 2) {
			ScramblesWithTwoWideMoves++;
		}
	}
	expect(scramblesWIthNoWideMoves / NUMBER_OF_RUNS).toBeGreaterThan((1 / 6) * (1 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWIthNoWideMoves / NUMBER_OF_RUNS).toBeLessThan((1 / 6) * (1 / 4) + LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWIthOneWideMove / NUMBER_OF_RUNS).toBeGreaterThan((1 / 6) * (3 / 4) + (5 / 6) * (1 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWIthOneWideMove / NUMBER_OF_RUNS).toBeLessThan((1 / 6) * (3 / 4) + (5 / 6) * (1 / 4) + LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWithTwoWideMoves / NUMBER_OF_RUNS).toBeGreaterThan((5 / 6) * (3 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWithTwoWideMoves / NUMBER_OF_RUNS).toBeLessThan((5 / 6) * (3 / 4) + LIKELINESS_RELATIVE_ERROR);
});

test("fmc", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.FMC} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		expect(scramble.startsWith(FMC_PREFIX)).toBe(true);
		expect(scramble.endsWith(FMC_PREFIX.trim())).toBe(true);
		const axisMoves = scramble.replace(/[2'\s]/g, "");
		expect(axisMoves.length).toBe(FMC_SCRAMBLE_LENGTH);
		checkThreeScramble(axisMoves);
	}
});

test("megaminx", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.MEGAMINX} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/[-+'\s]/g, "");
		expect(axisMoves.length).toBe(MEGAMINX_SCRAMBLE_LENGTH);
		expect(axisMoves.startsWith("R")).toBe(true);
		expect(axisMoves).not.toContain("UU");
		expect(axisMoves).not.toContain("RR");
		expect(axisMoves).not.toContain("DD");
	}
});

const checkSkewbScramble = (axisMoves) => {
	expect(axisMoves).not.toContain("UU");
	expect(axisMoves).not.toContain("BB");
	expect(axisMoves).not.toContain("RR");
	expect(axisMoves).not.toContain("LL");
};

test("pyraminx", () => {
	let scramblesWithNoTips = 0;
	let scramblesWithOneTip = 0;
	let scramblesWithTwoTips = 0;
	let scramblesWithThreeTips = 0;
	let scramblesWithFourTips = 0;
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.PYRAMINX} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/['\s]/g, "");
		const numberOfTipsScrambled = (scramble.match(/[ubrl]/g) || []).length;
		expect(numberOfTipsScrambled).not.toBeGreaterThan(4);
		expect(axisMoves.length).toBe(PYRAMINX_SCRAMBLE_LENGTH + numberOfTipsScrambled);
		checkSkewbScramble(axisMoves);
		if (numberOfTipsScrambled === 0) {
			scramblesWithNoTips++;
		} else if (numberOfTipsScrambled === 1) {
			scramblesWithOneTip++;
		} else if (numberOfTipsScrambled === 2) {
			scramblesWithTwoTips++;
		} else if (numberOfTipsScrambled === 3) {
			scramblesWithThreeTips++;
		} else if (numberOfTipsScrambled === 4) {
			scramblesWithFourTips++;
		}
	}
	expect(scramblesWithNoTips / NUMBER_OF_RUNS).toBeGreaterThan((1 / 3) ** 4 - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithNoTips / NUMBER_OF_RUNS).toBeLessThan((1 / 3) ** 4 + LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithOneTip / NUMBER_OF_RUNS).toBeGreaterThan(4 * ((2 / 3) * (1 / 3) ** 3) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithOneTip / NUMBER_OF_RUNS).toBeLessThan(4 * ((2 / 3) * (1 / 3) ** 3) + LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithTwoTips / NUMBER_OF_RUNS).toBeGreaterThan(4 * ((2 / 3) * (1 / 3) ** 2) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithTwoTips / NUMBER_OF_RUNS).toBeLessThan(4 * ((2 / 3) * (1 / 3) ** 2) + LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithThreeTips / NUMBER_OF_RUNS).toBeGreaterThan(4 * ((1 / 3) * (2 / 3) ** 3) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithThreeTips / NUMBER_OF_RUNS).toBeLessThan(4 * ((1 / 3) * (2 / 3) ** 3) + LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithFourTips / NUMBER_OF_RUNS).toBeGreaterThan(((2 / 3) ** 4) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWithFourTips / NUMBER_OF_RUNS).toBeLessThan(((2 / 3) ** 4) + LIKELINESS_RELATIVE_ERROR);
});

test("skewb", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.SKEWB} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const axisMoves = scramble.replace(/['\s]/g, "");
		expect(axisMoves.length).toBe(SKEWB_SCRAMBLE_LENGTH);
		checkSkewbScramble(axisMoves);
	}
});

test("sqone", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.SQUARE} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		expect(scramble.split("(").length - 1).toBe(SQUARE_SCRAMBLE_LENGTH);
		expect(scramble).not.toContain("NaN");
		expect(scramble).not.toContain("-6");
		expect(scramble).not.toContain("0, 0");
		const allButFirstThree = scramble.split("/").slice(3).join("/");
		expect(allButFirstThree.split("(").length - 1).toBe(SQUARE_SCRAMBLE_LENGTH - 3);
		expect(allButFirstThree).not.toContain(", 1");
		expect(allButFirstThree).not.toContain(", 2");
		expect(allButFirstThree).not.toContain(", 3");
		expect(allButFirstThree).not.toContain(", 4");
		expect(allButFirstThree).not.toContain(", 5");
		expect(allButFirstThree).not.toContain(", 6");
	}
})

test("5BLD", () => {
	for (let i = 0; i < NUMBER_OF_RUNS; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.FIVE_BLD} display="block" quantity={0} ></Scramble>)
	}
	for (let scramble of screen.getAllByTestId("scramble").map(scramble => scramble.textContent)) {
		const sanitizedMoves = scramble.replace(/[2']/g, "");
		checkFourScramble(sanitizedMoves);
		expect(sanitizedMoves).not.toContain("3Uw 3Uw");
		expect(sanitizedMoves).not.toContain("3Dw 3Dw");
		expect(sanitizedMoves).not.toContain("3Fw 3Fw");
		expect(sanitizedMoves).not.toContain("3Bw 3Bw");
		expect(sanitizedMoves).not.toContain("3Rw 3Rw");
		expect(sanitizedMoves).not.toContain("3Lw 3Lw");
		expect(sanitizedMoves).not.toContain("Uw 3Dw");
		expect(sanitizedMoves).not.toContain("Dw 3Uw");
		expect(sanitizedMoves).not.toContain("Fw 3Bw");
		expect(sanitizedMoves).not.toContain("Bw 3Fw");
		expect(sanitizedMoves).not.toContain("Rw 3Lw");
		expect(sanitizedMoves).not.toContain("Lw 3Rw");
	}
});

test("multi", () => {
	let scramblesWIthNoWideMoves = 0;
	let ScramblesWIthOneWideMove = 0;
	let ScramblesWithTwoWideMoves = 0;
	const SCRAMBLES_PER_BATCH = 5
	for (let i = 0; i < NUMBER_OF_RUNS / SCRAMBLES_PER_BATCH; i++) {
		render(<Scramble isNewScramble={true} onScrambleChange={() => { }} puzzle={constants.MULTI} display="block" quantity={SCRAMBLES_PER_BATCH} ></Scramble>)
	}
	for (let scrambles of screen.getAllByTestId("scramble")) {
		const specificScrambles = within(scrambles).getAllByText(text => text.includes(")")).map(scramble => scramble.textContent);
		expect(specificScrambles.length).toBe(SCRAMBLES_PER_BATCH);
		for (let i = 0; i < specificScrambles.length; i++) {
			const header = (i + 1).toString().concat(") ")
			expect(specificScrambles[i]).toContain(header);
			const specificScramble = specificScrambles[i].replace(header, "");
			const numberOfWideMoves = (specificScramble.match(/w/g) || []).length;
			checkBldScramble(specificScramble, numberOfWideMoves);
			if (numberOfWideMoves === 0) {
				scramblesWIthNoWideMoves++;
			} else if (numberOfWideMoves === 1) {
				ScramblesWIthOneWideMove++;
			} else if (numberOfWideMoves === 2) {
				ScramblesWithTwoWideMoves++;
			}
		}
	}
	expect(scramblesWIthNoWideMoves / NUMBER_OF_RUNS).toBeGreaterThan((1 / 6) * (1 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(scramblesWIthNoWideMoves / NUMBER_OF_RUNS).toBeLessThan((1 / 6) * (1 / 4) + LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWIthOneWideMove / NUMBER_OF_RUNS).toBeGreaterThan((1 / 6) * (3 / 4) + (5 / 6) * (1 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWIthOneWideMove / NUMBER_OF_RUNS).toBeLessThan((1 / 6) * (3 / 4) + (5 / 6) * (1 / 4) + LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWithTwoWideMoves / NUMBER_OF_RUNS).toBeGreaterThan((5 / 6) * (3 / 4) - LIKELINESS_RELATIVE_ERROR);
	expect(ScramblesWithTwoWideMoves / NUMBER_OF_RUNS).toBeLessThan((5 / 6) * (3 / 4) + LIKELINESS_RELATIVE_ERROR);
});

test("Render Averages", () => {
	const times = [484, 148567, 4847, 61874, 848975, 1877894, 157, 87974, 4876, 15879, 189687, 489751, 48976, 487, 7888];
	const { container } = render(renderStats({ times }));
	expect(container.innerHTML).toContain("best 0:00:157");
	expect(container.innerHTML).toContain("mo3 0:19:117");
	expect(container.innerHTML).toContain("avg5 1:22:183");
	expect(container.innerHTML).toContain("avg12 2:55:636");
	expect(container.innerHTML).toContain("mo50 " + constants.EMPTY_TIMER);
	expect(container.innerHTML).toContain("mo100 " + constants.EMPTY_TIMER);
});

test("Render incomplete averages", () => {
	const times = [148567, 484, 4847, 61874];
	const { container } = render(renderStats({ times }));
	expect(container.innerHTML).toContain("mean 0:53:943");
	expect(container.innerHTML).toContain("median 0:33:360");
	expect(container.innerHTML).toContain("last 1:01:874");
	expect(container.innerHTML).toContain("best 0:00:484");
	expect(container.innerHTML).toContain("worst 2:28:567");
	expect(container.innerHTML).toContain("current mo3 0:22:401");
	expect(container.innerHTML).toContain("best mo3 0:22:401");
	expect(container.innerHTML).toContain("worst mo3 0:51:299");
	expect(container.innerHTML).toContain("current avg5 1:11:762");
	expect(container.innerHTML).toContain("best avg5 1:11:762");
	expect(container.innerHTML).toContain("worst avg5 1:11:762");
	expect(container.innerHTML).toContain("avg12 " + constants.EMPTY_TIMER);
	expect(container.innerHTML).toContain("mo50 " + constants.EMPTY_TIMER);
	expect(container.innerHTML).toContain("mo100 " + constants.EMPTY_TIMER);
});

test("Render worst best and current averages", () => {
	const times = [6000, 7000, 5, 4, 6, 5000, 4000, 3000, 11, 5];
	const { container } = render(renderStats({ times }));
	expect(container.innerHTML).toContain("current mo3 0:01:005");
	expect(container.innerHTML).toContain("best mo3 0:00:005");
	expect(container.innerHTML).toContain("worst mo3 0:04:335");
	expect(container.innerHTML).toContain("current avg5 0:02:337");
	expect(container.innerHTML).toContain("best avg5 0:01:337");
	expect(container.innerHTML).toContain("worst avg5 0:02:337");
});
