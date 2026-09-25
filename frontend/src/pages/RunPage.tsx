import { useCallback, useEffect, useState, type CSSProperties } from "react";
import { Link, useParams } from "react-router";
import { api, ApiError } from "../api";
import { AnimatedNumber } from "../components/AnimatedNumber";
import { CompetenceList } from "../components/CompetenceList";
import { DifficultyIndicator } from "../components/DifficultyIndicator";
import { ArrowRightIcon, ChevronLeftIcon, PinIcon } from "../components/icons";
import { Rewards } from "../components/Rewards";
import { ScaleBar } from "../components/ScaleBar";
import { Timer } from "../components/Timer";
import { useNow } from "../hooks";
import { categoryLabels, outcomeLabels, signed, speakerLabels } from "../labels";
import type { RunState, Step } from "../types";

// Клиентские часы могут отличаться от серверных: запоминаем разницу при каждом ответе
interface Loaded {
  run: RunState;
  clockOffsetMs: number;
}

function toLoaded(run: RunState): Loaded {
  return { run, clockOffsetMs: Date.parse(run.server_time) - Date.now() };
}

const LETTERS = "АБВГДЕ";

export function RunPage() {
  const { runId } = useParams() as { runId: string };
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const now = useNow(200);

  const refresh = useCallback(() => {
    api
      .getRun(runId)
      .then((run) => setLoaded(toLoaded(run)))
      .catch((e) => setNotice(e instanceof ApiError ? e.message : "Не удалось загрузить сценарий"));
  }, [runId]);

  useEffect(refresh, [refresh]);

  // Когда сервер применит истечение таймера, запрашиваем новое состояние
  const timeoutAt = loaded?.run.node?.timeout_at;
  useEffect(() => {
    if (!loaded || !timeoutAt) return;
    const delay = Date.parse(timeoutAt) - loaded.clockOffsetMs - Date.now() + 300;
    const id = setTimeout(refresh, Math.max(0, delay));
    return () => clearTimeout(id);
  }, [loaded, timeoutAt, refresh]);

  if (!loaded) return notice ? <p className="error">{notice}</p> : <p className="muted">Загрузка…</p>;

  const { run, clockOffsetMs } = loaded;
  const node = run.node;
  const remainingMs = node?.deadline_at ? Math.max(0, Date.parse(node.deadline_at) - clockOffsetMs - now) : null;
  const timeIsUp = remainingMs === 0;

  async function choose(choiceId: string) {
    if (!node) return;
    setSelected(choiceId);
    setNotice(null);
    try {
      setLoaded(toLoaded(await api.choose(run.id, node.id, choiceId)));
    } catch (e) {
      if (e instanceof ApiError && ["time_expired", "stale_node", "run_finished"].includes(e.code)) {
        setNotice(e.message);
        refresh();
      } else {
        setNotice(e instanceof ApiError ? e.message : "Не удалось отправить ответ");
      }
    } finally {
      setSelected(null);
    }
  }

  return (
    <div className="stack run">
      <Link to={`/scenarios/${run.scenario_id}`} className="back-link">
        <ChevronLeftIcon /> К сценарию
      </Link>

      <section className="card run-head">
        <div className="tags">
          <span className="tag tag--category">{categoryLabels[run.category] ?? run.category}</span>
          <span className="tag">Шаг {run.steps_taken + (node ? 1 : 0)}</span>
        </div>
        <h1 className="page-title">{run.scenario_title}</h1>
        <div className="run-head__meta">
          <p className="route">
            <PinIcon />
            {run.route} · {run.service_class}
          </p>
          <DifficultyIndicator level={run.difficulty} />
        </div>
      </section>

      <section className="card hud">
        <ScaleBar label="Лояльность пассажира" value={run.loyalty} kind="loyalty" />
        <ScaleBar label="Рейтинг безопасности" value={run.safety} kind="safety" />
        {node?.timer_seconds && remainingMs !== null && (
          <Timer remainingMs={remainingMs} totalSeconds={node.timer_seconds} />
        )}
      </section>

      {run.last_steps.map((step, index) => (
        <StepFeedback key={index} step={step} />
      ))}
      {notice && <p className="notice">{notice}</p>}
      <Rewards achievements={run.new_achievements} levelUp={run.level_up} />

      {node && (
        <section className="card situation" key={node.id}>
          <p className="situation__text">{node.situation}</p>
          {node.line && (
            <blockquote className="line">
              <span className="line__speaker">
                {speakerLabels[node.line.speaker]}
                {node.line.name && ` · ${node.line.name}`}
              </span>
              «{node.line.text}»
            </blockquote>
          )}
          <h2 className="situation__question">Что вы будете делать?</h2>
          <div className="answers">
            {node.choices.map((choice, index) => (
              <button
                key={choice.id}
                style={{ "--i": index } as CSSProperties}
                className={`answer ${selected === choice.id ? "answer--selected" : ""}`}
                disabled={selected !== null || timeIsUp}
                onClick={() => choose(choice.id)}
              >
                <span className="answer__letter">{LETTERS[index]}</span>
                <span>{choice.text}</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {run.final && (
        <section className={`card final final--${run.final.outcome}`}>
          <p className="hero__eyebrow">Результат</p>
          <h2 className="page-title">{outcomeLabels[run.final.outcome]}</h2>
          <p>{run.final.text}</p>
          <div className="kpis kpis--compact">
            <div className="kpi">
              <span className="kpi__label">Получено баллов</span>
              <strong className="kpi__value">
                +<AnimatedNumber value={run.final.xp_earned} suffix=" XP" />
              </strong>
            </div>
            <div className="kpi">
              <span className="kpi__label">Итоговые шкалы</span>
              <strong className="kpi__value">
                {run.loyalty} / {run.safety}
              </strong>
            </div>
            <div className="kpi">
              <span className="kpi__label">Шагов пройдено</span>
              <strong className="kpi__value">{run.steps_taken}</strong>
            </div>
          </div>
          <CompetenceList points={run.final.competence_points} />
          <div className="actions">
            <Link className="button" to={`/runs/${run.id}/debrief`}>
              Разбор решений <ArrowRightIcon />
            </Link>
            <Link className="button button--ghost" to="/scenarios">
              Вернуться к сценариям
            </Link>
          </div>
        </section>
      )}
    </div>
  );
}

function StepFeedback({ step }: { step: Step }) {
  const good = step.loyalty_delta + step.safety_delta >= 0;
  return (
    <div className={`feedback ${step.kind === "timeout" ? "feedback--timeout" : good ? "feedback--good" : "feedback--bad"}`}>
      <strong>{step.kind === "timeout" ? "⏱ Время на решение истекло" : good ? "Решение принято" : "Решение принято — есть потери"}</strong>
      <span className={step.loyalty_delta < 0 ? "negative" : "positive"}>Лояльность {signed(step.loyalty_delta)}</span>
      <span className={step.safety_delta < 0 ? "negative" : "positive"}>Безопасность {signed(step.safety_delta)}</span>
      <CompetenceList points={step.competences} inline />
    </div>
  );
}
