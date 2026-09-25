const URGENT_SECONDS = 5;

interface Props {
  remainingMs: number;
  totalSeconds: number;
}

export function Timer({ remainingMs, totalSeconds }: Props) {
  const seconds = Math.ceil(remainingMs / 1000);
  const urgent = seconds < URGENT_SECONDS;
  return (
    <div className={`timer ${urgent ? "timer--urgent" : ""}`} aria-live="polite">
      <div className="timer__value">{seconds > 0 ? `${seconds} с` : "Время вышло"}</div>
      <div className="timer__track">
        <div className="timer__fill" style={{ width: `${(remainingMs / (totalSeconds * 1000)) * 100}%` }} />
      </div>
    </div>
  );
}
