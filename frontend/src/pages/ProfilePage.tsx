import { useEffect, useState } from "react";
import { Link } from "react-router";
import { PolarAngleAxis, PolarGrid, Radar, RadarChart, ResponsiveContainer } from "recharts";
import { api, ApiError } from "../api";
import { Avatar } from "../components/Avatar";
import { LevelProgress } from "../components/LevelProgress";
import { useCompetenceNames } from "../hooks";
import { categoryLabels, outcomeLabels, roleLabels } from "../labels";
import type { Profile } from "../types";

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
  const earned = profile.achievements.filter((a) => a.earned_at).length;

  return (
    <div className="stack">
      <section className="card profile-head">
        <Avatar size={112} />
        <div className="profile-head__info">
          <h1 className="page-title">{profile.full_name}</h1>
          <p className="muted">
            {roleLabels[profile.role]} · {profile.brigade} · {profile.depot}
          </p>
          <p className="muted">Табельный № {profile.personnel_number}</p>
        </div>
        <LevelProgress level={profile.level} />
      </section>

      <section className="kpis">
        <div className="card kpi">
          <span className="kpi__label">Общий балл</span>
          <strong className="kpi__value">{profile.level.xp} XP</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Пройдено сценариев</span>
          <strong className="kpi__value">{profile.runs_completed}</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Достижения</span>
          <strong className="kpi__value">
            {earned} из {profile.achievements.length}
          </strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Уровень</span>
          <strong className="kpi__value">
            {profile.level.level} · {profile.level.title}
          </strong>
        </div>
      </section>

      <div className="two-columns">
        <section className="card">
          <h2>Компетенции</h2>
          <ResponsiveContainer width="100%" height={300}>
            <RadarChart data={competences} outerRadius="68%">
              <PolarGrid stroke="var(--border)" />
              <PolarAngleAxis dataKey="name" tick={{ fill: "var(--muted)", fontSize: 12 }} />
              <Radar dataKey="value" stroke="var(--brand)" fill="var(--brand)" fillOpacity={0.25} />
            </RadarChart>
          </ResponsiveContainer>
        </section>

        <section className="card">
          <h2>Достижения</h2>
          <div className="achievements">
            {profile.achievements.map((a) => (
              <div key={a.code} className={`achievement ${a.earned_at ? "" : "achievement--locked"}`}>
                <strong>
                  {a.earned_at ? "🏅" : "🔒"} {a.title}
                </strong>
                <span className="muted">{a.description}</span>
                {a.earned_at && <span className="muted">{new Date(a.earned_at).toLocaleDateString("ru-RU")}</span>}
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="card">
        <h2>История прохождений</h2>
        {profile.history.length === 0 ? (
          <p className="muted">
            Пока нет завершённых сценариев. <Link to="/scenarios">Выбрать сценарий</Link>
          </p>
        ) : (
          <ul className="history">
            {profile.history.map((item) => (
              <li key={item.run_id}>
                <span>{new Date(item.finished_at).toLocaleString("ru-RU")}</span>
                <span>
                  {item.scenario_title}
                  <span className="muted"> · {categoryLabels[item.category]}</span>
                </span>
                <span className={`outcome outcome--${item.outcome}`}>{outcomeLabels[item.outcome]}</span>
                <span>+{item.xp_earned} XP</span>
                <Link to={`/runs/${item.run_id}/debrief`}>Разбор</Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
