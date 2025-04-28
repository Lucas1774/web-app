import React, { useState } from "react";
import { Button, Form, Row } from "react-bootstrap";
import { get, post } from "../../api";
import Spinner from "../Spinner";
import { handleError } from "../errorHandler";
import "./Calculator.css";

const Calculator = () => {
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleKeyDown = (event) => {
    event.preventDefault();
    setInput(event.target.value);
  };

  const handleClick = (event) => {
    event.preventDefault();
    setInput(input + event.target.value);
  };

  const handleDelete = () => {
    setInput(input.substring(0, input.length - 1));
  };

  const handleClear = () => {
    setInput("");
  };

  const handleSubmit = (e, isSubmit) => {
    e.preventDefault();
    let tempInput = input;
    setInput("");
    setIsLoading(true);
    let action = isSubmit ? () => post("/calculator/ans", input) : () => get("/calculator/ans");
    action()
      .then(response => {
        let calculatorData = response.data;
        setInput(
          isSubmit
            ? calculatorData.toString()
            : input + (calculatorData.textMode
              ? calculatorData.text.toString()
              : calculatorData.ans.toString())
        );
      })
      .catch(error => {
        const prefix = "Error " + (isSubmit ? "sending " : "receiving ") + "data";
        if (error.response && error.response.status === 403) {
          setInput("stop");
        } else {
          handleError(prefix, error);
          setInput(tempInput);
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  return (
    <><h1 id="calculator">Calculator</h1>
      <div className="app calculator">
        <Form onSubmit={(e) => handleSubmit(e, true)}>
          <Form.Control value={input} onChange={handleKeyDown} />
          {isLoading && <Spinner color="#000" position="absolute" />}
        </Form>
        <Row className="first">
          <Button onClick={handleClick} value="(">{"("}</Button>
          <Button onClick={handleClick} value=")">{")"}</Button>
          <Button onClick={handleClick} value="sqrt">{"\u221A"}</Button>
          <Button onClick={handleClick} value="^">^</Button>
          <Button onClick={handleClick} value="log">log</Button>
        </Row>
        <Row>
          <Button onClick={handleClick} value="7">7</Button>
          <Button onClick={handleClick} value="8">8</Button>
          <Button onClick={handleClick} value="9">9</Button>
          <Button onClick={handleDelete}>{"\u2190"}</Button>
          <Button onClick={handleClear}>C</Button>
        </Row>
        <Row>
          <Button onClick={handleClick} value="6">6</Button>
          <Button onClick={handleClick} value="5">5</Button>
          <Button onClick={handleClick} value="4">4</Button>
          <Button onClick={handleClick} value="*">*</Button>
          <Button onClick={handleClick} value="/">/</Button>
        </Row>
        <Row>
          <Button onClick={handleClick} value="1">1</Button>
          <Button onClick={handleClick} value="2">2</Button>
          <Button onClick={handleClick} value="3">3</Button>
          <Button onClick={handleClick} value="+">+</Button>
          <Button onClick={handleClick} value="-">-</Button>
        </Row>
        <Row>
          <Button onClick={handleClick} value="0">0</Button>
          <Button onClick={handleClick} value=".">.</Button>
          <Button onClick={handleClick} value="*10^">EXP</Button>
          <Button onClick={(e) => handleSubmit(e, false)}>Ans</Button>
          <Button type="submit" variant="success" onClick={(e) => { handleSubmit(e, true) }}>=</Button>
        </Row>
      </div></>
  );
};

export default Calculator;
