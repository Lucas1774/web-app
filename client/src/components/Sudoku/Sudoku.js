import React, { useCallback, useEffect, useRef, useState } from "react";
import { Button, Form } from "react-bootstrap";
import { get, post } from "../../api";
import { TIMEOUT_DELAY } from "../../constants";
import { handleError } from "../errorHandler";
import FileImporter from "../FileImporter";
import Spinner from "../Spinner";
import "./Sudoku.css";
import SudokuGrid from "./SudokuGrid";

const Sudoku = () => {
    const [sudoku, setSudoku] = useState("");
    const [initialSudoku, setInitialSudoku] = useState("");
    const [difficulty, setDifficulty] = useState(1);
    const [isGenerateOrImportVisible, setIsGenerateOrImportVisible] = useState(true);
    const [isUploadResponseVisible, setIsUploadResponseVisible] = useState(false);
    const [isPickDifficultyVisible, setIsPickDifficultyVisible] = useState(false);
    const [isSudokuVisible, setIsSudokuVisible] = useState(false);
    const [isRestartButtonVisible, setIsRestartButtonVisible] = useState(true);
    const [isLoading, setIsLoading] = useState(false);

    const responseMessage = useRef("");
    const formRef = useRef(null);
    const gridRef = useRef(null);
    const wakeLock = useRef(null);

    const restoreDefaults = useCallback(() => {
        setSudoku("");
        setInitialSudoku("");
        setDifficulty(1);
        setIsGenerateOrImportVisible(true);
        setIsUploadResponseVisible(false);
        setIsPickDifficultyVisible(false);
        setIsSudokuVisible(false);
        setIsRestartButtonVisible(true);
        responseMessage.current = "";
    }, []);

    const hideEverything = useCallback(() => {
        setIsGenerateOrImportVisible(false);
        setIsUploadResponseVisible(false);
        setIsPickDifficultyVisible(false);
        setIsSudokuVisible(false);
        setIsRestartButtonVisible(false);
    }, []);

    const isSolved = useCallback(() => {
        return sudoku.indexOf("0") === -1;
    }, [sudoku]);

    const check = useCallback(() => {
        const showBorderThenRemove = async (grid, color) => {
            let numberOfFlashes = color === "green" && isSolved() ? 3 : 1;
            for (let i = 0; i < numberOfFlashes; i++) {
                grid.classList.add(`${color}-border`);
                await new Promise(resolve => setTimeout(resolve, 500));
                grid.classList.remove(`${color}-border`);
                await new Promise(resolve => setTimeout(resolve, 200));
            }
        }
        get(`/sudoku/check/sudoku?initialSudoku=${initialSudoku}&currentSudoku=${sudoku}`)
            .then(response => {
                if (response.data === 0 || response.data === 1) {
                    let color = response.data === 1 ? "green" : "red";
                    let grid = gridRef.current;
                    if (grid !== null) {
                        showBorderThenRemove(grid, color);
                    }
                } else {
                    responseMessage.current = response.data;
                    setIsUploadResponseVisible(true);
                    setTimeout(() => {
                        restoreDefaults();
                    }, TIMEOUT_DELAY);
                }
            })
            .catch(error => {
                handleError("Error sending data", error);
                restoreDefaults();
            });
    }, [initialSudoku, isSolved, restoreDefaults, sudoku]);

    const generateOrFetch = useCallback((generateOrFetch) => {
        let params = generateOrFetch === "generate" ? `?difficulty=${difficulty}` : "";
        hideEverything();
        setIsLoading(true);
        get(`/sudoku/${generateOrFetch}/sudoku${params}`)
            .then(response => {
                if (response.data.match(/^\d{81}$/)) {
                    setSudoku(response.data);
                    setInitialSudoku(response.data);
                    setIsSudokuVisible(true);
                    setIsRestartButtonVisible(true);
                }
                else {
                    responseMessage.current = response.data;
                    setIsUploadResponseVisible(true);
                    setTimeout(() => {
                        restoreDefaults();
                    }, TIMEOUT_DELAY);
                }
            })
            .catch(error => {
                handleError("Error sending data", error);
                restoreDefaults();
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [difficulty, hideEverything, restoreDefaults]);

    useEffect(() => {
        if (sudoku.length === 81 && isSolved() && initialSudoku.indexOf("0") !== -1) {
            check();
        }
    }, [check, initialSudoku, isSolved, sudoku.length]);

    const handleKeyPress = useCallback((event) => {
        if (event.key === "ArrowUp" && difficulty < 9) {
            event.preventDefault();
            setDifficulty(prevDifficulty => prevDifficulty + 1);
        } else if (event.key === "ArrowDown" && difficulty > 1) {
            event.preventDefault();
            setDifficulty(prevDifficulty => prevDifficulty - 1);
        } else if (event.key === "Enter") {
            event.preventDefault();
            generateOrFetch(event.ctrlKey ? "fetch" : "generate");
        } else if (event.key === "Escape") {
            event.preventDefault();
            restoreDefaults();
        }
    }, [difficulty, generateOrFetch, restoreDefaults]);

    useEffect(() => {
        if (isPickDifficultyVisible) {
            document.addEventListener("keydown", handleKeyPress);
            formRef.current.focus();
        } else {
            document.removeEventListener("keydown", handleKeyPress);
        }
        return () => {
            document.removeEventListener("keydown", handleKeyPress);
        };
    }, [handleKeyPress, isPickDifficultyVisible]);

    const solve = useCallback(() => {
        hideEverything();
        setIsLoading(true);
        get(`/sudoku/solve/sudoku?sudoku=${initialSudoku}`)
            .then(response => {
                if (response.data.match(/^\d{81}$/)) {
                    setSudoku(response.data);
                    setInitialSudoku(response.data);
                    setIsSudokuVisible(true);
                    setIsRestartButtonVisible(true);
                }
                else {
                    responseMessage.current = response.data;
                    setIsUploadResponseVisible(true);
                    setTimeout(() => {
                        restoreDefaults();
                    }, TIMEOUT_DELAY);
                }
            })
            .catch(error => {
                handleError("Error sending data", error);
                restoreDefaults();
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [hideEverything, initialSudoku, restoreDefaults]);

    const handleCheckOrSolve = useCallback((event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            if (event.ctrlKey) {
                solve();
            } else {
                check();
            }
        } else if (event.key === "Escape") {
            restoreDefaults();
        }
    }, [check, restoreDefaults, solve]);

    const requestWakeLock = useCallback(async () => {
        if (navigator.wakeLock) {
            try {
                wakeLock.current = await navigator.wakeLock.request("screen");
            } catch (error) {
                console.error(error);
            }
        }
    }, []);

    useEffect(() => {
        if (isSudokuVisible) {
            document.addEventListener("keydown", handleCheckOrSolve);
        } else {
            document.removeEventListener("keydown", handleCheckOrSolve);
        }
        return () => {
            document.removeEventListener("keydown", handleCheckOrSolve);
        };
    }, [handleCheckOrSolve, isSudokuVisible]);

    useEffect(() => {
        if (isSudokuVisible) { // small hack to screen lock after phone unlock
            requestWakeLock();
        } else {
            wakeLock.current?.release();
        }
        return () => {
            wakeLock.current?.release();
        };
    }, [isSudokuVisible, requestWakeLock, sudoku, initialSudoku]); // extra dependencies to trigger the effect

    const handleGenerate = () => {
        hideEverything();
        setIsPickDifficultyVisible(true);
        setIsRestartButtonVisible(true);
    };

    const handleFormInputChange = (event) => {
        let newValue = event.key;
        if (!isNaN(newValue) && parseInt(newValue) >= 1 && parseInt(newValue) <= 9) {
            event.preventDefault();
            setDifficulty(parseInt(newValue));
        }
    };

    const handleSudokuChange = (index, element, event) => {
        event.preventDefault();
        const newValue = (event.key === "Backspace" || event.key === "Delete") ? 0 : parseInt(event.key);
        if (newValue >= 0 && newValue <= 9 && initialSudoku[index] === "0") {
            let auxSudoku = [...sudoku];
            auxSudoku[index] = newValue.toString();
            if (0 !== newValue) {
                element.classList.remove(`white-background`);
                element.classList.add(`blue-background`);
            } else {
                element.classList.remove(`blue-background`);
                element.classList.add(`white-background`);
            }
            setSudoku(auxSudoku.join(""));
        }
    };

    const sendFile = (content) => {
        hideEverything();
        setIsLoading(true);
        post("/sudoku/upload/sudokus", content)
            .then((response) => {
                if (response.data === 1) {
                    responseMessage.current = "Successfully uploaded!";
                } else {
                    responseMessage.current = response.data.toString();
                }
                setIsUploadResponseVisible(true);
                setTimeout(() => {
                    restoreDefaults();
                }, TIMEOUT_DELAY);
            })
            .catch(error => {
                handleError("Error sending data", error);
                restoreDefaults();
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    return (
        <>
            <h1 id="sudoku">Sudoku</h1>
            <div className="app sudoku">
                {isGenerateOrImportVisible &&
                    <>
                        <Button variant="success" onClick={handleGenerate}>Generate</Button>
                        <FileImporter onFileContentChange={sendFile} />
                    </>}
                {isUploadResponseVisible && <div>{responseMessage.current}</div>}
                {isPickDifficultyVisible &&
                    <Form>
                        <Form.Label>Pick difficulty (only for generated sudoku):</Form.Label>
                        <Form.Control inputMode="numeric" value={difficulty} onKeyDown={handleFormInputChange} ref={formRef} onChange={() => { }} />
                        <Button id="generate" variant="success" onClick={(e) => generateOrFetch(e.target.id)}>Generate</Button>
                        <Button id="fetch" onClick={(e) => generateOrFetch(e.target.id)}>Fetch</Button>
                    </Form>}
                {isSudokuVisible &&
                    <><SudokuGrid ref={gridRef} sudokuString={sudoku} onSudokuChange={handleSudokuChange} solved={isSolved} />
                        <Button type="submit" variant="success" onClick={solve}>Solve</Button>
                        <Button onClick={check}>Check</Button></>
                }
                {isLoading && <Spinner />}
                {isRestartButtonVisible && <Button className="restart" onClick={restoreDefaults}>Restart</Button>}
            </div>
        </>
    );
};

export default Sudoku;
