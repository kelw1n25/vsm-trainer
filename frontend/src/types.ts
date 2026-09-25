// Типы ответов API. Повторяют Pydantic-схемы backend (см. Swagger: /api/docs).

export type Role = "conductor" | "instructor";
export type RunStatus = "in_progress" | "success" | "partial" | "failure";
export type FinishReason = "final" | "loyalty_depleted" | "safety_depleted";

export interface Session {
  token: string;
  employeeId: number;
  fullName: string;
  role: Role;
}

export interface LoginResponse {
  access_token: string;
  employee_id: number;
  full_name: string;
  role: Role;
}

export interface Meta {
  competences: Record<string, string>;
}

export interface ScenarioSummary {
  id: string;
  title: string;
  category: string;
  difficulty: number;
  service_class: string;
  route: string;
  demo: boolean;
}

export interface Line {
  speaker: string;
  name: string | null;
  text: string;
}

export interface NodeState {
  id: string;
  situation: string;
  line: Line | null;
  choices: { id: string; text: string }[];
  timer_seconds: number | null;
  deadline_at: string | null;
  timeout_at: string | null;
}

export interface Step {
  kind: "choice" | "timeout";
  text: string;
  loyalty_delta: number;
  safety_delta: number;
  competences: Record<string, number>;
}

export interface FinalState {
  outcome: RunStatus;
  reason: FinishReason;
  text: string;
  xp_earned: number;
  competence_points: Record<string, number>;
}

export interface RunState {
  id: string;
  scenario_id: string;
  scenario_title: string;
  status: RunStatus;
  loyalty: number;
  safety: number;
  node: NodeState | null;
  final: FinalState | null;
  last_steps: Step[];
  server_time: string;
}
