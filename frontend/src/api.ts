import type {
  Analytics,
  AvatarConfig,
  Debrief,
  Handbook,
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
  StoryMap,
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

// Запрос с разбором ошибки сервера; тело — JSON или файл (фото аватара)
async function call(method: string, path: string, body?: BodyInit, contentType?: string): Promise<Response> {
  const token = loadSession()?.token;
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (contentType) headers["Content-Type"] = contentType;

  let response: Response;
  try {
    response = await fetch(path, { method, headers, body });
  } catch {
    throw new ApiError(0, "network", "Нет связи с сервером. Проверьте подключение.");
  }

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    if (response.status === 401 && token) unauthorizedHandler();
    const detail = data?.detail;
    throw new ApiError(response.status, detail?.code ?? "error", detail?.message ?? "Ошибка сервера");
  }
  return response;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await (body === undefined
    ? call(method, path)
    : call(method, path, JSON.stringify(body), "application/json"));
  return (await response.json().catch(() => null)) as T;
}

export const api = {
  login: (personnelNumber: string, password: string) =>
    request<LoginResponse>("POST", "/api/auth/login", { personnel_number: personnelNumber, password }),
  meta: () => request<Meta>("GET", "/api/meta"),
  scenarios: () => request<ScenarioSummary[]>("GET", "/api/scenarios"),
  startRun: (scenarioId: string) => request<RunState>("POST", "/api/runs", { scenario_id: scenarioId }),
  getRun: (runId: string) => request<RunState>("GET", `/api/runs/${runId}`),
  reveal: (runId: string, nodeId: string) =>
    request<RunState>("POST", `/api/runs/${runId}/reveal`, { node_id: nodeId }),
  choose: (runId: string, nodeId: string, choiceId: string) =>
    request<RunState>("POST", `/api/runs/${runId}/choices`, { node_id: nodeId, choice_id: choiceId }),
  debrief: (runId: string) => request<Debrief>("GET", `/api/runs/${runId}/debrief`),
  storyMap: (scenarioId: string) => request<StoryMap>("GET", `/api/scenarios/${scenarioId}/story-map`),
  handbook: () => request<Handbook>("GET", "/api/handbook"),
  profile: () => request<Profile>("GET", "/api/profile"),
  updateAvatar: (avatar: AvatarConfig) => request<AvatarConfig>("PUT", "/api/profile/avatar", avatar),
  // Своё фото: загрузка только с согласием сотрудника; сервер пересобирает снимок без метаданных
  uploadPhoto: (photo: Blob) =>
    call("PUT", "/api/profile/avatar/photo?consent=true", photo, photo.type).then(
      (response) => response.json() as Promise<{ version: number }>,
    ),
  deletePhoto: () => call("DELETE", "/api/profile/avatar/photo").then(() => undefined),
  // Картинке нужен заголовок авторизации, поэтому фото приходит blob-ом, а не адресом для <img>
  photo: (version: number) => call("GET", `/api/profile/avatar/photo?v=${version}`).then((response) => response.blob()),
  leaderboard: (scope: LeaderboardScope, period: LeaderboardPeriod) =>
    request<Leaderboard>("GET", `/api/leaderboard?scope=${scope}&period=${period}`),
  notifications: () => request<NotificationList>("GET", "/api/notifications"),
  readAllNotifications: () => request<{ status: string }>("POST", "/api/notifications/read-all"),
  myAnalytics: () => request<Analytics>("GET", "/api/analytics/me"),
  employeeAnalytics: (employeeId: string) => request<Analytics>("GET", `/api/analytics/employees/${employeeId}`),
  team: () => request<TeamMember[]>("GET", "/api/analytics/team"),
};
