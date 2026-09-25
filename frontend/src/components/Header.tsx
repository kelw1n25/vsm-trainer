import { Link, NavLink } from "react-router";
import { useAuth } from "../auth";
import { LogoMark } from "./icons";
import { UserDropdown } from "./UserDropdown";

export function Header() {
  const { session } = useAuth();
  return (
    <header className="header">
      <Link to="/" className="brand" aria-label="ВСМ — на главную">
        <LogoMark />
        <span className="brand__name">ВСМ</span>
        <span className="brand__caption">
          Геймификация
          <br />
          обучения
        </span>
      </Link>
      <nav className="nav" aria-label="Основные разделы">
        <NavLink to="/scenarios">Сценарии</NavLink>
        <NavLink to="/profile">Профиль</NavLink>
        <NavLink to="/rating">Рейтинг</NavLink>
        <NavLink to="/analytics">Аналитика</NavLink>
        {session?.role === "instructor" && <NavLink to="/team">Команда</NavLink>}
      </nav>
      <UserDropdown />
    </header>
  );
}
