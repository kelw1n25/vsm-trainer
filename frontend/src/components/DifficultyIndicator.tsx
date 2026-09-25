const MAX_DIFFICULTY = 3;

export function DifficultyIndicator({ level }: { level: number }) {
  return (
    <p className="difficulty" aria-label={`Сложность ${level} из ${MAX_DIFFICULTY}`}>
      <span>Сложность:</span>
      {Array.from({ length: MAX_DIFFICULTY }, (_, index) => (
        <span key={index} className={`difficulty__dot ${index < level ? "difficulty__dot--on" : ""}`} />
      ))}
    </p>
  );
}
