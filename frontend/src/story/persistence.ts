// Прогресс чтения новеллы хранится в браузере: на какой реплике остановился игрок, журнал диалогов,
// какие сцены уже видел, какие финалы открыл. Решения и шкалы — на сервере: подделать их отсюда нельзя.

export interface HistoryEntry {
  kind: "speech" | "thought" | "narration" | "choice" | "timeout";
  speaker: string | null;
  text: string;
}

export interface RunProgress {
  scenarioId: string;
  nodeId: string;
  /** Сколько реплик текущей сцены уже прочитано. */
  lineIndex: number;
  history: HistoryEntry[];
  choices: string[];
  ending: string | null;
}

export interface ScenarioMemory {
  seenNodes: string[];
  endings: string[];
  playthroughs: number;
}

interface Store {
  runs: Record<string, RunProgress>;
  scenarios: Record<string, ScenarioMemory>;
  auto: boolean;
  muted?: boolean;
}

const KEY_PREFIX = "vsm_story_v1";
// Журналы старых прохождений не нужны вечно — держим последние
const MAX_RUNS = 20;

function storageKey(employeeId: number): string {
  return `${KEY_PREFIX}:${employeeId}`;
}

function read(employeeId: number): Store {
  try {
    const raw = localStorage.getItem(storageKey(employeeId));
    if (raw) return JSON.parse(raw) as Store;
  } catch {
    // повреждённые или недоступные данные — начинаем с чистого листа
  }
  return { runs: {}, scenarios: {}, auto: false };
}

function write(employeeId: number, store: Store): void {
  const runIds = Object.keys(store.runs);
  for (const runId of runIds.slice(0, Math.max(0, runIds.length - MAX_RUNS))) delete store.runs[runId];
  try {
    localStorage.setItem(storageKey(employeeId), JSON.stringify(store));
  } catch {
    // без localStorage прогресс живёт до закрытия вкладки
  }
}

export function loadProgress(employeeId: number, runId: string): RunProgress | null {
  return read(employeeId).runs[runId] ?? null;
}

export function saveProgress(employeeId: number, runId: string, progress: RunProgress): void {
  const store = read(employeeId);
  delete store.runs[runId];
  store.runs[runId] = progress;
  write(employeeId, store);
}

export function scenarioMemory(employeeId: number, scenarioId: string): ScenarioMemory {
  return read(employeeId).scenarios[scenarioId] ?? { seenNodes: [], endings: [], playthroughs: 0 };
}

export function rememberScene(employeeId: number, scenarioId: string, nodeId: string): void {
  const store = read(employeeId);
  const memory = store.scenarios[scenarioId] ?? { seenNodes: [], endings: [], playthroughs: 0 };
  if (!memory.seenNodes.includes(nodeId)) memory.seenNodes.push(nodeId);
  store.scenarios[scenarioId] = memory;
  write(employeeId, store);
}

export function rememberEnding(employeeId: number, scenarioId: string, runId: string, ending: string): void {
  const store = read(employeeId);
  const memory = store.scenarios[scenarioId] ?? { seenNodes: [], endings: [], playthroughs: 0 };
  // Финал одного прохождения засчитываем один раз, даже если экран открыли повторно
  if (store.runs[runId]?.ending === null || !store.runs[runId]) memory.playthroughs += 1;
  if (!memory.endings.includes(ending)) memory.endings.push(ending);
  store.scenarios[scenarioId] = memory;
  if (store.runs[runId]) store.runs[runId].ending = ending;
  write(employeeId, store);
}

export function loadAuto(employeeId: number): boolean {
  return read(employeeId).auto;
}

export function loadMuted(employeeId: number): boolean {
  return read(employeeId).muted ?? false;
}

export function saveMuted(employeeId: number, muted: boolean): void {
  const store = read(employeeId);
  store.muted = muted;
  write(employeeId, store);
}

export function saveAuto(employeeId: number, auto: boolean): void {
  const store = read(employeeId);
  store.auto = auto;
  write(employeeId, store);
}
