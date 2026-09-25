import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import { Avatar, InitialsAvatar } from "../components/Avatar";
import { LevelProgress } from "../components/LevelProgress";
import type { Leaderboard, LeaderboardPeriod, LeaderboardScope, Profile } from "../types";

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

export function RatingPage() {
  const [scope, setScope] = useState<LeaderboardScope>("brigade");
  const [period, setPeriod] = useState<LeaderboardPeriod>("week");
  const [board, setBoard] = useState<Leaderboard | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.profile().then(setProfile).catch(() => {});
  }, []);

  useEffect(() => {
    api
      .leaderboard(scope, period)
      .then(setBoard)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить рейтинг"));
  }, [scope, period]);

  const me = board?.rows.find((row) => row.is_me);

  return (
    <div className="stack">
      <h1 className="page-title">Рейтинг</h1>

      {me && (
        <section className="card my-place">
          <Avatar size={88} />
          <div className="my-place__info">
            <span className="muted">Ваше место · {board?.title}</span>
            <strong className="my-place__rank">
              {me.rank}
              <span className="muted"> из {board?.participants}</span>
            </strong>
            <span>{me.full_name}</span>
          </div>
          <div className="my-place__points">
            <span className="muted">Баллы за период</span>
            <strong className="kpi__value">{me.points} XP</strong>
          </div>
          {profile && <LevelProgress level={profile.level} />}
        </section>
      )}

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
          <ol className="rating">
            {board.rows.map((row) => (
              <li key={row.employee_id} className={`rating__row ${row.is_me ? "rating__row--me" : ""}`}>
                <span className={`rank ${row.rank <= 3 ? `rank--top${row.rank}` : ""}`}>{row.rank}</span>
                {row.is_me ? <Avatar size={44} /> : <InitialsAvatar name={row.full_name} size={44} />}
                <span className="rating__name">
                  <strong>
                    {row.full_name}
                    {row.is_me && " (вы)"}
                  </strong>
                  <span className="muted">
                    {row.brigade} · {row.depot} · {row.level_title}
                  </span>
                </span>
                <strong className="rating__points">{row.points} XP</strong>
              </li>
            ))}
          </ol>
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
