import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { applyReduceMotion, loadReduceMotion } from "./preferences";
import "./styles.css";

applyReduceMotion(loadReduceMotion());

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
