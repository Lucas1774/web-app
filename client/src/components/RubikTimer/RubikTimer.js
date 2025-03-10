import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Form } from "react-bootstrap";
import * as constants from "../../constants";
import Popup from "./Popup";
import "./RubikTimer.css";
import Scramble from "./scramblers/Scramble";
import { formatTime, renderStats } from "./statsHelper";
const RubikTimer = () => {
    // IDENTIFIERS
    const isAndroid = /Android/i.test(navigator.userAgent);

    const [elapsedTime, setElapsedTime] = useState(0);
    const [scramble, setScramble] = useState("");
    const [isTimerPrepared, setIsTimerPrepared] = useState(false);
    const [isTimerRunning, setIsTimerRunning] = useState(false);
    const [isFormVisible, setIsFormVisible] = useState(true);
    const [scrambleDisplayMode, setScrambleDisplayMode] = useState("none");
    const [isTimerVisible, setIsTimerVisible] = useState(false);
    const [isRestartButtonVisible, setIsRestartButtonVisible] = useState(true);
    const [isSelectMultiLengthVisible, setIsSelectMultiLengthVisible] = useState(false);
    const [isMultiQuantityInvalidVisible, setIsMultiQuantityInvalidVisible] = useState(false);
    const [isShowMoreStatsVisible, setIsShowMoreStatsVisible] = useState(false);
    const [isPopupVisible, setIsPopupVisible] = useState(false);
    const [isEditTimeVisible, setIsEditTimeVisible] = useState(false);
    const [focusTimer, setFocusTimer] = useState("");
    const [focusFormLabel, setFocusFormLabel] = useState("");
    const [recentTimes, setRecentTimes] = useState([]);
    const [recentScrambles, setRecentScrambles] = useState([]);
    const [isHorizontal, setIsHorizontal] = useState(window.innerWidth > window.innerHeight && isAndroid);

    const startTime = useRef(0);
    const wakeLock = useRef(null);
    const selectedPuzzle = useRef("");
    const timer = useRef(null);
    const timerInterval = useRef(null)
    const isDragAllowed = useRef(true);
    const isNewScramble = useRef(true);
    const multiQuantity = useRef(0);
    const formLabel = useRef(null);

    // LOGIC FUNCTIONS
    const prepare = useCallback(() => {
        setIsTimerPrepared(true);
        setIsShowMoreStatsVisible(false);
        setIsRestartButtonVisible(false);
        setScrambleDisplayMode("none");
        if (isAndroid) {
            isDragAllowed.current = false; // to not accidentally cancel
            setTimeout(() => {
                isDragAllowed.current = true;
            }, 100);
        }
    }, [isAndroid]);

    const interrupt = useCallback(() => {
        setIsTimerPrepared(false);
        setScrambleDisplayMode("block");
        setIsShowMoreStatsVisible(true);
        setIsRestartButtonVisible(true);
    }, []);

    const start = useCallback(() => {
        const now = performance.now(); // instant time fetch
        startTime.current = now;
        timerInterval.current = setInterval(() => {
            setElapsedTime(performance.now() - now);
        }, constants.TIMER_REFRESH_RATE);
        setIsTimerPrepared(false);
        setIsTimerRunning(true);
        if (isAndroid) {
            setFocusTimer("center");
        }
    }, [isAndroid]);

    const stop = useCallback(() => {
        const now = performance.now(); // instant time fetch
        clearInterval(timerInterval.current);
        const finalTime = now - startTime.current;
        setElapsedTime(finalTime);
        setRecentTimes((previousTimes) => [...previousTimes, finalTime]);
        setRecentScrambles((previousScrambles) => [...previousScrambles, scramble]);
        setIsTimerRunning(false);
        isNewScramble.current = true;
        setScrambleDisplayMode("block");
        setIsShowMoreStatsVisible(true);
        setIsRestartButtonVisible(true);
        if (isAndroid) {
            isDragAllowed.current = false // to not accidentally drag, restart or click a button
            setFocusTimer("end");
            setTimeout(() => {
                isDragAllowed.current = true;
            }, 300);
        }
    }, [isAndroid, scramble]);

    const editLastTime = useCallback(() => {
        setIsShowMoreStatsVisible(false);
        setIsTimerVisible(false);
        setIsTimerPrepared(false);
        setScrambleDisplayMode("none");
        setIsEditTimeVisible(true);
        setIsRestartButtonVisible(true);
    }, []);

    const hideEverything = () => {
        setIsFormVisible(false);
        setScrambleDisplayMode("none");
        setIsTimerVisible(false);
        setIsRestartButtonVisible(false);
        setIsSelectMultiLengthVisible(false);
        setIsMultiQuantityInvalidVisible(false);
        setIsShowMoreStatsVisible(false);
        setIsPopupVisible(false);
        setIsEditTimeVisible(false);
    };

    const restoreDefaults = useCallback(() => {
        setElapsedTime(0);
        setScramble("");
        setIsTimerPrepared(false);
        setIsTimerRunning(false);
        hideEverything();
        setIsFormVisible(true);
        setIsRestartButtonVisible(true);
        setRecentTimes([]);
        setRecentScrambles([]);
        startTime.current = 0;
        selectedPuzzle.current = "";
        isNewScramble.current = true;
        multiQuantity.current = 0;
    }, []);

    // EVENT HANDLERS
    const handleTouchStart = useCallback((event) => {
        if (isTimerVisible && !isTimerRunning && !isTimerPrepared && selectedPuzzle.current !== constants.MULTI && event.target.tagName !== "BUTTON" && event.target.tagName !== "INPUT" && isDragAllowed.current) {
            prepare();
        } else if (isTimerPrepared && 0 !== recentTimes.length) {
            editLastTime();
        } else if (isTimerRunning) {
            stop();
        }
    }, [editLastTime, isTimerPrepared, isTimerRunning, isTimerVisible, prepare, recentTimes.length, stop]);

    const handleTouchMove = useCallback((event) => {
        if (!isDragAllowed.current) {
            event.preventDefault();
        } else if (isTimerPrepared) {
            interrupt();
        }
    }, [interrupt, isTimerPrepared]);

    const handleTouchEnd = useCallback(() => {
        if (isTimerPrepared) {
            start();
        }
    }, [isTimerPrepared, start]);

    const handleResize = useCallback(() => {
        setIsHorizontal(window.innerWidth > window.innerHeight);
        if (isTimerVisible) {
            setFocusTimer(isTimerRunning ? "center" : "end");
        }
        if (isSelectMultiLengthVisible) {
            setFocusFormLabel("start");
        }
    }, [isSelectMultiLengthVisible, isTimerRunning, isTimerVisible]);

    const handleKeyDown = useCallback((event) => {
        if (isTimerVisible && !isTimerRunning && !isTimerPrepared) {
            if (event.key === " ") {
                event.preventDefault();
                prepare();
            } else if (event.key === "Delete" && 0 !== recentTimes.length) {
                event.preventDefault();
                editLastTime();
            } else if (event.key === "Escape") {
                event.preventDefault();
                restoreDefaults();
            }
        } else if (isTimerPrepared && event.key !== " ") {
            event.preventDefault();
            interrupt();
        } else if (isTimerRunning) {
            event.preventDefault();
            stop();
        } else if (isPopupVisible && event.key === "Escape") {
            event.preventDefault();
            restoreDefaults();
        }
    }, [editLastTime, interrupt, isPopupVisible, isTimerPrepared, isTimerRunning, isTimerVisible, prepare, recentTimes.length, restoreDefaults, stop]);

    const handleKeyUp = useCallback((event) => {
        if (isTimerPrepared && event.key === " ") {
            event.preventDefault();
            start();
        }
    }, [isTimerPrepared, start]);

    const handleSubmit = (event) => {
        let puzzle = event.target.id;
        if (puzzle === constants.MULTI_UNPROCESSED) {
            hideEverything();
            setIsSelectMultiLengthVisible(true);
            setIsRestartButtonVisible(true);
            if (isAndroid) {
                setFocusFormLabel("start");
            }
        } else {
            if (constants.MULTI === puzzle && (isNaN(multiQuantity.current) || multiQuantity.current < 1 || multiQuantity.current > 200)) {
                hideEverything();
                setIsMultiQuantityInvalidVisible(true);
                setTimeout(() => {
                    setIsMultiQuantityInvalidVisible(false);
                    setIsSelectMultiLengthVisible(true);
                    setIsRestartButtonVisible(true);
                    if (isAndroid) {
                        setFocusFormLabel("start");
                    }
                }, constants.TIMEOUT_DELAY)
                return;
            }
            hideEverything();
            selectedPuzzle.current = puzzle;
            setScrambleDisplayMode("block");
            setIsTimerVisible(true);
            setIsShowMoreStatsVisible(true);
            setIsRestartButtonVisible(true);
            if (isAndroid) {
                setFocusTimer("end");
            }
        }
    };

    // EFFECTS
    // Set up event listeners
    useEffect(() => {
        if (isAndroid) {
            document.addEventListener("touchstart", handleTouchStart, true);
            document.addEventListener("touchmove", handleTouchMove, { passive: false });
            document.addEventListener("touchend", handleTouchEnd);
            window.addEventListener("resize", handleResize);
        } else {
            document.addEventListener("keydown", handleKeyDown, { passive: false });
            document.addEventListener("keyup", handleKeyUp, { passive: false });
        };
        return () => {
            if (isAndroid) {
                document.removeEventListener("touchstart", handleTouchStart, true);
                document.removeEventListener("touchmove", handleTouchMove);
                document.removeEventListener("touchend", handleTouchEnd);
                window.removeEventListener("resize", handleResize);
            } else {
                document.removeEventListener("keydown", handleKeyDown);
                document.removeEventListener("keyup", handleKeyUp);
            }
        };
    }, [handleKeyDown, handleKeyUp, handleResize, handleTouchEnd, handleTouchMove, handleTouchStart, isAndroid]);

    // Set screen lock
    useEffect(() => {
        const requestWakeLock = async () => {
            if (navigator.wakeLock) {
                try {
                    wakeLock.current = await navigator.wakeLock.request("screen");
                } catch (error) {
                    console.error(error);
                }
            }
        };

        if (isTimerVisible || isTimerPrepared || isTimerRunning) { // small hack to screen lock after phone unlock
            requestWakeLock();
        } else {
            wakeLock.current?.release();
        }
        return () => {
            wakeLock.current?.release();
        };
    }, [isTimerPrepared, isTimerRunning, isTimerVisible]); // extra dependencies to trigger the effect

    // Set non-selectable document
    useEffect(() => {
        if (isTimerVisible && isAndroid) {
            document.body.classList.add("nonSelectable");
        } else {
            document.body.classList.remove("nonSelectable");
        }
        return () => {
            document.body.classList.remove("nonSelectable");
        };
    }, [isAndroid, isTimerVisible]);

    // Auto-scroll effect
    useEffect(() => {
        if (focusTimer !== "" && timer.current) {
            timer.current.scrollIntoView({
                block: focusTimer
            })
            setFocusTimer("");
        }
    }, [focusTimer]);

    // Auto-scroll effect
    useEffect(() => {
        if (focusFormLabel !== "") {
            formLabel.current.scrollIntoView({
                block: focusFormLabel
            })
            setFocusFormLabel("");
        }
    }, [focusFormLabel]);

    // RENDER
    const renderForm = () => {
        return (
            <Form>
                <Button className="thirty-percent" id={constants.THREE} onClick={handleSubmit}>3x3</Button>
                <Button className="thirty-percent" id={constants.TWO} onClick={handleSubmit}>2x2</Button>
                <Button className="thirty-percent" id={constants.FOUR} onClick={handleSubmit}>4x4</Button>
                <Button className="thirty-percent" id={constants.FIVE} onClick={handleSubmit}>5X5</Button>
                <Button className="thirty-percent" id={constants.SIX} onClick={handleSubmit}>6X6</Button>
                <Button className="thirty-percent" id={constants.SEVEN} onClick={handleSubmit}>7X7</Button>
                <Button className="thirty-percent" id={constants.BLD} onClick={handleSubmit}>BLD</Button>
                <Button className="thirty-percent" id={constants.FMC} onClick={handleSubmit}>FMC</Button>
                <Button className="thirty-percent" id={constants.OH} onClick={handleSubmit}>OH</Button>
                <Button className="thirty-percent" id={constants.CLOCK} onClick={handleSubmit}>Clock</Button>
                <Button className="thirty-percent" id={constants.MEGAMINX} onClick={handleSubmit}>Mega</Button>
                <Button className="thirty-percent" id={constants.PYRAMINX} onClick={handleSubmit}>Pyra</Button>
                <Button className="thirty-percent" id={constants.SKEWB} onClick={handleSubmit}>Skewb</Button>
                <Button className="thirty-percent" id={constants.SQUARE} onClick={handleSubmit}>Sq-1</Button>
                <Button className="thirty-percent" id={constants.FOUR_BLD} onClick={handleSubmit}>4BLD</Button>
                <Button className="fifty-percent" id={constants.FIVE_BLD} onClick={handleSubmit}>5BLD</Button>
                <Button className="fifty-percent" id={constants.MULTI_UNPROCESSED} onClick={handleSubmit}>Multi</Button>
            </Form>
        );
    };

    const renderTimer = () => {
        let timerValue = isTimerPrepared ? "0:00:000" : formatTime(elapsedTime);
        let timerClass = isTimerRunning ? "running-timer" : isTimerPrepared ? "green-timer" : "";
        return (
            <h3 ref={timer} className={timerClass}>{timerValue}</h3>
        );
    };

    const renderAverages = () => {
        const averageDisplay = isTimerRunning || isTimerPrepared ? "none" : "grid";
        const params = [
            { label: "best", length: 1, what: "best", removeBestAndWorst: false, align: "left" },
            { label: "mo3", length: 3, what: "last", removeBestAndWorst: false, align: "left" },
            { label: "avg5", length: 5, what: "last", removeBestAndWorst: true, align: "left" },
            { label: "avg12", length: 12, what: "last", removeBestAndWorst: true, align: isHorizontal ? "left" : "right" },
            { label: "mo50", length: 50, what: "last", removeBestAndWorst: false, align: "right" },
            { label: "mo100", length: 100, what: "last", removeBestAndWorst: false, align: "right" },
        ];
        return renderStats({ times: recentTimes, averageDisplay: averageDisplay, className: "background", params: params });
    };

    const renderSelectMultiLength = () => {
        return (
            <Form id={constants.MULTI} onSubmit={handleSubmit}>
                <Form.Label ref={formLabel}>Number of scrambles:</Form.Label>
                <Form.Control inputMode="numeric" onChange={(event) => multiQuantity.current = parseInt(event.target.value)} />
                <Button type="submit" variant="success">Generate</Button>
            </Form>
        );
    };

    const renderScramble = () => {
        return (
            <Scramble isNewScramble={isNewScramble.current}
                onScrambleChange={(s) => { setScramble(s); isNewScramble.current = false }}
                puzzle={selectedPuzzle.current}
                display={scrambleDisplayMode}
                quantity={multiQuantity.current}
                isHorizontal={isHorizontal} />
        );
    };

    return (
        <>
            <h1 id="rubikTimer">Rubik timer</h1>
            <div className="app rubikTimer">
                {isFormVisible && renderForm()}
                {isSelectMultiLengthVisible && renderSelectMultiLength()}
                {isMultiQuantityInvalidVisible && <h2>Enter a number between 1 and 200</h2>}
                {isTimerVisible && renderStats({
                    times: recentTimes,
                    averageDisplay: !isTimerVisible || isTimerRunning || isTimerPrepared ? "none" : "grid",
                    params: [
                        { label: "session", align: "left" }]
                })}
                <div className="timerContainer">
                    {isPopupVisible
                        ? <>
                            <Popup content={{ recentTimes: recentTimes, recentScrambles: recentScrambles }} onPopupClose={() => {
                                setIsPopupVisible(false);
                                setElapsedTime(recentTimes.length > 0
                                    ? recentTimes[recentTimes.length - 1]
                                    : 0);
                                setScrambleDisplayMode("block");
                                setIsTimerVisible(true);
                                setIsShowMoreStatsVisible(true);
                            }} />
                            {renderScramble()}
                        </>
                        : <>
                            {isTimerVisible && renderAverages()}
                            {renderScramble()}
                            {isTimerVisible && renderTimer()}
                        </>
                    }
                </div>
                <Button style={{ display: isShowMoreStatsVisible && isAndroid && selectedPuzzle.current === constants.MULTI ? "block" : "none" }} variant="success" onTouchStart={prepare}>Start</Button>
                {isEditTimeVisible && <Popup content={{ recentTimes: recentTimes, recentScrambles: recentScrambles }} justEditLast={true} onPopupClose={() => {
                    setIsEditTimeVisible(false);
                    setElapsedTime(recentTimes.length > 0
                        ? recentTimes[recentTimes.length - 1]
                        : 0);
                    setScrambleDisplayMode("block");
                    setIsTimerVisible(true);
                    setIsShowMoreStatsVisible(true);
                }} />}
                {isShowMoreStatsVisible && <Button onClick={(event) => {
                    if (isAndroid && !isDragAllowed.current) {
                        event.preventDefault();
                        return;
                    }
                    setIsShowMoreStatsVisible(false);
                    setIsTimerVisible(false);
                    setScrambleDisplayMode("none");
                    setIsPopupVisible(true);
                }}>Session stats</Button>}
                {isRestartButtonVisible && <Button className="restart" onClick={(event) => {
                    if (isAndroid && !isDragAllowed.current) {
                        event.preventDefault();
                        return;
                    }
                    restoreDefaults();
                }}
                >Restart</Button>}
            </div>
        </>
    );
};

export default RubikTimer;
