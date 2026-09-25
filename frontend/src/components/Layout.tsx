import { Outlet, useLocation } from "react-router";
import { Header } from "./Header";

export function Layout() {
  const { pathname } = useLocation();
  return (
    <div className="shell">
      <Header />
      {/* key по адресу: при переходе страница заново проигрывает мягкое появление */}
      <main className="page" key={pathname}>
        <Outlet />
      </main>
      <footer className="footer">
        <span>ВСМ · Геймификация обучения проводников</span>
        <span className="muted">Демо-версия · все данные синтетические</span>
      </footer>
    </div>
  );
}
