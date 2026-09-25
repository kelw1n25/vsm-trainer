import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { api } from "../api";
import { useAuth } from "../auth";
import { roleLabels } from "../labels";
import { Avatar } from "./Avatar";
import { ChevronDownIcon } from "./icons";

const POLL_MS = 30_000;

export function UserDropdown() {
  const { session, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const ref = useRef<HTMLDivElement>(null);

  // Ошибку опроса не показываем: счётчик обновится при следующей попытке
  const loadUnread = useCallback(() => {
    api
      .notifications()
      .then((data) => setUnread(data.unread))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadUnread();
    const id = setInterval(loadUnread, POLL_MS);
    return () => clearInterval(id);
  }, [loadUnread]);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (event instanceof KeyboardEvent ? event.key === "Escape" : !ref.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open]);

  const close = () => setOpen(false);

  return (
    <div className="user" ref={ref}>
      <button
        className="user__toggle"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Меню пользователя"
        onClick={() => setOpen(!open)}
      >
        <span className="user__avatar">
          <Avatar size={64} />
          <span className="user__online" />
          {unread > 0 && <span className="user__badge">{unread}</span>}
        </span>
        <span className={`user__chevron ${open ? "user__chevron--open" : ""}`}>
          <ChevronDownIcon />
        </span>
      </button>
      {open && (
        <div className="menu" role="menu">
          <div className="menu__head">
            <strong>{session?.fullName}</strong>
            <span className="muted">{session && roleLabels[session.role]}</span>
          </div>
          <Link role="menuitem" to="/profile" onClick={close}>
            Профиль
          </Link>
          <Link role="menuitem" to="/notifications" onClick={close}>
            Уведомления
            {unread > 0 && <span className="count-badge">{unread}</span>}
          </Link>
          <Link role="menuitem" to="/settings" onClick={close}>
            Настройки
          </Link>
          <button role="menuitem" className="menu__logout" onClick={logout}>
            Выйти
          </button>
        </div>
      )}
    </div>
  );
}
