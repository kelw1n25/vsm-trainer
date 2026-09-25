import { useSyncExternalStore } from "react";

// Личные настройки интерфейса хранятся только в этом браузере
const REDUCE_MOTION_KEY = "vsm_reduce_motion";

export function loadReduceMotion(): boolean {
  try {
    return localStorage.getItem(REDUCE_MOTION_KEY) === "1";
  } catch {
    return false;
  }
}

export function applyReduceMotion(enabled: boolean): void {
  document.documentElement.toggleAttribute("data-reduce-motion", enabled);
  try {
    localStorage.setItem(REDUCE_MOTION_KEY, enabled ? "1" : "0");
  } catch {
    // без localStorage настройка действует до перезагрузки страницы
  }
}

export type Theme = "light" | "dark";

const THEME_KEY = "vsm_theme";
const themeListeners = new Set<() => void>();

/** Сохранённая тема, а если пользователь ещё не выбирал — системная. */
export function loadTheme(): Theme {
  try {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved === "light" || saved === "dark") return saved;
  } catch {
    // без localStorage берём системную тему
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
  themeListeners.forEach((listener) => listener());
}

export function saveTheme(theme: Theme): void {
  applyTheme(theme);
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {
    // без localStorage тема действует до перезагрузки страницы
  }
}

function subscribeTheme(listener: () => void): () => void {
  themeListeners.add(listener);
  return () => themeListeners.delete(listener);
}

/** Текущая тема; переключатели в шапке и в настройках всегда показывают одно и то же. */
export function useTheme(): Theme {
  return useSyncExternalStore(subscribeTheme, () => (document.documentElement.dataset.theme === "dark" ? "dark" : "light"));
}
