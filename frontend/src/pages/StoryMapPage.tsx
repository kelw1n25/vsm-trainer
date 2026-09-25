import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import { api, ApiError } from "../api";
import { ChevronLeftIcon } from "../components/icons";
import { useScenarios, useStartScenario } from "../hooks";
import { outcomeLabels } from "../labels";
import type { StoryMap } from "../types";

type MapNode = StoryMap["nodes"][number];

/** Архив веток: какие развилки сотрудник уже исследовал и какие финалы открыл. Закрытое — без содержания. */
export function StoryMapPage() {
  const { scenarioId } = useParams() as { scenarioId: string };
  const { scenarios } = useScenarios();
  const { start, error: startError } = useStartScenario();
  const [map, setMap] = useState<StoryMap | null>(null);
  const [error, setError] = useState<string | null>(null);
  const owners = useMemo(() => (map ? firstAppearance(map) : {}), [map]);

  useEffect(() => {
    api
      .storyMap(scenarioId)
      .then(setMap)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить карту истории"));
  }, [scenarioId]);

  if (error) return <p className="error">{error}</p>;
  if (!map) return <p className="muted">Загрузка…</p>;

  const title = scenarios?.find((s) => s.id === scenarioId)?.title ?? "Сценарий";
  const nodes = Object.fromEntries(map.nodes.map((node) => [node.id, node]));
  const endings = Object.fromEntries(map.endings.map((ending) => [ending.id, ending]));
  const reached = map.endings.filter((ending) => ending.reached).length;

  return (
    <div className="stack">
      <Link to={`/scenarios/${scenarioId}`} className="back-link">
        <ChevronLeftIcon /> К сценарию
      </Link>
      <h1 className="page-title">Развитие истории</h1>
      <p className="muted story-map__lead">
        «{title}» — пройдено раз: {map.playthroughs}, исследовано решений: {map.choices_explored} из {map.choices_total},
        открыто финалов: {reached} из {map.endings.length}. Неисследованные ветки остаются закрытыми.
      </p>

      <section className="card story-map">
        <p className="story-map__root">Начало</p>
        <Branch nodeId={map.start_node} via={ROOT} nodes={nodes} endings={endings} owners={owners} />
      </section>

      <section className="card">
        <h2>Финалы</h2>
        <ul className="story-map__endings">
          {map.endings.map((ending) => (
            <li key={ending.id} className={ending.reached ? `is-reached outcome--${ending.outcome}` : "is-locked"}>
              {ending.reached ? (
                <>
                  <strong>{ending.ending}</strong>
                  <span>{outcomeLabels[ending.outcome]}</span>
                </>
              ) : (
                <>
                  <strong>🔒 Финал не открыт</strong>
                  <span>Попробуйте другую линию поведения</span>
                </>
              )}
            </li>
          ))}
        </ul>
        {startError && <p className="error">{startError}</p>}
        <button className="button" onClick={() => start(scenarioId)}>
          Пройти заново
        </button>
      </section>
    </div>
  );
}

const ROOT = "root";

/**
 * В графе ветки сходятся: узел разворачивается там, где встретился впервые при обходе в глубину,
 * а в остальных местах показывается ссылка «сходится с веткой». Считается заранее, без побочных эффектов при отрисовке.
 */
function firstAppearance(map: StoryMap): Record<string, string> {
  const nodes = Object.fromEntries(map.nodes.map((node) => [node.id, node]));
  const owners: Record<string, string> = {};
  const visit = (nodeId: string, via: string) => {
    if (owners[nodeId] || !nodes[nodeId]) return;
    owners[nodeId] = via;
    const node = nodes[nodeId];
    for (const choice of node.choices) if (choice.next) visit(choice.next, `${nodeId}:${choice.id}`);
    if (node.timeout_next) visit(node.timeout_next, `${nodeId}:timeout`);
  };
  visit(map.start_node, ROOT);
  return owners;
}

interface BranchProps {
  nodeId: string;
  /** Через какой переход пришли в узел. */
  via: string;
  nodes: Record<string, MapNode>;
  endings: Record<string, StoryMap["endings"][number]>;
  owners: Record<string, string>;
}

function Branch({ nodeId, via, nodes, endings, owners }: BranchProps) {
  const ending = endings[nodeId];
  if (ending) {
    return (
      <p className={`story-map__ending outcome--${ending.outcome}`}>
        Финал: <strong>{ending.ending}</strong>
      </p>
    );
  }
  const node = nodes[nodeId];
  if (!node) return null;
  if (owners[nodeId] !== via) return <p className="story-map__again">→ сходится с веткой «{node.situation}»</p>;

  return (
    <div className="story-map__node">
      <p className="story-map__situation">{node.situation}</p>
      <ul className="story-map__choices">
        {node.choices.map((choice) => (
          <li key={choice.id} className={choice.explored ? "is-explored" : "is-locked"}>
            {choice.explored ? (
              <>
                <span className="story-map__choice">✓ {choice.text}</span>
                {choice.next && (
                  <Branch nodeId={choice.next} via={`${node.id}:${choice.id}`} nodes={nodes} endings={endings} owners={owners} />
                )}
              </>
            ) : (
              <span className="story-map__choice">🔒 Ветка не исследована</span>
            )}
          </li>
        ))}
        {node.timer && (
          <li className={node.timeout_explored ? "is-explored" : "is-locked"}>
            {node.timeout_explored ? (
              <>
                <span className="story-map__choice">⏱ Время на решение истекло</span>
                {node.timeout_next && (
                  <Branch nodeId={node.timeout_next} via={`${node.id}:timeout`} nodes={nodes} endings={endings} owners={owners} />
                )}
              </>
            ) : (
              <span className="story-map__choice">🔒 Ветка не исследована</span>
            )}
          </li>
        )}
      </ul>
    </div>
  );
}
