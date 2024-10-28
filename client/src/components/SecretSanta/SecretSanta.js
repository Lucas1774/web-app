import React, { useState } from "react";
import { Form, Button } from "react-bootstrap";
import "./SecretSanta.css";

const SecretSanta = () => {
    const [participantCounter, setParticipantCounter] = useState(1);
    const [participants, setParticipants] = useState([]);
    const [targets, setTargets] = useState([]);
    const [input, setInput] = useState("");
    const [isFormVisible, setIsFormVisible] = useState(true);
    const [isSavingVisible, setIsSavingVisible] = useState(false);
    const [isCalculatingVisible, setIsCalculatingVisible] = useState(false);
    const [isNoParticipantsVisible, setIsNoParticipantsVisible] = useState(false);
    const [isChosenVisible, setIsChosenVisible] = useState(false);
    const [isTargetVisible, setIsTargetVisible] = useState(false);
    const [isRestartButtonVisible, setIsRestartButtonVisible] = useState(true);
    const [isParticipantListVisible, setIsParticipantListVisible] = useState(true);

    const handleKeyDown = (event) => {
        event.preventDefault();
        setInput(event.target.value);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        hideEverything();
        setIsSavingVisible(true);
        let delay = 1000;
        if (input !== "" && !participants.includes(input)) {
            setParticipants([...participants, input]);
            delay = 0;
        }
        setTimeout(() => {
            setInput("");
            setIsSavingVisible(false);
            setIsFormVisible(true);
            setIsRestartButtonVisible(true);
            setIsParticipantListVisible(true);
        }, delay);
    };

    const handleStartLottery = () => {
        hideEverything();
        if (participants.length >= 3) {
            generateRandomCycle(participants);
            setIsCalculatingVisible(true);
            setTimeout(() => {
                setIsCalculatingVisible(false);
                setIsChosenVisible(true);
                setIsRestartButtonVisible(true);
            }, 1000);
        } else {
            setIsNoParticipantsVisible(true);
            setTimeout(() => {
                setInput("");
                setIsSavingVisible(false);
                setIsFormVisible(true);
                setIsRestartButtonVisible(true);
                setIsParticipantListVisible(true);
                setIsNoParticipantsVisible(false);
            }, 1000);
        }
    };

    const showTarget = () => {
        hideEverything();
        setIsTargetVisible(true);
        setTimeout(() => {
            setIsTargetVisible(false);
            setParticipantCounter(participantCounter + 1);
            if (participants.length > participantCounter) {
                setIsChosenVisible(true);
                setIsRestartButtonVisible(true);
            } else {
                restoreDefaults();
            }
        }, 2000);
    };

    const renderForm = () => {
        return (
            <Form onSubmit={handleSubmit}>
                <Form.Label>Enter your name:</Form.Label>
                <Form.Control type="text" value={input} onChange={handleKeyDown} />
                <Button className="fifty-percent" type="submit" variant="success">Add player</Button>
                <Button className="fifty-percent" variant="success" onClick={handleStartLottery}>Start raffle</Button>
            </Form>
        );
    };

    const renderChosen = () => {
        return (
            <>
                <div>{participants[participantCounter - 1]} is the secret santa for...</div>
                <div>
                    <Button variant="success" onClick={showTarget}>
                        Press me to show!
                    </Button>
                </div>
            </>
        );
    };

    const renderTarget = () => {
        const participant = participants[participantCounter - 1];
        const target = targets[(targets.indexOf(participant) + 1) % participants.length];
        return (
            <h2 style={{ textAlign: "center" }}>{target}!</h2>
        );
    };

    const renderParticipantList = () => {
        return (
            <>
                <h2 style={{ marginTop: "20px" }}>Participants</h2>
                <div>
                    {participants.map((participant) => (
                        <div key={participant}>{participant}</div>
                    ))}
                </div>
            </>
        );
    };

    const generateRandomCycle = (originalArray) => {
        const unusedElements = [...originalArray];
        let newTargets = [];
        while (unusedElements.length !== 0) {
            const randomIndex = Math.floor(Math.random() * unusedElements.length);
            const newTarget = unusedElements[randomIndex].toString();
            newTargets = [...newTargets, newTarget];
            unusedElements.splice(randomIndex, 1);
        }
        setTargets(newTargets);
    };

    const hideEverything = () => {
        setIsFormVisible(false);
        setIsSavingVisible(false);
        setIsCalculatingVisible(false);
        setIsNoParticipantsVisible(false);
        setIsChosenVisible(false);
        setIsTargetVisible(false);
        setIsRestartButtonVisible(false);
        setIsParticipantListVisible(false);
    };

    const restoreDefaults = () => {
        setParticipantCounter(1);
        setParticipants([]);
        setTargets([]);
        setInput("");
        setIsFormVisible(true);
        setIsRestartButtonVisible(true);
        setIsParticipantListVisible(true);
    };

    return (
        <>
            <h1 id="secretSanta">Secret Santa</h1>
            <div className="app secretSanta">
                {isFormVisible && renderForm()}
                {isSavingVisible && (input === ""
                    ? <div>Type a name</div> : participants.includes(input)
                        ? <div>Player already registered</div> : <div></div>)}
                {isCalculatingVisible && <div>Calculating...</div>}
                {isNoParticipantsVisible && (
                    <div>Not enough participants</div>
                )}
                {isChosenVisible && renderChosen()}
                {isTargetVisible && renderTarget()}
                {isRestartButtonVisible && <Button className="restart" onClick={() => { hideEverything(); restoreDefaults(); }}
                >Restart</Button>}
                {isParticipantListVisible && renderParticipantList()}
            </div>
        </>
    );
};

export default SecretSanta;
