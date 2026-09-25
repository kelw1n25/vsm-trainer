import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export interface ScalePoint {
  label: string;
  loyalty: number;
  safety: number;
}

/** Траектория обеих шкал по шагам сценария. */
export function ScaleChart({ points }: { points: ScalePoint[] }) {
  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={points} margin={{ top: 8, right: 16, bottom: 0, left: -16 }}>
        <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
        <XAxis dataKey="label" stroke="var(--muted)" fontSize={12} />
        <YAxis domain={[0, 100]} stroke="var(--muted)" fontSize={12} />
        <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)" }} />
        <Legend />
        <Line type="monotone" dataKey="loyalty" name="Лояльность" stroke="var(--loyalty)" strokeWidth={2} />
        <Line type="monotone" dataKey="safety" name="Безопасность" stroke="var(--safety)" strokeWidth={2} />
      </LineChart>
    </ResponsiveContainer>
  );
}
