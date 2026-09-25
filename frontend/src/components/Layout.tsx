import { NavLink, Outlet } from "react-router";
import { useAuth } from "../auth";

export function Layout() {
  const { session, logout } = useAuth();
  return (
    <>
      <header className="header">
        <div className="header__inner">
          <span className="header__brand">ВСМ-тренажёр</span>
          <nav className="header__nav">
            <NavLink to="/" end>
              Сценарии
            </NavLink>
            <NavLink to="/profile">Профиль</NavLink>
            <NavLink to="/leaderboard">Рейтинг</NavLink>
          </nav>
          <span className="header__user">{session?.fullName}</span>
          <button className="button button--ghost" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
