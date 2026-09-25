import type {
  Analytics,
  Debrief,
  Leaderboard,
  LeaderboardPeriod,
  LeaderboardScope,
  LoginResponse,
  Meta,
  NotificationList,
  Profile,
  RunState,
  ScenarioSummary,
  Session,
  TeamMember,
} from "./types";

const SESSION_KEY = "vsm_session";

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

// localStorage может быть недоступен (приватный режим) — тогда просто живём без сохранения сессии
export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function saveSession(session: Session | null): void {
  try {
    if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    else localStorage.removeItem(SESSION_KEY);
  } catch {
    // сессия останется только в памяти вкладки
  }
}

let unauthorizedHandler: () => void = () => {};

export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = loadSession()?.token;
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "network", "Нет связи с сервером. Проверьте подключение.");
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && token) unauthorizedHandler();
    const detail = data?.detail;
    throw new ApiError(response.status, detail?.code ?? "error", detail?.message ?? "Ошибка сервера");
  }
  return data as T;
}

export const api = {
  login: (personnelNumber: string, password: string) =>
    request<LoginResponse>("POST", "/api/auth/login", { personnel_number: personnelNumber, password }),
  meta: () => request<Meta>("GET", "/api/meta"),
  scenarios: () => request<ScenarioSummary[]>("GET", "/api/scenarios"),
  startRun: (scenarioId: string) => request<RunState>("POST", "/api/runs", { scenario_id: scenarioId }),
  getRun: (runId: string) => request<RunState>("GET", `/api/runs/${runId}`),
  choose: (runId: string, nodeId: string, choiceId: string) =>
    request<RunState>("POST", `/api/runs/${runId}/choices`, { node_id: nodeId, choice_id: choiceId }),
  debrief: (runId: string) => request<Debrief>("GET", `/api/runs/${runId}/debrief`),
  profile: () => request<Profile>("GET", "/api/profile"),
  leaderboard: (scope: LeaderboardScope, period: LeaderboardPeriod) =>
    request<Leaderboard>("GET", `/api/leaderboard?scope=${scope}&period=${period}`),
  notifications: () => request<NotificationList>("GET", "/api/notifications"),
  readAllNotifications: () => request<{ status: string }>("POST", "/api/notifications/read-all"),
  myAnalytics: () => request<Analytics>("GET", "/api/analytics/me"),
  employeeAnalytics: (employeeId: string) => request<Analytics>("GET", `/api/analytics/employees/${employeeId}`),
  team: () => request<TeamMember[]>("GET", "/api/analytics/team"),
};
