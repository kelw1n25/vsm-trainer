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
  weekly_challenge: { scenario_id: string; bonus_xp: number } | null;
}

export interface ScenarioSummary {
  id: string;
  title: string;
  category: string;
  difficulty: number;
  service_class: string;
  route: string;
  description: string;
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
  runs_completed: number;
  competence_points: Record<string, number>;
  achievements: (Achievement & { earned_at: string | null })[];
  history: HistoryItem[];
}

export interface Analytics {
  employee_id: number;
  full_name: string;
  total_runs: number;
  progress: { week_start: string; xp: number; runs: number; successes: number }[];
  categories: {
    category: string;
    title: string;
    runs: number;
    decisions: number;
    timed_steps: number;
    success_rate: number | null;
    best_choice_rate: number | null;
    timeout_rate: number | null;
    average_reaction_share: number | null;
  }[];
  competences: { code: string; title: string; points: number }[];
  strengths: string[];
  weaknesses: string[];
  mistakes: string[];
  recommendation: { scenario_id: string; title: string; reason: string } | null;
}

export interface TeamMember {
  employee_id: number;
  full_name: string;
  brigade: string;
  level_title: string;
  xp: number;
  last_activity_at: string | null;
  weakest_competence: string | null;
}

export interface NotificationList {
  unread: number;
  items: { id: number; type: string; title: string; body: string; created_at: string; read: boolean }[];
}

export type LeaderboardScope = "brigade" | "depot" | "company";
export type LeaderboardPeriod = "week" | "month" | "all";

export interface Leaderboard {
  scope: LeaderboardScope;
  period: LeaderboardPeriod;
  title: string;
  participants: number;
  rows: {
    rank: number;
    employee_id: number;
    full_name: string;
    brigade: string;
    depot: string;
    level_title: string;
    points: number;
    is_me: boolean;
  }[];
}

export interface RunState {
  id: string;
  scenario_id: string;
  scenario_title: string;
  category: string;
  difficulty: number;
  route: string;
  service_class: string;
  steps_taken: number;
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
