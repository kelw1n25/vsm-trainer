import { api, ApiError } from "../api";
import type { Expression, Line, RunState, Scene } from "../types";
import * as storage from "./persistence";
import type { HistoryEntry } from "./persistence";

/**
 * Движок визуальной новеллы на стороне браузера. Не зависит от React: хранит состояние показа
 * и сообщает о его изменениях подписчикам (интерфейс подключается через useSyncExternalStore).
 *
 * Сервер решает, что происходит в истории: какие варианты доступны, куда ведёт выбор, когда
 * истекает таймер. Движок отвечает за подачу: очередь реплик, печать текста, смену сцен, журнал,
 * автопрокрутку, пропуск уже виденного и сохранение места, где игрок остановился.
 */

export type Phase = "loading" | "intro" | "dialogue" | "choices" | "ending" | "error";

export interface StoryView {
  phase: Phase;
  run: RunState | null;
  scene: Scene | null;
  /** Текущее выражение лица каждого персонажа в сцене. */
  expressions: Record<string, Expression>;
  line: Line | null;
  /** Сколько символов реплики уже напечатано. */
  shownChars: number;
  typing: boolean;
  history: HistoryEntry[];
  auto: boolean;
  /** Сцену уже видели в прошлых прохождениях — её можно пропустить до выбора. */
  canSkip: boolean;
  busy: boolean;
  notice: string | null;
  /** Разница серверных и клиентских часов — таймер считается по серверному времени. */
  clockOffsetMs: number;
}

/** Кто проигрывает звуки реплик — движок не знает, как именно (в браузере это StoryAudio). */
export interface SoundPlayer {
  play(name: string): void;
  /** «Голос» персонажа во время печати реплики. */
  talk(speaker: string, soft?: boolean): void;
}

// Щелчок голоса — на каждый третий напечатанный символ, кроме пробелов и знаков препинания
const TALK_EVERY_CHARS = 3;

interface Segment {
  scene: Scene;
  /** Узел сценария; у реакции на выбор — null (она звучит в сцене, где был сделан выбор). */
  nodeId: string | null;
  lines: Line[];
}

const TYPE_INTERVAL_MS = 28;
// Пауза после конца предложения — реплика «дышит», как в живой речи
const SENTENCE_PAUSE_TICKS = 9;
const INTRO_MS = 2400;
const AUTO_BASE_MS = 1100;
const AUTO_PER_CHAR_MS = 32;

