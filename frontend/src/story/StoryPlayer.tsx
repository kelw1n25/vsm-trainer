import { useEffect, useMemo, useRef, useState, useSyncExternalStore, type CSSProperties } from "react";
import { Link, useParams } from "react-router";
import { useAuth } from "../auth";
import { useNow } from "../hooks";
import { categoryLabels, firstName, signed } from "../labels";
import type { Character, Line, RunState } from "../types";
import { StoryAudio } from "./audio";
import { loadMuted, saveMuted } from "./persistence";
import { StoryEnd } from "./StoryEnd";
import { StoryEngine } from "./StoryEngine";
import { StoryBackground, StoryCast } from "./StoryStage";

/** Полноэкранный режим визуальной новеллы: сцена, персонажи, диалог и выбор. */
export function StoryPlayer() {
  const { runId } = useParams() as { runId: string };
  const { session } = useAuth();
  const playerName = session ? firstName(session.fullName) : "Проводник";
  const employeeId = session!.employeeId;
  const [muted, setMuted] = useState(() => loadMuted(employeeId));
  const audio = useMemo(() => new StoryAudio(loadMuted(employeeId)), [employeeId]);
  const engine = useMemo(() => new StoryEngine(runId, employeeId, playerName, audio), [runId, employeeId, playerName, audio]);
  const view = useSyncExternalStore(engine.subscribe, engine.getSnapshot);
  const [historyOpen, setHistoryOpen] = useState(false);

  useEffect(() => {
    // Переход с дашборда затемнил экран — теперь новелла проявляется из темноты
    document.documentElement.classList.remove("story-leaving");
    engine.startScenario();
    return () => engine.dispose();
  }, [engine]);

  useEffect(() => {
    audio.startMusic();
    // Если браузер не дал включить звук сразу, музыка начнётся с первого действия на экране
    const resume = () => audio.resume();
    window.addEventListener("pointerdown", resume);
    window.addEventListener("keydown", resume);
    return () => {
      window.removeEventListener("pointerdown", resume);
      window.removeEventListener("keydown", resume);
      audio.stop();
    };
  }, [audio]);

  function toggleSound() {
    const next = !muted;
    setMuted(next);
    saveMuted(employeeId, next);
    audio.setMuted(next);
  }

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.target instanceof HTMLElement && event.target.closest("button, a, input")) return;
      if (event.key === "Escape") setHistoryOpen(false);
      if (historyOpen) return;
      if (event.key === " " || event.key === "Enter") {
        event.preventDefault();
        engine.advance();
      }
      const number = Number(event.key);
      const choices = engine.getSnapshot().run?.node?.choices ?? [];
      if (engine.getSnapshot().phase === "choices" && number >= 1 && number <= choices.length) {
        engine.goToScene(choices[number - 1].id);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [engine, historyOpen]);

  const { run, scene, phase, line } = view;
  const cast = useMemo(
    () => Object.fromEntries((run?.characters ?? []).map((character) => [character.id, character])),
    [run?.characters],
  );

  // Высота голоса: детский и женский выше, мужской ниже, у проводника — ровный средний
  useEffect(() => {
    const voices: Record<string, number> = { player: 190 };
    for (const character of run?.characters ?? []) {
      const { look } = character;
      voices[character.id] = look.child ? 330 : look.hair_style === "short" ? 150 : 250;
    }
    audio.setVoices(voices);
  }, [audio, run?.characters]);

  if (phase === "error") {
    return (
      <div className="story story--message">
        <p>{view.notice}</p>
        <Link className="button" to="/scenarios">
          К сценариям
        </Link>
      </div>
    );
  }
  if (!run || !scene) return <div className="story story--message" aria-busy="true" />;

  return (
    <div className="story" data-phase={phase}>
      <StoryBackground name={scene.background} />
      <div className="story__shade" />
      <StoryCast
        characters={scene.characters}
        expressions={view.expressions}
        cast={cast}
        speaker={line?.speaker ?? null}
        speaking={view.typing && line?.kind === "speech"}
      />

      <StoryBar
        run={run}
        engine={engine}
        auto={view.auto}
        canSkip={view.canSkip}
        muted={muted}
        onSound={toggleSound}
        onHistory={() => setHistoryOpen(true)}
      />

      {/* Клик по сцене — то же, что «Далее»: мгновенно допечатывает реплику или листает дальше */}
      {phase === "dialogue" && <button className="story__advance-area" aria-label="Далее" onClick={() => engine.advance()} />}

      {phase === "dialogue" && line && (
        <DialogueBox line={line} shown={view.shownChars} typing={view.typing} cast={cast} playerName={playerName} onNext={() => engine.advance()} />
      )}

      {phase === "choices" && run.node && (
        <Choices run={run} engine={engine} busy={view.busy} clockOffsetMs={view.clockOffsetMs} />
      )}

      {view.notice && phase !== "ending" && <p className="story__notice">{view.notice}</p>}

      {phase === "intro" && (
        <button className="story-intro" onClick={() => engine.advance()}>
          <span className="story-intro__eyebrow">
            {categoryLabels[run.category]} · {run.service_class} · {run.route}
          </span>
          <span className="story-intro__title">{run.scenario_title}</span>
        </button>
      )}

      {phase === "ending" && <StoryEnd run={run} engine={engine} />}

      {historyOpen && <HistoryPanel entries={view.history} onClose={() => setHistoryOpen(false)} />}
    </div>
  );
}

