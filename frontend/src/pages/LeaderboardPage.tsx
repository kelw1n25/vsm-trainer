import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { Leaderboard, LeaderboardPeriod, LeaderboardScope } from "../types";

const scopes: [LeaderboardScope, string][] = [
  ["brigade", "Бригада"],
  ["depot", "Депо"],
  ["company", "Компания"],
];

const periods: [LeaderboardPeriod, string][] = [
  ["week", "Неделя"],
  ["month", "Месяц"],
  ["all", "Всё время"],
];

export function LeaderboardPage() {
  const [scope, setScope] = useState<LeaderboardScope>("brigade");
  const [period, setPeriod] = useState<LeaderboardPeriod>("week");
  const [board, setBoard] = useState<Leaderboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .leaderboard(scope, period)
      .then(setBoard)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить рейтинг"));
  }, [scope, period]);

  return (
    <div className="run">
      <h1>Рейтинг</h1>
      <div className="tabs-row">
        <Tabs options={scopes} value={scope} onChange={setScope} />
        <Tabs options={periods} value={period} onChange={setPeriod} />
      </div>
      {error && <p className="error">{error}</p>}
      {board && (
        <section className="card">
          <h2>
            {board.title} <span className="muted">· участников: {board.participants}</span>
          </h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Место</th>
                  <th>Проводник</th>
                  <th>Бригада / депо</th>
                  <th>Уровень</th>
                  <th>XP</th>
                </tr>
              </thead>
              <tbody>
                {board.rows.map((row) => (
                  <tr key={row.employee_id} className={row.is_me ? "me" : ""}>
                    <td>{row.rank}</td>
                    <td>
                      {row.full_name}
                      {row.is_me && " (вы)"}
                    </td>
                    <td>
                      {row.brigade} · {row.depot}
                    </td>
                    <td>{row.level_title}</td>
                    <td>{row.points}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}

function Tabs<T extends string>({
  options,
  value,
  onChange,
}: {
  options: [T, string][];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <div className="tabs" role="tablist">
      {options.map(([key, label]) => (
        <button
          key={key}
          role="tab"
          aria-selected={key === value}
          className={`tab ${key === value ? "tab--active" : ""}`}
          onClick={() => onChange(key)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
