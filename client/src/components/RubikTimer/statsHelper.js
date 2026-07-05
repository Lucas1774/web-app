import { DNF, EMPTY_TIMER, noop } from "../../constants";

export const formatTime = (time) => {
    if (time === Infinity) {
        return DNF;
    }
    if (time === EMPTY_TIMER) {
        return EMPTY_TIMER;
    }
    const minutes = Number.parseInt(Math.floor(time / 60000)).toString();
    const seconds = Number.parseInt(Math.floor((time % 60000) / 1000)).toString().padStart(2, "0");
    const milliseconds = Number.parseInt((time % 1000)).toString().padStart(3, "0");
    return `${minutes}:${seconds}:${milliseconds}`;
};

export const renderAllTimes = ({ recentTimes, recentScrambles, firstIndex = 0, lastIndex = recentTimes.length - 1, onClickEffect = noop }) => {
    return recentTimes.map((time, index) => {
        if (index >= firstIndex && index <= lastIndex) {
            return (
                /* NOSONAR */<h4 key={index + 1} onClick={() => onClickEffect(index)} style={{ cursor: onClickEffect === noop ? "default" : "pointer" }}>
                    {index + 1}{")"} {formatTime(time)} {recentScrambles[index]}
                </h4>
            );
        } else {
            return null;
        }
    });
};

export const renderStats = ({ times, formatter = formatTime, onClickEffect = noop, averageDisplay = "grid", className = "", params = [
    { label: "0", length: -1 },
    { label: "session", align: "left" },
    { label: "mean", length: 0, what: "mean", removeBestAndWorst: false, align: "left" },
    { label: "median", length: 0, what: "median", removeBestAndWorst: false, align: "left" },
    { label: "1", length: -1 },
    { label: "best", length: 1, what: "best", removeBestAndWorst: false, align: "left" },
    { label: "worst", length: 1, what: "worst", removeBestAndWorst: false, align: "left" },
    { label: "last", length: 1, what: "last", removeBestAndWorst: false, align: "left" },
    { label: "2", length: -1 },
    { label: "best mo3", length: 3, what: "best", removeBestAndWorst: false, align: "left" },
    { label: "worst mo3", length: 3, what: "worst", removeBestAndWorst: false, align: "left" },
    { label: "current mo3", length: 3, what: "last", removeBestAndWorst: false, align: "left" },
    { label: "3", length: -1 },
    { label: "best avg5", length: 5, what: "best", removeBestAndWorst: true, align: "left" },
    { label: "worst avg5", length: 5, what: "worst", removeBestAndWorst: true, align: "left" },
    { label: "current avg5", length: 5, what: "last", removeBestAndWorst: true, align: "left" },
    { label: "4", length: -1 },
    { label: "best avg12", length: 12, what: "best", removeBestAndWorst: true, align: "left" },
    { label: "worst avg12", length: 12, what: "worst", removeBestAndWorst: true, align: "left" },
    { label: "current avg12", length: 12, what: "last", removeBestAndWorst: true, align: "left" },
    { label: "5", length: -1 },
    { label: "best mo50", length: 50, what: "best", removeBestAndWorst: false, align: "left" },
    { label: "worst mo50", length: 50, what: "worst", removeBestAndWorst: false, align: "left" },
    { label: "current mo50", length: 50, what: "last", removeBestAndWorst: false, align: "left" },
    { label: "6", length: -1 },
    { label: "best mo100", length: 100, what: "best", removeBestAndWorst: false, align: "left" },
    { label: "worst mo100", length: 100, what: "worst", removeBestAndWorst: false, align: "left" },
    { label: "current mo100", length: 100, what: "last", removeBestAndWorst: false, align: "left" },
] }) => {
    const getAverage = (times) => {
        return times.reduce((sum, time) => sum + time, 0) / times.length;
    };
    const getMedian = (times) => {
        let sortedTimes = [...times].sort((a, b) => a - b);
        let middle = Math.floor(sortedTimes.length / 2);
        return sortedTimes.length % 2 === 0 ? (sortedTimes[middle - 1] + sortedTimes[middle]) / 2 : sortedTimes[middle];
    };
    return (
        <div className={className} style={{ display: averageDisplay }}>
            {params.map(({ label, length, what, removeBestAndWorst, align }) => {
                if ("session" === label) {
                    return (<h4 key={label} style={{ textAlign: align }}>{label} {"(" + times.filter(time => time !== Infinity).length} / {times.length + ")"}</h4>)
                }
                if (length === - 1) {
                    return (<h4 key={label}> </h4>)
                }
                let displayTime = EMPTY_TIMER;
                let indexes = [];
                let aux = [...times];
                if (removeBestAndWorst) {
                    if (aux.length === length - 1) {
                        displayTime = getAverage(aux.toSorted((a, b) => a - b).slice(1));
                        indexes = ({ start: 0, end: aux.length - 1 });
                    } else if (aux.length >= length) {
                        if ("last" === what) {
                            displayTime = getAverage(aux.slice(-length).sort((a, b) => a - b).slice(1, -1));
                            indexes = ({ start: aux.length - length, end: aux.length - 1 });
                        } else if ("best" === what || "worst" === what) {
                            for (let i = 0; i <= times.length - length; i++) {
                                let aux2 = [...aux];
                                let newAverage = getAverage(aux2.slice(0, length).sort((a, b) => a - b).slice(1, -1));
                                if (i === 0 || (what === "best" ? newAverage < displayTime : newAverage > displayTime)) {
                                    displayTime = newAverage;
                                    indexes = ({ start: i, end: (i + length) - 1 });
                                }
                                aux.shift();
                            }
                        }
                    }
                } else if (aux.length >= length) {
                    if (length === 0 && aux.length > 0) {
                        if (what === "mean") {
                            displayTime = getAverage(aux);
                        } else if (what === "median") {
                            displayTime = getMedian(aux);
                            let indexOfDisplayTime = aux.indexOf(displayTime);
                            indexes = ({ start: indexOfDisplayTime, end: indexOfDisplayTime });
                        }
                    } else if (length === 1) {
                        if (what === "last") {
                            displayTime = aux.at(-1);
                        } else if (what === "best") {
                            displayTime = Math.min(...aux);
                        } else if (what === "worst") {
                            displayTime = Math.max(...aux);
                        }
                        let indexOfDisplayTime = aux.indexOf(displayTime);
                        indexes = ({ start: indexOfDisplayTime, end: indexOfDisplayTime });
                    } else if (what === "last") {
                        displayTime = getAverage(aux.slice(-length));
                        indexes = ({ start: aux.length - length, end: aux.length - 1 });
                    } else if (what === "best" || what === "worst") {
                        for (let i = 0; i <= times.length - length; i++) {
                            let aux2 = [...aux];
                            let newAverage = getAverage(aux2.slice(0, length));
                            if (i === 0 || (what === "best" ? newAverage < displayTime : newAverage > displayTime)) {
                                displayTime = newAverage;
                                indexes = ({ start: i, end: (i + length) - 1 });
                            }
                            aux.shift();
                        }
                    }
                }
                displayTime = formatter(displayTime);
                return (
                    /* NOSONAR */ <h4 key={label} onClick={() => onClickEffect(indexes)} style={{ textAlign: align, cursor: onClickEffect === noop || indexes.length === 0 ? "default" : "pointer" }}>
                        {align === "left" ? label + " " + displayTime : displayTime + " " + label}
                    </h4>
                );
            })}
        </div>
    );
};
