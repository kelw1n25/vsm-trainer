import type { RunStatus } from "./types";

export const categoryLabels: Record<string, string> = {
  conflict: "Конфликт",
  medical: "Медицина",
  service: "Сервис",
  safety: "Безопасность",
};

export const speakerLabels: Record<string, string> = {
  passenger: "Пассажир",
  colleague: "Коллега",
  train_chief: "Начальник поезда",
};

export const outcomeLabels: Record<RunStatus, string> = {
  in_progress: "В процессе",
  success: "Успех",
  partial: "Частичный успех",
  failure: "Провал",
};

export function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}
