import { useEffect, useSyncExternalStore } from "react";
import { api } from "./api";
import { useAuth } from "./auth";
import type { AvatarBackground, AvatarConfig, AvatarHeadwear, AvatarTie } from "./types";

// Варианты конструктора — те же, что принимает сервер (backend/app/profiles/avatar.py)
export const DEFAULT_AVATAR: AvatarConfig = { background: "blue", headwear: "cap", tie: "red" };

export const AVATAR_BACKGROUNDS: Record<AvatarBackground, { label: string; top: string; bottom: string }> = {
  blue: { label: "Голубой", top: "#EAF2FF", bottom: "#C9DDFB" },
  mint: { label: "Мятный", top: "#E6F7EF", bottom: "#BFE8D3" },
  sand: { label: "Песочный", top: "#FFF4E4", bottom: "#F5D9AE" },
  lilac: { label: "Сиреневый", top: "#F1ECFF", bottom: "#D8CCF7" },
  coral: { label: "Коралловый", top: "#FDECEC", bottom: "#F6C6C6" },
  night: { label: "Ночной", top: "#2A3A5E", bottom: "#16223F" },
};

export const AVATAR_HEADWEAR: Record<AvatarHeadwear, string> = { cap: "Фуражка", none: "Без головного убора" };

export const AVATAR_TIES: Record<AvatarTie, { label: string; color: string }> = {
  red: { label: "Красный", color: "#D23A3A" },
  blue: { label: "Синий", color: "#2F6FDD" },
  green: { label: "Зелёный", color: "#169C7A" },
  graphite: { label: "Графитовый", color: "#3A4460" },
};

// Аватар вошедшего сотрудника — один на все места (шапка, профиль, рейтинг): смена в настройках видна сразу
let current: { employeeId: number; avatar: AvatarConfig } | null = null;
const listeners = new Set<() => void>();

function publish(employeeId: number, avatar: AvatarConfig): void {
  current = { employeeId, avatar };
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Аватар текущего сотрудника; пока профиль не загружен — аватар по умолчанию. */
export function useMyAvatar(): AvatarConfig {
  const { session } = useAuth();
  const employeeId = session?.employeeId;
  const snapshot = useSyncExternalStore(subscribe, () => current);
  useEffect(() => {
    if (employeeId === undefined || current?.employeeId === employeeId) return;
    api
      .profile()
      .then((profile) => publish(employeeId, profile.avatar))
      .catch(() => {
        // без профиля остаётся аватар по умолчанию
      });
  }, [employeeId]);
  return snapshot && snapshot.employeeId === employeeId ? snapshot.avatar : DEFAULT_AVATAR;
}

/** Сохранить выбор на сервере; при ошибке вернуть прежний и пробросить ошибку экрану. */
export async function saveMyAvatar(employeeId: number, next: AvatarConfig, previous: AvatarConfig): Promise<void> {
  publish(employeeId, next);
  try {
    publish(employeeId, await api.updateAvatar(next));
  } catch (error) {
    publish(employeeId, previous);
    throw error;
  }
}
