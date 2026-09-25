import type { Level } from "../types";

/** Уровень и прогресс XP до следующего. */
export function LevelProgress({ level }: { level: Level }) {
  const span = level.next_level_xp === null ? 1 : level.next_level_xp - level.level_xp;
  const progress = level.next_level_xp === null ? 100 : ((level.xp - level.level_xp) / span) * 100;
  return (
    <div className="level">
      <strong>
        Уровень {level.level} · {level.title}
      </strong>
      <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
        <div className="progress__fill" style={{ width: `${progress}%` }} />
      </div>
      <span className="muted">
        {level.xp} XP
        {level.next_level_xp !== null ? ` · до следующего уровня ${level.next_level_xp - level.xp} XP` : " · максимальный уровень"}
      </span>
    </div>
  );
}
