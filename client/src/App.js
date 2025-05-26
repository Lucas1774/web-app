import { Col, Row } from "react-bootstrap";
import "./App.css";
import Calculator from "./components/Calculator/Calculator";
import Portfolio from "./components/Portfolio/Portfolio";
import RubikTimer from "./components/RubikTimer/RubikTimer";
import SecretSanta from "./components/SecretSanta/SecretSanta";
import Shopping from "./components/Shopping/Shopping";
import Sudoku from "./components/Sudoku/Sudoku";

const App = () => {
  return (
    <>
      <Row>
        <Col>
          <Portfolio />
        </Col>
      </Row>
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
      </Row>
      <Row>
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
