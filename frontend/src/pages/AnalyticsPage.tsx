import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, ApiError } from "../api";
import type { Analytics } from "../types";

function percent(value: number | null): string {
  return value === null ? "—" : `${Math.round(value * 100)}%`;
}

/** Аналитика своя (/analytics) или проводника для инструктора (/team/:employeeId). */
export function AnalyticsPage() {
  const { employeeId } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState<Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const own = employeeId === undefined;

  useEffect(() => {
    setData(null);
    (own ? api.myAnalytics() : api.employeeAnalytics(employeeId))
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить аналитику"));
  }, [own, employeeId]);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Загрузка…</p>;

  async function startRecommended(scenarioId: string) {
    try {
      const run = await api.startRun(scenarioId);
      navigate(`/runs/${run.id}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось начать сценарий");
    }
  }

  const progress = data.progress.map((week) => ({
    week: new Date(week.week_start).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" }),
    xp: week.xp,
  }));
  const maxPoints = Math.max(1, ...data.competences.map((c) => c.points));

  return (
    <div className="run">
      <h1>{own ? "Моя аналитика" : `Аналитика: ${data.full_name}`}</h1>

      {data.recommendation && (
        <section className="card recommendation">
          <h2>Рекомендуем: {data.recommendation.title}</h2>
          <p>{data.recommendation.reason}</p>
          {own && (
            <button className="button" onClick={() => startRecommended(data.recommendation!.scenario_id)}>
              Пройти сценарий
            </button>
          )}
        </section>
      )}

      {data.total_runs === 0 ? (
        <p className="muted">Пройдите первый сценарий — здесь появятся выводы о ваших сильных и слабых сторонах.</p>
      ) : (
        <>
          <section className="card">
            <h2>Выводы</h2>
            {data.strengths.length > 0 && <p>💪 Сильные стороны: {data.strengths.join(", ")}</p>}
            {data.weaknesses.length > 0 && <p>🎯 Стоит подтянуть: {data.weaknesses.join(", ")}</p>}
            {data.mistakes.length > 0 ? (
              <ul className="mistakes">
                {data.mistakes.map((mistake) => (
                  <li key={mistake}>{mistake}</li>
                ))}
              </ul>
            ) : (
              <p className="muted">Типичных ошибок не найдено.</p>
            )}
          </section>

          <section className="card">
            <h2>XP по неделям</h2>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={progress} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="week" stroke="var(--muted)" fontSize={12} />
                <YAxis stroke="var(--muted)" fontSize={12} />
                <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)" }} />
                <Bar dataKey="xp" name="XP" fill="var(--brand)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </section>

          <section className="card">
            <h2>Компетенции</h2>
            {data.competences.map((c) => (
              <div key={c.code} className="scale">
                <div className="scale__head">
                  <span>{c.title}</span>
                  <strong>{c.points}</strong>
                </div>
                <div className="scale__track">
                  <div className="scale__fill scale__fill--loyalty" style={{ width: `${(c.points / maxPoints) * 100}%` }} />
                </div>
              </div>
            ))}
          </section>

          <section className="card">
            <h2>По типам ситуаций</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Категория</th>
                    <th>Прохождений</th>
                    <th>Успех</th>
                    <th>Лучшие решения</th>
                    <th>Истёк таймер</th>
                    <th>Время решения</th>
                  </tr>
                </thead>
                <tbody>
                  {data.categories.map((c) => (
                    <tr key={c.category}>
                      <td>{c.title}</td>
                      <td>{c.runs}</td>
                      <td>{percent(c.success_rate)}</td>
                      <td>{percent(c.best_choice_rate)}</td>
                      <td>{percent(c.timeout_rate)}</td>
                      <td>{c.average_reaction_share === null ? "—" : `${percent(c.average_reaction_share)} таймера`}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
