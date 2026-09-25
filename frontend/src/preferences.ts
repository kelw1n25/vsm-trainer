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
