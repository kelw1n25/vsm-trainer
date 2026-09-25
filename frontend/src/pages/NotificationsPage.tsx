import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { NotificationList } from "../types";

export function NotificationsPage() {
  const [data, setData] = useState<NotificationList | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .notifications()
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить уведомления"));
  }, []);

  useEffect(load, [load]);

  async function readAll() {
    await api.readAllNotifications().catch(() => {});
    load();
  }

  return (
    <div className="stack narrow">
      <div className="section__head">
        <h1 className="page-title">Уведомления</h1>
        {data && data.unread > 0 && (
          <button className="button button--ghost" onClick={readAll}>
            Отметить все прочитанными
          </button>
        )}
      </div>
      {error && <p className="error">{error}</p>}
      {data?.items.length === 0 && <p className="muted">Уведомлений пока нет.</p>}
      {data?.items.map((item) => (
        <article key={item.id} className={`card notification ${item.read ? "" : "notification--new"}`}>
          <strong>{item.title}</strong>
          <span>{item.body}</span>
          <span className="muted">{new Date(item.created_at).toLocaleString("ru-RU")}</span>
        </article>
      ))}
    </div>
  );
}
