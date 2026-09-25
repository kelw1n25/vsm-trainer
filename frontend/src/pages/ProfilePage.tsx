import { useEffect, useState } from "react";
import { Link } from "react-router";
import { PolarAngleAxis, PolarGrid, Radar, RadarChart, ResponsiveContainer } from "recharts";
import { api, ApiError } from "../api";
import { useCompetenceNames } from "../hooks";
import { categoryLabels, outcomeLabels } from "../labels";
import type { Level, Profile } from "../types";

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const names = useCompetenceNames();

  useEffect(() => {
    api
      .profile()
      .then(setProfile)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить профиль"));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!profile) return <p className="muted">Загрузка…</p>;

  const competences = Object.entries(profile.competence_points).map(([code, value]) => ({
    name: names[code] ?? code,
    value,
  }));

  return (
    <div className="run">
      <section className="card profile-head">
        <div>
          <h1>{profile.full_name}</h1>
          <p className="muted">
            Табельный № {profile.personnel_number} · {profile.brigade} · {profile.depot}
          </p>
        </div>
        <LevelProgress level={profile.level} />
      </section>

      <section className="card">
        <h2>Компетенции</h2>
        <ResponsiveContainer width="100%" height={300}>
          <RadarChart data={competences} outerRadius="70%">
            <PolarGrid stroke="var(--border)" />
            <PolarAngleAxis dataKey="name" tick={{ fill: "var(--muted)", fontSize: 12 }} />
            <Radar dataKey="value" stroke="var(--brand)" fill="var(--brand)" fillOpacity={0.3} />
          </RadarChart>
        </ResponsiveContainer>
        <ul className="competences">
          {competences.map((c) => (
            <li key={c.name}>
              {c.name}: <strong>{c.value}</strong>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>Достижения</h2>
        <div className="achievements">
          {profile.achievements.map((a) => (
            <div key={a.code} className={`achievement ${a.earned_at ? "" : "achievement--locked"}`}>
              <strong>{a.earned_at ? "🏅" : "🔒"} {a.title}</strong>
              <span className="muted">{a.description}</span>
              {a.earned_at && <span className="muted">{new Date(a.earned_at).toLocaleDateString("ru-RU")}</span>}
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>История прохождений</h2>
        {profile.history.length === 0 ? (
          <p className="muted">Пока нет завершённых сценариев.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Дата</th>
                  <th>Сценарий</th>
                  <th>Итог</th>
                  <th>Шкалы</th>
                  <th>XP</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {profile.history.map((item) => (
                  <tr key={item.run_id}>
                    <td>{new Date(item.finished_at).toLocaleString("ru-RU")}</td>
                    <td>
                      {item.scenario_title}
                      <span className="muted"> · {categoryLabels[item.category]}</span>
                    </td>
                    <td>{outcomeLabels[item.outcome]}</td>
                    <td>
                      {item.loyalty} / {item.safety}
                    </td>
                    <td>+{item.xp_earned}</td>
                    <td>
                      <Link to={`/runs/${item.run_id}/debrief`}>Разбор</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function LevelProgress({ level }: { level: Level }) {
  const span = level.next_level_xp === null ? 1 : level.next_level_xp - level.level_xp;
  const progress = level.next_level_xp === null ? 100 : ((level.xp - level.level_xp) / span) * 100;
  return (
    <div className="level">
      <strong>
        Уровень {level.level} · {level.title}
      </strong>
      <div className="timer__track">
        <div className="timer__fill" style={{ width: `${progress}%` }} />
      </div>
      <span className="muted">
        {level.xp} XP{level.next_level_xp !== null && ` из ${level.next_level_xp} до следующего уровня`}
      </span>
    </div>
  );
}
