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

export interface DebriefStep {
  kind: "choice" | "timeout";
  situation: string;
  chosen_text: string | null;
  explanation: string | null;
  was_best: boolean;
  best_text: string | null;
  best_explanation: string | null;
  loyalty_delta: number;
  safety_delta: number;
  loyalty_after: number;
  safety_after: number;
  competences: Record<string, number>;
  elapsed_seconds: number | null;
  timer_seconds: number | null;
}

export interface Debrief {
  run_id: string;
  scenario_id: string;
  scenario_title: string;
  outcome: RunStatus;
  reason: FinishReason;
  final_text: string;
  xp_earned: number;
  competence_points: Record<string, number>;
  initial_loyalty: number;
  initial_safety: number;
  loyalty: number;
  safety: number;
  decisions: number;
  best_decisions: number;
  timeouts: number;
  average_reaction_seconds: number | null;
  steps: DebriefStep[];
}

export interface Achievement {
  code: string;
  title: string;
  description: string;
}

export interface Level {
  level: number;
  title: string;
  xp: number;
  level_xp: number;
  next_level_xp: number | null;
}

export interface HistoryItem {
  run_id: string;
  scenario_id: string;
  scenario_title: string;
  category: string;
  outcome: RunStatus;
  xp_earned: number;
  loyalty: number;
  safety: number;
  finished_at: string;
}

export interface Profile {
  id: number;
  full_name: string;
  personnel_number: string;
  role: Role;
  brigade: string;
  depot: string;
  level: Level;
  competence_points: Record<string, number>;
  achievements: (Achievement & { earned_at: string | null })[];
  history: HistoryItem[];
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
  new_achievements: Achievement[];
  level_up: Level | null;
  server_time: string;
}