interface BarProps {
  run: RunState;
  engine: StoryEngine;
  auto: boolean;
  canSkip: boolean;
  muted: boolean;
  onSound: () => void;
  onHistory: () => void;
}

function StoryBar({ run, engine, auto, canSkip, muted, onSound, onHistory }: BarProps) {
  // Точки истории: пройденные шаги, текущий и ближайший путь до финала — без карты будущих развилок
  const total = run.steps_taken + Math.max(run.steps_left, run.node ? 1 : 0);
  return (
    <header className="story-bar">
      <Link className="story-bar__exit" to={`/scenarios/${run.scenario_id}`} title="Прогресс сохранится">
        ← Выйти
      </Link>
      <div className="story-bar__title">
        <span>{run.scenario_title}</span>
        <span className="story-dots" aria-label={`Шаг ${run.steps_taken + 1}`}>
          {Array.from({ length: total }, (_, i) => (
            <i key={i} className={i < run.steps_taken ? "done" : i === run.steps_taken && run.node ? "current" : ""} />
          ))}
        </span>
      </div>
      <Scales run={run} />
      <div className="story-bar__tools">
        <button onClick={onHistory}>История</button>
        <button className={muted ? "" : "is-on"} aria-pressed={!muted} title="Музыка и звуки реплик" onClick={onSound}>
          {muted ? "Звук выкл." : "Звук"}
        </button>
        <button className={auto ? "is-on" : ""} aria-pressed={auto} onClick={() => engine.toggleAuto()}>
          Авто
        </button>
        <button disabled={!canSkip} title={canSkip ? "Эту сцену вы уже видели" : "Пропуск доступен для уже просмотренных сцен"} onClick={() => engine.skip()}>
          Пропустить
        </button>
      </div>
    </header>
  );
}

/** Две шкалы из ТЗ: лояльность пассажира и рейтинг безопасности. После решения видно, как они изменились. */
function Scales({ run }: { run: RunState }) {
  const step = run.last_steps[run.last_steps.length - 1];
  return (
    <div className="story-scales">
      <Scale label="Лояльность" value={run.loyalty} delta={step?.loyalty_delta ?? 0} kind="loyalty" stepKey={run.steps_taken} />
      <Scale label="Безопасность" value={run.safety} delta={step?.safety_delta ?? 0} kind="safety" stepKey={run.steps_taken} />
    </div>
  );
}

function Scale({ label, value, delta, kind, stepKey }: { label: string; value: number; delta: number; kind: string; stepKey: number }) {
  return (
    <div className={`story-scale story-scale--${kind}`}>
      <span className="story-scale__label">{label}</span>
      <span className="story-scale__track">
        <span className="story-scale__fill" style={{ width: `${value}%` }} />
      </span>
      <span className="story-scale__value">{value}</span>
      {delta !== 0 && (
        <span key={stepKey} className={`story-scale__delta ${delta < 0 ? "is-down" : "is-up"}`}>
          {signed(delta)}
        </span>
      )}
    </div>
  );
}

