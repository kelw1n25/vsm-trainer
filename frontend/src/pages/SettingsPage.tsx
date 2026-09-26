import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { api } from "../api";
import { AvatarEditor } from "../components/AvatarEditor";
import { useAuth } from "../auth";
import { roleLabels } from "../labels";
import { applyReduceMotion, loadReduceMotion, saveTheme, useTheme } from "../preferences";
import type { Profile } from "../types";

export function SettingsPage() {
  const { logout } = useAuth();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [reduceMotion, setReduceMotion] = useState(loadReduceMotion);
  const theme = useTheme();

  const { hash } = useLocation();

  useEffect(() => {
    api.profile().then(setProfile).catch(() => {});
  }, []);

  // «Изменить аватар» из профиля ведёт на /settings#avatar — сразу к конструктору
  useEffect(() => {
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView({ block: "start" });
  }, [hash]);

  function toggleMotion(enabled: boolean) {
    setReduceMotion(enabled);
    applyReduceMotion(enabled);
  }

  return (
    <div className="stack narrow">
      <h1 className="page-title">Настройки</h1>

      <section className="card">
        <h2>Учётная запись</h2>
        <dl className="details">
          <dt>Сотрудник</dt>
          <dd>{profile?.full_name ?? "—"}</dd>
          <dt>Табельный номер</dt>
          <dd>{profile?.personnel_number ?? "—"}</dd>
          <dt>Роль</dt>
          <dd>{profile ? roleLabels[profile.role] : "—"}</dd>
          <dt>Бригада и депо</dt>
          <dd>{profile ? `${profile.brigade} · ${profile.depot}` : "—"}</dd>
        </dl>
        <p className="muted">Данные учётной записи ведутся в HR-системе и обновляются через интеграцию.</p>
      </section>

      <AvatarEditor />

      <section className="card">
        <h2>Интерфейс</h2>
        <label className="switch">
          <input type="checkbox" checked={theme === "dark"} onChange={(e) => saveTheme(e.target.checked ? "dark" : "light")} />
          <span className="switch__track" />
          <span>
            <strong>Тёмная тема</strong>
            <span className="muted"> — то же, что ползунок в шапке</span>
          </span>
        </label>
        <label className="switch">
          <input type="checkbox" checked={reduceMotion} onChange={(e) => toggleMotion(e.target.checked)} />
          <span className="switch__track" />
          <span>
            <strong>Уменьшить анимацию</strong>
            <span className="muted"> — отключает переходы и эффекты при наведении</span>
          </span>
        </label>
      </section>

      <button className="button button--ghost" onClick={logout}>
        Выйти из системы
      </button>
    </div>
  );
}
