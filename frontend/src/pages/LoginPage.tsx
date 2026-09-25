import { useState, type FormEvent } from "react";
import { Navigate } from "react-router";
import { ApiError } from "../api";
import { useAuth } from "../auth";

export function LoginPage() {
  const { session, login } = useAuth();
  const [personnelNumber, setPersonnelNumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  if (session) return <Navigate to="/" replace />;

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await login(personnelNumber.trim(), password);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось войти");
    } finally {
      setSending(false);
    }
  }

  return (
    <main className="login">
      <form className="card login__form" onSubmit={submit}>
        <h1>ВСМ-тренажёр проводника</h1>
        <p className="muted">Нештатные ситуации на скорости 400 км/ч</p>
        <label>
          Табельный номер
          <input value={personnelNumber} onChange={(e) => setPersonnelNumber(e.target.value)} required autoFocus />
        </label>
        <label>
          Пароль
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="button" disabled={sending}>
          {sending ? "Входим…" : "Войти"}
        </button>
      </form>
    </main>
  );
}
