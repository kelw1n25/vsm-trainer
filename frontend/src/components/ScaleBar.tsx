interface Props {
  label: string;
  value: number;
  kind: "loyalty" | "safety";
}

export function ScaleBar({ label, value, kind }: Props) {
  const level = value < 30 ? "low" : value < 60 ? "mid" : "high";
  return (
    <div className="scale">
      <div className="scale__head">
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
      <div className="scale__track" role="meter" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={value}>
        <div className={`scale__fill scale__fill--${kind} scale__fill--${level}`} style={{ width: `${value}%` }} />
      </div>
    </div>
  );
}
