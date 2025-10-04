import PropTypes from "prop-types";
import { createRef, forwardRef, useEffect, useRef, useState } from "react";

const SudokuGrid = forwardRef(({ sudokuString, onSudokuChange, solved }, ref) => {
    const [focusedIndex, setFocusedIndex] = useState(null);
    const [started, setStarted] = useState(false);
    const inputRefs = useRef(Array.from({ length: 81 }, () => createRef()));

    useEffect(() => {
        for (let i = 0; i < 81; i++) {
            if (sudokuString[i] === "0") {
                const element = inputRefs.current[i].current;
                if (element) {
                    element.classList.remove("blue-background");
                    element.classList.add("white-background");
                }
            }
        }
    }, [sudokuString]);

    useEffect(() => {
        if (!solved() && !started) {
            const input = inputRefs.current[0].current;
            input.focus();
            setStarted(true);
        }
    }, [solved, started]);

    useEffect(() => {
        const handleKeyDown = (event) => {
            const arrowKeyMap = {
                ArrowUp: -9,
                ArrowDown: 9,
                ArrowLeft: -1,
                ArrowRight: 1,
            };

            const key = event.key;
            if (key in arrowKeyMap && focusedIndex !== null) {
                event.preventDefault();
                const newIndex = focusedIndex + arrowKeyMap[key];
                if (newIndex >= 0 && newIndex < 81) {
                    const input = inputRefs.current[newIndex].current;
                    if (input) {
                        input.focus();
                        setFocusedIndex(newIndex);
                    }
                }
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [focusedIndex]);

    return (
        <>
            <div className="sudoku-grid" ref={ref}>
                {sudokuString.split("").map((digit, index) => (
                    <span className="sudoku-cell" key={index}>
                        <input
                            onChange={() => { }}
                            ref={inputRefs.current[index]}
                            inputMode="numeric"
                            value={digit === "0" ? "" : digit}
                            onKeyDown={(event) => onSudokuChange(index, inputRefs.current[index].current, event)}
                            onFocus={() => setFocusedIndex(index)}
                            onBlur={() => setFocusedIndex(null)}
                        />
                    </span>
                ))}
            </div>
            <br></br>
        </>
    );
});

SudokuGrid.propTypes = {
    sudokuString: PropTypes.string,
    onSudokuChange: PropTypes.func,
    solved: PropTypes.func
};

export default SudokuGrid;
