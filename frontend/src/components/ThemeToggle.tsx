import { saveTheme, useTheme } from "../preferences";
import { MoonIcon, SunIcon } from "./icons";

/** Ползунок смены темы: солнце — светлая, луна — тёмная. */
export function ThemeToggle() {
  const dark = useTheme() === "dark";
  return (
    <button
      type="button"
      role="switch"
      aria-checked={dark}
      aria-label="Тёмная тема"
      title={dark ? "Включить светлую тему" : "Включить тёмную тему"}
      className={`theme-toggle ${dark ? "theme-toggle--dark" : ""}`}
      onClick={() => saveTheme(dark ? "light" : "dark")}
    >
      <span className="theme-toggle__thumb" />
      <span className="theme-toggle__icon theme-toggle__icon--sun">
        <SunIcon />
      </span>
      <span className="theme-toggle__icon theme-toggle__icon--moon">
        <MoonIcon />
      </span>
    </button>
  );
}
