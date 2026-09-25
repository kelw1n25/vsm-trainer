import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api, ApiError } from "./api";
import type { Meta, ScenarioSummary } from "./types";

// Справочники и каталог одинаковы для всех пользователей — загружаем один раз за сессию вкладки
let metaRequest: Promise<Meta> | null = null;
let scenariosRequest: Promise<ScenarioSummary[]> | null = null;

export function useMeta(): Meta | null {
  const [meta, setMeta] = useState<Meta | null>(null);
  useEffect(() => {
    metaRequest ??= api.meta();
    metaRequest.then(setMeta).catch(() => {
      metaRequest = null;
    });
  }, []);
  return meta;
}

/** Названия компетенций из config/game.yaml на backend. */
export function useCompetenceNames(): Record<string, string> {
  return useMeta()?.competences ?? {};
}

export function useScenarios(): { scenarios: ScenarioSummary[] | null; error: string | null } {
  const [scenarios, setScenarios] = useState<ScenarioSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    scenariosRequest ??= api.scenarios();
    scenariosRequest.then(setScenarios).catch((e) => {
      scenariosRequest = null;
      setError(e instanceof ApiError ? e.message : "Не удалось загрузить сценарии");
    });
  }, []);
  return { scenarios, error };
}

/** Старт сценария (или продолжение незавершённого) и переход на экран прохождения. */
export function useStartScenario(): { start: (scenarioId: string) => void; error: string | null } {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const start = useCallback(
    (scenarioId: string) => {
      setError(null);
      api
        .startRun(scenarioId)
        .then((run) => navigate(`/runs/${run.id}`))
        .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось начать сценарий"));
    },
    [navigate],
  );
  return { start, error };
}

/** Текущее время, обновляется каждые intervalMs — для обратного отсчёта. */
export function useNow(intervalMs: number): number {
  const [now, setNow] = useState(Date.now);
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}