function motionReduced(): boolean {
  return (
    document.documentElement.hasAttribute("data-reduce-motion") ||
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

export class StoryEngine {
  private view: StoryView;
  private readonly listeners = new Set<() => void>();
  private queue: Segment[] = [];
  private segmentIndex = 0;
  private lineIndex = 0;
  private typingTimer: ReturnType<typeof setInterval> | undefined;
  private autoTimer: ReturnType<typeof setTimeout> | undefined;
  private introTimer: ReturnType<typeof setTimeout> | undefined;
  private timeoutTimer: ReturnType<typeof setTimeout> | undefined;
  private pauseTicks = 0;
  private seenBefore: Set<string> = new Set();
  private choices: string[] = [];
  // Номер запуска: ответ сервера на запрос прошлого запуска (после dispose) игнорируется
  private generation = 0;

  constructor(
    private readonly runId: string,
    private readonly employeeId: number,
    private readonly playerName: string,
    private readonly sounds: SoundPlayer | null = null,
  ) {
    this.view = {
      phase: "loading",
      run: null,
      scene: null,
      expressions: {},
      line: null,
      shownChars: 0,
      typing: false,
      history: [],
      auto: storage.loadAuto(employeeId),
      canSkip: false,
      busy: false,
      notice: null,
      clockOffsetMs: 0,
    };
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): StoryView => this.view;

  private update(patch: Partial<StoryView>): void {
    this.view = { ...this.view, ...patch };
    this.listeners.forEach((listener) => listener());
  }

  private setRun(run: RunState): void {
    this.update({ run, clockOffsetMs: Date.parse(run.server_time) - Date.now() });
  }

  /** Загружает прохождение и продолжает с того места, где игрок остановился. */
  async startScenario(): Promise<void> {
    const generation = ++this.generation;
    let run: RunState;
    try {
      run = await api.getRun(this.runId);
    } catch (error) {
      if (generation !== this.generation) return;
      this.update({ phase: "error", notice: error instanceof ApiError ? error.message : "Не удалось загрузить сценарий" });
      return;
    }
    if (generation !== this.generation) return;
    this.setRun(run);
    this.seenBefore = new Set(storage.scenarioMemory(this.employeeId, run.scenario_id).seenNodes);
    const progress = storage.loadProgress(this.employeeId, this.runId);
    this.choices = progress?.choices ?? [];
    this.update({ history: progress?.history ?? [] });

    if (run.status !== "in_progress") {
      this.finishScenario();
      return;
    }
    const node = run.node!;
    if (node.choices_shown) {
      // Варианты уже показаны и таймер идёт — сразу к выбору
      this.update({ scene: node.scene, expressions: this.expressionsAfter(node.scene, node.dialogue) });
      this.showChoices();
      return;
    }
    this.queue = [{ scene: node.scene, nodeId: node.id, lines: node.dialogue }];
    const resumeAt = progress?.nodeId === node.id ? Math.min(progress.lineIndex, node.dialogue.length - 1) : 0;
    if (progress) {
      // Реплика, на которой остановились, уже есть в журнале — покажем её снова, но не задвоим запись
      const last = progress.history[progress.history.length - 1];
      if (last?.text === node.dialogue[resumeAt]?.text) this.update({ history: progress.history.slice(0, -1) });
      this.playSegment(0, resumeAt);
      return;
    }
    // Новое прохождение: затемнение, название сценария, затем первая сцена
    this.update({ phase: "intro" });
    this.introTimer = setTimeout(() => this.playSegment(0, 0), motionReduced() ? 300 : INTRO_MS);
  }

  /** Строит очередь показа из ответа сервера: реакция на решение, затем новая сцена или финал. */
  loadScene(run: RunState, previousScene: Scene | null): void {
    this.setRun(run);
    const queue: Segment[] = [];
    for (const step of run.last_steps) {
      if (step.kind === "timeout") this.log({ kind: "timeout", speaker: null, text: "Время на решение истекло" });
      if (step.reaction.length && previousScene) queue.push({ scene: previousScene, nodeId: null, lines: step.reaction });
    }
    if (run.node) queue.push({ scene: run.node.scene, nodeId: run.node.id, lines: run.node.dialogue });
    else if (run.final) queue.push({ scene: run.final.scene, nodeId: null, lines: run.final.dialogue });
    this.queue = queue;
    this.playSegment(0, 0);
  }

  private playSegment(index: number, lineIndex: number): void {
    clearTimeout(this.introTimer);
    this.segmentIndex = index;
    const segment = this.queue[index];
    if (!segment) {
      this.endOfQueue();
      return;
    }
    const canSkip = segment.nodeId !== null && this.seenBefore.has(segment.nodeId);
    if (segment.nodeId && this.view.run) storage.rememberScene(this.employeeId, this.view.run.scenario_id, segment.nodeId);
    this.update({
      phase: "dialogue",
      scene: segment.scene,
      expressions: this.expressionsAfter(segment.scene, segment.lines.slice(0, lineIndex)),
      canSkip,
      notice: null,
    });
    if (!segment.lines.length) {
      this.playSegment(index + 1, 0);
      return;
    }
    this.showLine(lineIndex);
  }

  private showLine(index: number): void {
    const segment = this.queue[this.segmentIndex];
    this.lineIndex = index;
    const line = segment.lines[index];
    const expressions = line.expression ? { ...this.view.expressions, [line.speaker]: line.expression } : this.view.expressions;
    this.log({ kind: line.kind, speaker: this.speakerName(line), text: line.text });
    this.update({ line, expressions, shownChars: 0, typing: true });
    this.saveProgress();
    if (line.sound) this.sounds?.play(line.sound);

    clearInterval(this.typingTimer);
    clearTimeout(this.autoTimer);
    if (motionReduced()) {
      this.finishTyping();
      return;
    }
    this.pauseTicks = 0;
    this.typingTimer = setInterval(() => {
      if (this.pauseTicks > 0) {
        this.pauseTicks -= 1;
        return;
      }
      const shown = this.view.shownChars + 1;
      if (shown >= line.text.length) {
        this.finishTyping();
        return;
      }
      if (".!?…".includes(line.text[shown - 1]) && line.text[shown] === " ") this.pauseTicks = SENTENCE_PAUSE_TICKS;
      const char = line.text[shown - 1];
      if (line.kind !== "narration" && shown % TALK_EVERY_CHARS === 0 && /[\p{L}\p{N}]/u.test(char)) {
        this.sounds?.talk(line.speaker, line.kind === "thought");
      }
      this.update({ shownChars: shown });
    }, TYPE_INTERVAL_MS);
  }

  private finishTyping(): void {
    clearInterval(this.typingTimer);
    const line = this.view.line;
    this.update({ shownChars: line?.text.length ?? 0, typing: false });
    if (this.view.auto && line) {
      const delay = Math.min(4000, AUTO_BASE_MS + line.text.length * AUTO_PER_CHAR_MS);
      this.autoTimer = setTimeout(() => this.advance(), delay);
    }
  }

  /** «Далее» или клик по сцене: допечатать реплику сразу, а если она уже напечатана — следующая. */
  advance(): void {
    if (this.view.phase === "intro") {
      this.playSegment(0, this.lineIndex);
      return;
    }
    if (this.view.phase !== "dialogue") return;
    if (this.view.typing) {
      this.finishTyping();
      return;
    }
    clearTimeout(this.autoTimer);
    const segment = this.queue[this.segmentIndex];
    if (this.lineIndex + 1 < segment.lines.length) this.showLine(this.lineIndex + 1);
    else this.playSegment(this.segmentIndex + 1, 0);
  }

  /** Пропустить уже знакомую сцену до момента выбора. */
  skip(): void {
    if (!this.view.canSkip || this.view.phase !== "dialogue") return;
    clearInterval(this.typingTimer);
    clearTimeout(this.autoTimer);
    const segment = this.queue[this.segmentIndex];
    for (const line of segment.lines.slice(this.lineIndex + 1)) {
      this.log({ kind: line.kind, speaker: this.speakerName(line), text: line.text });
    }
    this.update({ expressions: this.expressionsAfter(segment.scene, segment.lines), typing: false });
    this.lineIndex = segment.lines.length - 1;
    this.playSegment(this.segmentIndex + 1, 0);
  }

  toggleAuto(): void {
    const auto = !this.view.auto;
    storage.saveAuto(this.employeeId, auto);
    this.update({ auto });
    if (auto && this.view.phase === "dialogue" && !this.view.typing) this.advance();
    if (!auto) clearTimeout(this.autoTimer);
  }

  private endOfQueue(): void {
    const run = this.view.run;
    if (run?.node) this.showChoices();
    else if (run?.final) this.finishScenario();
  }

  /** Сцена дочитана: сервер показывает варианты и запускает таймер. */
  async showChoices(): Promise<void> {
    const run = this.view.run;
    if (!run?.node) return;
    clearTimeout(this.autoTimer);
    this.update({ phase: "choices", line: null, typing: false, canSkip: false });
    if (!run.node.choices_shown) {
      this.update({ busy: true });
      try {
        this.setRun(await api.reveal(run.id, run.node.id));
      } catch (error) {
        await this.recover(error);
        return;
      } finally {
        this.update({ busy: false });
      }
    }
    this.saveProgress();
    this.watchTimeout();
  }

  /** Истечение таймера применяет сервер — в момент дедлайна запрашиваем, что произошло. */
  private watchTimeout(): void {
    clearTimeout(this.timeoutTimer);
    const run = this.view.run;
    const timeoutAt = run?.node?.timeout_at;
    if (!run || !timeoutAt) return;
    const delay = Date.parse(timeoutAt) - this.view.clockOffsetMs - Date.now() + 300;
    this.timeoutTimer = setTimeout(() => this.refresh(), Math.max(0, delay));
  }

  private async refresh(): Promise<void> {
    const before = this.view.run;
    try {
      const run = await api.getRun(this.runId);
      if (run.node?.id === before?.node?.id && run.status === "in_progress") {
        this.setRun(run);
        this.watchTimeout();
      } else {
        this.loadScene(run, this.view.scene);
      }
    } catch (error) {
      this.update({ notice: error instanceof ApiError ? error.message : "Нет связи с сервером" });
    }
  }

  /** Решение игрока: сервер применяет последствия, а история продолжается реакцией персонажей. */
  async goToScene(choiceId: string): Promise<void> {
    const run = this.view.run;
    if (!run?.node || this.view.busy || this.view.phase !== "choices") return;
    const choice = run.node.choices.find((item) => item.id === choiceId);
    if (!choice) return;
    clearTimeout(this.timeoutTimer);
    this.update({ busy: true });
    try {
      const next = await api.choose(run.id, run.node.id, choiceId);
      this.choices = [...this.choices, choiceId];
      this.log({ kind: "choice", speaker: this.playerName, text: choice.text });
      this.update({ busy: false });
      this.loadScene(next, this.view.scene);
    } catch (error) {
      this.update({ busy: false });
      await this.recover(error);
    }
  }

  private async recover(error: unknown): Promise<void> {
    if (error instanceof ApiError && ["time_expired", "stale_node", "run_finished", "choices_not_shown"].includes(error.code)) {
      this.update({ notice: error.message });
      await this.refresh();
      return;
    }
    this.update({ notice: error instanceof ApiError ? error.message : "Не удалось связаться с сервером" });
  }

  finishScenario(): void {
    clearTimeout(this.timeoutTimer);
    clearTimeout(this.autoTimer);
    const run = this.view.run;
    if (run?.final) storage.rememberEnding(this.employeeId, run.scenario_id, this.runId, run.final.ending);
    this.update({ phase: "ending", line: null, typing: false, canSkip: false });
  }

  /** Новое прохождение того же сценария — возвращает id нового прохождения. */
  async restartScenario(): Promise<string> {
    const run = await api.startRun(this.view.run!.scenario_id);
    return run.id;
  }

  saveProgress(): void {
    const run = this.view.run;
    if (!run) return;
    storage.saveProgress(this.employeeId, this.runId, {
      scenarioId: run.scenario_id,
      nodeId: run.node?.id ?? "",
      lineIndex: this.queue[this.segmentIndex]?.nodeId ? this.lineIndex : 0,
      history: this.view.history,
      choices: this.choices,
      ending: storage.loadProgress(this.employeeId, this.runId)?.ending ?? null,
    });
  }

  dispose(): void {
    this.generation += 1;
    clearInterval(this.typingTimer);
    clearTimeout(this.autoTimer);
    clearTimeout(this.introTimer);
    clearTimeout(this.timeoutTimer);
  }

  private log(entry: HistoryEntry): void {
    this.update({ history: [...this.view.history, entry] });
  }

  private speakerName(line: Line): string | null {
    if (line.kind === "narration") return null;
    return line.speaker === "player" ? this.playerName : line.name;
  }

  private expressionsAfter(scene: Scene, lines: Line[]): Record<string, Expression> {
    const expressions: Record<string, Expression> = {};
    for (const character of scene.characters) expressions[character.id] = character.expression;
    for (const line of lines) if (line.expression) expressions[line.speaker] = line.expression;
    return expressions;
  }
}
