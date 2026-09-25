import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, ApiError } from "../api";
import { ArrowRightIcon } from "../components/icons";
import { useStartScenario } from "../hooks";
import { outcomeLabels } from "../labels";
import type { Analytics, HistoryItem } from "../types";

function percent(value: number | null): string {
  return value === null ? "—" : `${Math.round(value * 100)}%`;
}

/** Взвешенная по числу прохождений / решений доля по всем категориям. */
function overall(data: Analytics, pick: (c: Analytics["categories"][number]) => [number | null, number]): number | null {
  const [sum, weight] = data.categories.reduce(
    ([s, w], c) => {
      const [rate, count] = pick(c);
      return rate === null ? [s, w] : [s + rate * count, w + count];
    },
    [0, 0],
  );
  return weight ? sum / weight : null;
}

/** Аналитика своя (/analytics) или проводника для инструктора (/team/:employeeId). */
export function AnalyticsPage() {
  const { employeeId } = useParams();
  const own = employeeId === undefined;
  const [data, setData] = useState<Analytics | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const { start, error: startError } = useStartScenario();

  useEffect(() => {
    setData(null);
    (own ? api.myAnalytics() : api.employeeAnalytics(employeeId))
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить аналитику"));
    // История доступна только по себе: профиль другого сотрудника инструктору не нужен
    if (own) api.profile().then((profile) => setHistory(profile.history)).catch(() => {});
  }, [own, employeeId]);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Загрузка…</p>;

  const progress = data.progress.map((week) => ({
    week: new Date(week.week_start).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" }),
    xp: week.xp,
  }));
  const maxPoints = Math.max(1, ...data.competences.map((c) => c.points));
  const weeksXp = data.progress.reduce((sum, week) => sum + week.xp, 0);

  return (
    <div className="stack">
      <h1 className="page-title">{own ? "Аналитика" : `Аналитика: ${data.full_name}`}</h1>

      <section className="kpis">
        <div className="card kpi">
          <span className="kpi__label">Пройдено сценариев</span>
          <strong className="kpi__value">{data.total_runs}</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Средний результат</span>
          <strong className="kpi__value">{percent(overall(data, (c) => [c.success_rate, c.runs]))}</strong>
          <span className="muted">успешных прохождений</span>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Набрано баллов</span>
          <strong className="kpi__value">{weeksXp} XP</strong>
          <span className="muted">за 8 недель</span>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Лучшие решения</span>
          <strong className="kpi__value">{percent(overall(data, (c) => [c.best_choice_rate, c.decisions]))}</strong>
          <span className="muted">от всех решений</span>
        </div>
      </section>

      {data.recommendation && (
        <section className="card recommendation">
          <div>
            <p className="hero__eyebrow">Рекомендация</p>
            <h2>{data.recommendation.title}</h2>
            <p className="muted">{data.recommendation.reason}</p>
            {startError && <p className="error">{startError}</p>}
          </div>
          {own && (
            <button className="button" onClick={() => start(data.recommendation!.scenario_id)}>
              Пройти <ArrowRightIcon />
            </button>
          )}
        </section>
      )}

      {data.total_runs === 0 ? (
        <p className="muted">Пройдите первый сценарий — здесь появятся выводы о сильных и слабых сторонах.</p>
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

          <div className="two-columns">
            <section className="card">
              <h2>Прогресс: XP по неделям</h2>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={progress} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
                  <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="week" stroke="var(--muted)" fontSize={12} />
                  <YAxis stroke="var(--muted)" fontSize={12} />
                  <Tooltip contentStyle={{ borderRadius: 12, border: "1px solid var(--border)" }} />
                  <Bar dataKey="xp" name="XP" fill="var(--brand)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </section>

            <section className="card">
              <h2>Навыки</h2>
              {data.competences.map((c) => (
                <div key={c.code} className="skill">
                  <div className="skill__head">
                    <span>{c.title}</span>
                    <strong>{c.points}</strong>
                  </div>
                  <div className="progress">
                    <div className="progress__fill" style={{ width: `${(c.points / maxPoints) * 100}%` }} />
                  </div>
                </div>
              ))}
            </section>
          </div>

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

          {own && history.length > 0 && (
            <section className="card">
              <h2>История прохождения</h2>
              <ul className="history">
                {history.map((item) => (
                  <li key={item.run_id}>
                    <span>{new Date(item.finished_at).toLocaleString("ru-RU")}</span>
                    <span>{item.scenario_title}</span>
                    <span className={`outcome outcome--${item.outcome}`}>{outcomeLabels[item.outcome]}</span>
                    <span>+{item.xp_earned} XP</span>
                    <Link to={`/runs/${item.run_id}/debrief`}>Разбор</Link>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}
