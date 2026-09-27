import { useState, type ChangeEvent } from "react";
import { ApiError } from "../api";
import {
  AVATAR_BACKGROUNDS,
  AVATAR_HEADWEAR,
  AVATAR_TIES,
  removeMyPhoto,
  saveMyAvatar,
  uploadMyPhoto,
  useMyAvatar,
  useMyPhoto,
} from "../avatar";
import { useAuth } from "../auth";
import type { AvatarBackground, AvatarConfig, AvatarHeadwear, AvatarTie } from "../types";
import { Avatar } from "./Avatar";

const PHOTO_TYPES = ["image/jpeg", "image/png", "image/webp"];
const PHOTO_MAX_BYTES = 8 * 1024 * 1024;

function failure(error: unknown): string {
  return error instanceof ApiError ? error.message : "Не удалось сохранить, повторите";
}

/**
 * Аватар: своё фото (с согласием на обработку) или конструктор — фон, головной убор, галстук.
 * Выбор сразу сохраняется на сервере и виден везде.
 */
export function AvatarEditor() {
  const { session } = useAuth();
  const avatar = useMyAvatar();
  const photo = useMyPhoto();
  const [status, setStatus] = useState<string | null>(null);
  const [consent, setConsent] = useState(false);
  const [uploading, setUploading] = useState(false);

  function pickPhoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!session || !file) return;
    if (!PHOTO_TYPES.includes(file.type)) return setStatus("Нужна фотография в формате JPEG, PNG или WebP");
    if (file.size > PHOTO_MAX_BYTES) return setStatus("Фотография больше 8 МБ — выберите снимок поменьше");
    setStatus(null);
    setUploading(true);
    uploadMyPhoto(session.employeeId, file)
      .then(() => setStatus("Фото сохранено"))
      .catch((error: unknown) => setStatus(failure(error)))
      .finally(() => setUploading(false));
  }

  function removePhoto() {
    if (!session) return;
    setStatus(null);
    removeMyPhoto(session.employeeId)
      .then(() => {
        setConsent(false);
        setStatus("Фото удалено с сервера");
      })
      .catch((error: unknown) => setStatus(failure(error)));
  }

  function choose(change: Partial<AvatarConfig>) {
    if (!session) return;
    const next = { ...avatar, ...change };
    setStatus(null);
    saveMyAvatar(session.employeeId, next, avatar)
      .then(() => setStatus("Сохранено"))
      .catch((error: unknown) => setStatus(failure(error)));
  }

  return (
    <section className="card avatar-editor" id="avatar">
      <h2>Аватар</h2>
      <div className="avatar-editor__body">
        <Avatar size={112} avatar={avatar} photo={photo} />
        <div className="avatar-editor__options">
          <fieldset>
            <legend>Своё фото</legend>
            {photo ? (
              <div className="avatar-editor__photo">
                <p className="muted">На аватаре — ваше фото. Его видите только вы: коллеги в рейтинге видят инициалы.</p>
                <button type="button" className="button button--ghost" onClick={removePhoto}>
                  Убрать фото
                </button>
              </div>
            ) : (
              <div className="avatar-editor__photo">
                <label className="consent">
                  <input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} />
                  <span>
                    Согласен(на) на обработку моей фотографии для аватара. Фото видно только мне, его можно удалить в
                    любой момент.
                  </span>
                </label>
                <label className={`button button--ghost${consent && !uploading ? "" : " is-disabled"}`}>
                  {uploading ? "Загружаем…" : "Загрузить фото"}
                  <input
                    type="file"
                    accept={PHOTO_TYPES.join(",")}
                    className="visually-hidden"
                    disabled={!consent || uploading}
                    onChange={pickPhoto}
                  />
                </label>
              </div>
            )}
          </fieldset>
          <fieldset disabled={photo !== null}>
            <legend>Фон{photo && " — для аватара без фото"}</legend>
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
          <fieldset disabled={photo !== null}>
            <legend>Головной убор</legend>
            <div className="chips">
              {(Object.keys(AVATAR_HEADWEAR) as AvatarHeadwear[]).map((code) => (
                <button key={code} type="button" className="chip" aria-selected={avatar.headwear === code} onClick={() => choose({ headwear: code })}>
                  {AVATAR_HEADWEAR[code]}
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset disabled={photo !== null}>
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
        {status ?? "Без своего фото аватар собирается из деталей формы."}
      </p>
    </section>
  );
}
