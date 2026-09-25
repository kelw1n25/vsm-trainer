import { useState, type FormEvent } from "react";
import { Navigate } from "react-router";
import { ApiError } from "../api";
import { useAuth } from "../auth";
import { LogoMark } from "../components/icons";
import { HeroTrain } from "../components/illustrations";

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
      <section className="login__panel">
        <HeroTrain />
        <form className="card login__form" onSubmit={submit}>
          <div className="brand">
            <LogoMark />
            <span className="brand__name">ВСМ</span>
            <span className="brand__caption">
              Геймификация
              <br />
              обучения
            </span>
          </div>
          <h1 className="page-title">Вход для сотрудников</h1>
          <label>
            Табельный номер
            <input value={personnelNumber} onChange={(e) => setPersonnelNumber(e.target.value)} required autoFocus />
          </label>
          <label>
            Пароль
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="button button--large" disabled={sending}>
            {sending ? "Входим…" : "Войти"}
          </button>
          <p className="muted">
            Демо-доступ (данные синтетические): проводник <strong>100001</strong>, инструктор <strong>900001</strong>,
            пароль <strong>demo2026</strong>
          </p>
        </form>
      </section>
    </main>
  );
}
