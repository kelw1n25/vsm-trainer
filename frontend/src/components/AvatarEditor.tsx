import { useState } from "react";
import { ApiError } from "../api";
import { AVATAR_BACKGROUNDS, AVATAR_HEADWEAR, AVATAR_TIES, saveMyAvatar, useMyAvatar } from "../avatar";
import { useAuth } from "../auth";
import type { AvatarBackground, AvatarConfig, AvatarHeadwear, AvatarTie } from "../types";
import { Avatar } from "./Avatar";

/** Конструктор аватара: фон, головной убор, галстук. Выбор сразу сохраняется на сервере и виден везде. */
export function AvatarEditor() {
  const { session } = useAuth();
  const avatar = useMyAvatar();
  const [status, setStatus] = useState<string | null>(null);

  function choose(change: Partial<AvatarConfig>) {
    if (!session) return;
    const next = { ...avatar, ...change };
    setStatus(null);
    saveMyAvatar(session.employeeId, next, avatar)
      .then(() => setStatus("Сохранено"))
      .catch((error: unknown) => setStatus(error instanceof ApiError ? error.message : "Не удалось сохранить, повторите"));
  }

  return (
    <section className="card avatar-editor" id="avatar">
      <h2>Аватар</h2>
      <div className="avatar-editor__body">
        <Avatar size={112} avatar={avatar} />
        <div className="avatar-editor__options">
          <fieldset>
            <legend>Фон</legend>
            <div className="swatches">
              {(Object.keys(AVATAR_BACKGROUNDS) as AvatarBackground[]).map((code) => (
                <button
                  key={code}
                  type="button"
                  className="swatch"
                  aria-pressed={avatar.background === code}
                  aria-label={AVATAR_BACKGROUNDS[code].label}
                  title={AVATAR_BACKGROUNDS[code].label}
                  style={{ background: `linear-gradient(${AVATAR_BACKGROUNDS[code].top}, ${AVATAR_BACKGROUNDS[code].bottom})` }}
                  onClick={() => choose({ background: code })}
                />
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend>Головной убор</legend>
            <div className="chips">
              {(Object.keys(AVATAR_HEADWEAR) as AvatarHeadwear[]).map((code) => (
                <button key={code} type="button" className="chip" aria-selected={avatar.headwear === code} onClick={() => choose({ headwear: code })}>
                  {AVATAR_HEADWEAR[code]}
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend>Галстук</legend>
            <div className="swatches">
              {(Object.keys(AVATAR_TIES) as AvatarTie[]).map((code) => (
                <button
                  key={code}
                  type="button"
                  className="swatch"
                  aria-pressed={avatar.tie === code}
                  aria-label={AVATAR_TIES[code].label}
                  title={AVATAR_TIES[code].label}
                  style={{ background: AVATAR_TIES[code].color }}
                  onClick={() => choose({ tie: code })}
                />
              ))}
            </div>
          </fieldset>
        </div>
      </div>
      <p className="muted" role="status">
        {status ?? "Аватар собирается из деталей формы — фотографии не нужны и не хранятся."}
      </p>
    </section>
  );
}
