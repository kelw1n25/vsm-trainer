import { useCompetenceNames } from "../hooks";
import { signed } from "../labels";

/** Изменения очков по компетенциям; нулевые не показываем. */
export function CompetenceList({ points, inline = false }: { points: Record<string, number>; inline?: boolean }) {
  const names = useCompetenceNames();
  const entries = Object.entries(points).filter(([, value]) => value !== 0);
  if (entries.length === 0) return null;
  return (
    <ul className={`competences ${inline ? "competences--inline" : ""}`}>
      {entries.map(([code, value]) => (
        <li key={code} className={value < 0 ? "negative" : "positive"}>
          {names[code] ?? code}: {signed(value)}
        </li>
      ))}
    </ul>
  );
}
