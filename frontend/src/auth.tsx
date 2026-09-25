import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, loadSession, onUnauthorized, saveSession } from "./api";
import type { Session } from "./types";

interface AuthContextValue {
  session: Session | null;
  login: (personnelNumber: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession);

  const logout = useCallback(() => {
    saveSession(null);
    setSession(null);
  }, []);

  useEffect(() => onUnauthorized(logout), [logout]);

  const login = useCallback(async (personnelNumber: string, password: string) => {
    const response = await api.login(personnelNumber, password);
    const next: Session = {
      token: response.access_token,
      employeeId: response.employee_id,
      fullName: response.full_name,
      role: response.role,
    };
    saveSession(next);
    setSession(next);
  }, []);

  return <AuthContext.Provider value={{ session, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth вне AuthProvider");
  return value;
}
