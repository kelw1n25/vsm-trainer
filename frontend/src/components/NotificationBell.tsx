import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import type { NotificationList } from "../types";

const POLL_MS = 30_000;

export function NotificationBell() {
  const [data, setData] = useState<NotificationList | null>(null);
  const [open, setOpen] = useState(false);

  // Ошибку опроса не показываем: колокольчик просто обновится при следующей попытке
  const load = useCallback(() => {
    api.notifications().then(setData).catch(() => {});
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MS);
    return () => clearInterval(id);
  }, [load]);

  async function toggle() {
    const opening = !open;
    setOpen(opening);
    if (opening && data?.unread) {
      await api.readAllNotifications().catch(() => {});
    }
    if (!opening) load();
  }

  return (
    <div className="bell">
      <button className="button button--ghost" onClick={toggle} aria-expanded={open}>
        🔔{data?.unread ? <span className="bell__count">{data.unread}</span> : null}
      </button>
      {open && (
        <div className="bell__panel card">
          {data?.items.length ? (
            data.items.map((item) => (
              <div key={item.id} className={`bell__item ${item.read ? "" : "bell__item--new"}`}>
                <strong>{item.title}</strong>
                <span>{item.body}</span>
                <span className="muted">{new Date(item.created_at).toLocaleString("ru-RU")}</span>
              </div>
            ))
          ) : (
            <p className="muted">Уведомлений пока нет</p>
          )}
        </div>
      )}
    </div>
  );
}
