import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { api, ApiError } from "../api";
import { CompetenceList } from "../components/CompetenceList";
import { ScaleChart } from "../components/ScaleChart";
import { outcomeLabels, signed } from "../labels";
import type { Debrief, DebriefStep } from "../types";

export function DebriefPage() {
  const { runId } = useParams() as { runId: string };
  const [debrief, setDebrief] = useState<Debrief | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .debrief(runId)
      .then(setDebrief)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить разбор"));
  }, [runId]);

  if (error) return <p className="error">{error}</p>;
  if (!debrief) return <p className="muted">Загрузка…</p>;

  const points = [
    { label: "Старт", loyalty: debrief.initial_loyalty, safety: debrief.initial_safety },
    ...debrief.steps.map((step, index) => ({
      label: `Шаг ${index + 1}`,
      loyalty: step.loyalty_after,
      safety: step.safety_after,
    })),
  ];

  return (
    <div className="stack">
      <h1 className="page-title">Разбор: {debrief.scenario_title}</h1>

      <section className={`card final final--${debrief.outcome}`}>
        <h2>{outcomeLabels[debrief.outcome]}</h2>
        <p>{debrief.final_text}</p>
        <div className="stats">
          <Stat value={`${debrief.best_decisions} из ${debrief.decisions}`} label="лучших решений" />
          <Stat value={String(debrief.timeouts)} label="истёкших таймеров" />
          <Stat
            value={debrief.average_reaction_seconds === null ? "—" : `${debrief.average_reaction_seconds} с`}
            label="среднее время решения"
          />
          <Stat value={`+${debrief.xp_earned}`} label="XP" />
        </div>
        <CompetenceList points={debrief.competence_points} />
      </section>

      <section className="card">
        <h2>Как менялись шкалы</h2>
        <ScaleChart points={points} />
      </section>

      {debrief.steps.map((step, index) => (
        <StepReview key={index} step={step} index={index} />
      ))}

      <div className="actions">
        <Link className="button" to="/scenarios">
          Вернуться к сценариям
        </Link>
      </div>
    </div>
  );
}

function Stat({ value, label }: { value: string; label: string }) {
  return (
    <div className="stat">
      <strong>{value}</strong>
      <span className="muted">{label}</span>
    </div>
  );
}

function StepReview({ step, index }: { step: DebriefStep; index: number }) {
  const verdict = step.kind === "timeout" ? "timeout" : step.was_best ? "best" : "other";
  return (
    <section className={`card review review--${verdict}`}>
      <p className="muted">
        Шаг {index + 1}
        {step.elapsed_seconds !== null && step.timer_seconds !== null &&
          ` · решение за ${step.elapsed_seconds.toFixed(1)} из ${step.timer_seconds} с`}
      </p>
      <p>{step.situation}</p>
      <p>
        <strong>{step.kind === "timeout" ? "⏱ Время истекло, решение не принято" : `Ваш выбор: ${step.chosen_text}`}</strong>
      </p>
      <p>
        <span className={step.loyalty_delta < 0 ? "negative" : "positive"}>Лояльность {signed(step.loyalty_delta)}</span>
        {" · "}
        <span className={step.safety_delta < 0 ? "negative" : "positive"}>Безопасность {signed(step.safety_delta)}</span>
      </p>
      <CompetenceList points={step.competences} />
      {step.explanation && <p className="explanation">{step.explanation}</p>}
      {!step.was_best && step.best_text && (
        <div className="best">
          <strong>Лучший вариант: {step.best_text}</strong>
          <p>{step.best_explanation}</p>
        </div>
      )}
    </section>
  );
}
