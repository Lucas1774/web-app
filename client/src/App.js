import { useState } from "react";
import { Button } from "react-bootstrap";
import "./App.css";
import Calculator from "./components/Calculator/Calculator";
import ParsedAbout from "./components/ParsedAbout/ParsedAbout";
import Portfolio from "./components/Portfolio/Portfolio";
import RubikTimer from "./components/RubikTimer/RubikTimer";
import SecretSanta from "./components/SecretSanta/SecretSanta";
import Shopping from "./components/Shopping/Shopping";
import Sudoku from "./components/Sudoku/Sudoku";

const COMPONENTS = {
  Portfolio,
  Shopping,
  RubikTimer,
  Sudoku,
  SecretSanta,
  Calculator,
};

const App = () => {
  const [program, setProgram] = useState("Portfolio");
  const Current = program ? COMPONENTS[program] : null;

  return (
    <>
      <div style={{ display: "flex", flexWrap: "wrap" }}>
        <ParsedAbout />
        <br></br>
      </div>
      <div className="app-wrapper">
        {!program ? (
          <div className="app" style={{ width: "auto" }}>
            {Object.keys(COMPONENTS).map((name) => (
              <Button key={name} onClick={() => setProgram(name)}>
                {name}
              </Button>
            ))}
          </div>
        ) : (
          Current && (
            <Current onClose={() => setProgram(null)} />
          )
        )}
      </div>
    </>
  );
}

export default App;
