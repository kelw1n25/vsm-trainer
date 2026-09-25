import { useEffect, useState } from "react";
import { api } from "./api";

let competencesRequest: Promise<Record<string, string>> | null = null;

/** Названия компетенций из config/game.yaml на backend (загружаются один раз). */
export function useCompetenceNames(): Record<string, string> {
  const [names, setNames] = useState<Record<string, string>>({});
  useEffect(() => {
    competencesRequest ??= api.meta().then((meta) => meta.competences);
    competencesRequest.then(setNames).catch(() => {
      competencesRequest = null;
    });
  }, []);
  return names;
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
