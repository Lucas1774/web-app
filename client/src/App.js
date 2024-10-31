import React from "react";
import { Col, Row } from "react-bootstrap";
import "./App.css";
import Calculator from "./components/Calculator/Calculator";
import ParsedAbout from "./components/ParsedAbout/ParsedAbout";
import RubikTimer from "./components/RubikTimer/RubikTimer";
import SecretSanta from "./components/SecretSanta/SecretSanta";
import Shopping from "./components/Shopping/Shopping";
import Sudoku from "./components/Sudoku/Sudoku";

const App = () => {
  return (
    <><div style={{ display: "flex", flexWrap: "wrap" }}>
      <ParsedAbout />
      <br></br>
    </div>
    <Row>
      <Col>
        <Shopping />
      </Col>
      <Col>
        <RubikTimer />
      </Col>
      <Col>
        <Sudoku />
      </Col>
      <Col>
        <SecretSanta />
      </Col>
      <Col>
        <Calculator />
      </Col>
    </Row>
    </>
  );
}

export default App;