interface DialogueProps {
  line: Line;
  shown: number;
  typing: boolean;
  cast: Record<string, Character>;
  playerName: string;
  onNext: () => void;
}

function DialogueBox({ line, shown, typing, cast, playerName, onNext }: DialogueProps) {
  const name = line.kind === "narration" ? "Рассказчик" : line.speaker === "player" ? playerName : (line.name ?? cast[line.speaker]?.name);
  const role = line.kind === "narration" ? null : line.role;
  return (
    <section className={`story-box story-box--${line.kind}`} aria-live="polite">
      <header className="story-box__name">
        <strong>{name}</strong>
        {role && <span>{role}</span>}
        {line.kind === "thought" && <span>мысли</span>}
      </header>
      <p className="story-box__text">
        {line.kind === "speech" && "«"}
        {line.text.slice(0, shown)}
        {line.kind === "speech" && !typing && "»"}
        {/* Невидимый остаток держит высоту окна, чтобы текст не прыгал при печати */}
        <span className="story-box__ghost">{line.text.slice(shown)}</span>
      </p>
      <button className="story-box__next" onClick={onNext}>
        {typing ? "Показать" : "Далее"} <span aria-hidden="true">→</span>
      </button>
    </section>
  );
}

interface ChoicesProps {
  run: RunState;
  engine: StoryEngine;
  busy: boolean;
  clockOffsetMs: number;
}

function Choices({ run, engine, busy, clockOffsetMs }: ChoicesProps) {
  const node = run.node!;
  const now = useNow(100);
  const [picked, setPicked] = useState<string | null>(null);
  const remainingMs = node.deadline_at ? Math.max(0, Date.parse(node.deadline_at) - clockOffsetMs - now) : null;
  const firstButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    setPicked(null);
    firstButton.current?.focus({ preventScroll: true });
  }, [node.id, node.choices.length]);

  return (
    <section className="story-choices" aria-label="Ваше решение">
      {node.timer_seconds && remainingMs !== null && (
        <div className={`story-timer ${remainingMs < 5000 ? "story-timer--hurry" : ""}`} role="timer">
          <span className="story-timer__fill" style={{ width: `${(remainingMs / (node.timer_seconds * 1000)) * 100}%` }} />
          <span className="story-timer__text">{Math.ceil(remainingMs / 1000)} с на решение</span>
        </div>
      )}
      {!node.choices.length && <p className="story-choices__wait">…</p>}
      {node.choices.map((choice, index) => (
        <button
          key={choice.id}
          ref={index === 0 ? firstButton : undefined}
          // Варианты появляются по очереди: 100, 200, 300 мс
          style={{ "--i": index } as CSSProperties}
          className={`story-choice ${picked === choice.id ? "is-picked" : ""}`}
          disabled={busy || picked !== null || remainingMs === 0}
          onClick={() => {
            setPicked(choice.id);
            engine.goToScene(choice.id);
          }}
        >
          <span className="story-choice__key">{index + 1}</span>
          <span>{choice.text}</span>
        </button>
      ))}
    </section>
  );
}

function HistoryPanel({ entries, onClose }: { entries: { kind: string; speaker: string | null; text: string }[]; onClose: () => void }) {
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    // Фигурные скобки обязательны: в новых браузерах scrollIntoView возвращает Promise, а не функцию очистки
    end.current?.scrollIntoView({ block: "end" });
  }, []);
  return (
    <div className="story-history" role="dialog" aria-label="История диалогов">
      <header>
        <strong>История</strong>
        <button onClick={onClose}>Закрыть</button>
      </header>
      <div className="story-history__list">
        {entries.map((entry, index) => (
          <p key={index} className={`story-history__item story-history__item--${entry.kind}`}>
            {entry.kind === "choice" && <span className="story-history__who">Ваше решение</span>}
            {entry.kind !== "choice" && entry.speaker && <span className="story-history__who">{entry.speaker}</span>}
            {entry.text}
          </p>
        ))}
        <div ref={end} />
      </div>
    </div>
  );
}
