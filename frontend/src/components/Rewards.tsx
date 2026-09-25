import type { Achievement, Level } from "../types";

/** Ачивки и повышение уровня, полученные только что. */
export function Rewards({ achievements, levelUp }: { achievements: Achievement[]; levelUp: Level | null }) {
  if (achievements.length === 0 && !levelUp) return null;
  return (
    <section className="rewards">
      {levelUp && (
        <div className="reward reward--level">
          <strong>Новый уровень {levelUp.level}: {levelUp.title}</strong>
        </div>
      )}
      {achievements.map((achievement) => (
        <div key={achievement.code} className="reward">
          <strong>🏅 {achievement.title}</strong>
          <span className="muted">{achievement.description}</span>
        </div>
      ))}
    </section>
  );
}
