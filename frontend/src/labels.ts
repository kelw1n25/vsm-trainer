import type { Role, RunStatus } from "./types";

export const categoryLabels: Record<string, string> = {
  conflict: "Конфликт",
  medical: "Медицина",
  service: "Сервис",
  safety: "Безопасность",
};

export const stageLabels: Record<string, string> = {
  boarding: "На посадке",
  onboard: "В пути",
};

export const outcomeLabels: Record<RunStatus, string> = {
  in_progress: "В процессе",
  success: "Успех",
  partial: "Частичный успех",
  failure: "Провал",
};

export const roleLabels: Record<Role, string> = {
  conductor: "Проводник ВСМ",
  instructor: "Инструктор",
};

export function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

/** «Смирнов Алексей Андреевич» → «Алексей». */
export function firstName(fullName: string): string {
  return fullName.split(" ")[1] ?? fullName;
}

/** Склонение по числу: plural(3, ["финал", "финала", "финалов"]) → «финала». */
export function plural(count: number, forms: [string, string, string]): string {
  const tens = count % 100;
  const units = count % 10;
  if (tens >= 11 && tens <= 14) return forms[2];
  if (units === 1) return forms[0];
  if (units >= 2 && units <= 4) return forms[1];
  return forms[2];
}
