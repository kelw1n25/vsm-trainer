import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { applyReduceMotion, applyTheme, loadReduceMotion, loadTheme } from "./preferences";
import "./styles.css";

applyReduceMotion(loadReduceMotion());
applyTheme(loadTheme());

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
