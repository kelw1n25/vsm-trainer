import { useEffect, useState } from "react";
import { Link } from "react-router";
import { api, ApiError } from "../api";
import type { TeamMember } from "../types";

/** Для инструктора: проводники его депо и переход к аналитике каждого. */
export function TeamPage() {
  const [team, setTeam] = useState<TeamMember[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .team()
      .then(setTeam)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить список"));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!team) return <p className="muted">Загрузка…</p>;

  return (
    <div className="run">
      <h1>Проводники депо</h1>
      <section className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Проводник</th>
              <th>Бригада</th>
              <th>Уровень</th>
              <th>XP</th>
              <th>Последняя активность</th>
              <th>Проседает</th>
            </tr>
          </thead>
          <tbody>
            {team.map((member) => (
              <tr key={member.employee_id}>
                <td>
                  <Link to={`/team/${member.employee_id}`}>{member.full_name}</Link>
                </td>
                <td>{member.brigade}</td>
                <td>{member.level_title}</td>
                <td>{member.xp}</td>
                <td>{member.last_activity_at ? new Date(member.last_activity_at).toLocaleDateString("ru-RU") : "—"}</td>
                <td>{member.weakest_competence ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
