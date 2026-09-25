import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { api, ApiError } from "../api";
import { CompetenceList } from "../components/CompetenceList";
import { Rewards } from "../components/Rewards";
import { ScaleBar } from "../components/ScaleBar";
import { Timer } from "../components/Timer";
import { useNow } from "../hooks";
import { outcomeLabels, signed, speakerLabels } from "../labels";
import type { RunState, Step } from "../types";

// Клиентские часы могут отличаться от серверных: запоминаем разницу при каждом ответе
interface Loaded {
  run: RunState;
  clockOffsetMs: number;
}

function toLoaded(run: RunState): Loaded {
  return { run, clockOffsetMs: Date.parse(run.server_time) - Date.now() };
}

export function RunPage() {
  const { runId } = useParams() as { runId: string };
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
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
    setSending(true);
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
      setSending(false);
    }
  }

  return (
    <div className="run">
      <h1>{run.scenario_title}</h1>
      <section className="hud card">
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
        <section className="card situation">
          <p>{node.situation}</p>
          {node.line && (
            <blockquote className="line">
              <span className="line__speaker">
                {speakerLabels[node.line.speaker]}
                {node.line.name && ` · ${node.line.name}`}
              </span>
              «{node.line.text}»
            </blockquote>
          )}
          <div className="choices">
            {node.choices.map((choice) => (
              <button
                key={choice.id}
                className="choice"
                disabled={sending || timeIsUp}
                onClick={() => choose(choice.id)}
              >
                {choice.text}
              </button>
            ))}
          </div>
        </section>
      )}

      {run.final && (
        <section className={`card final final--${run.final.outcome}`}>
          <h2>{outcomeLabels[run.final.outcome]}</h2>
          <p>{run.final.text}</p>
          <p>
            <strong>+{run.final.xp_earned} XP</strong>
          </p>
          <CompetenceList points={run.final.competence_points} />
          <div className="actions">
            <Link className="button" to={`/runs/${run.id}/debrief`}>
              Разбор решений
            </Link>
            <Link className="button button--ghost" to="/">
              К сценариям
            </Link>
          </div>
        </section>
      )}
    </div>
  );
}

function StepFeedback({ step }: { step: Step }) {
  return (
    <div className={`feedback ${step.kind === "timeout" ? "feedback--timeout" : ""}`}>
      <span>{step.kind === "timeout" ? "⏱ Время на решение истекло" : "Последствия решения"}</span>
      <span className={step.loyalty_delta < 0 ? "negative" : "positive"}>
        Лояльность {signed(step.loyalty_delta)}
      </span>
      <span className={step.safety_delta < 0 ? "negative" : "positive"}>
        Безопасность {signed(step.safety_delta)}
      </span>
    </div>
  );
}
