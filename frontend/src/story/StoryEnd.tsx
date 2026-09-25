import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router";
import { api } from "../api";
import { AnimatedNumber } from "../components/AnimatedNumber";
import { CompetenceList } from "../components/CompetenceList";
import { Rewards } from "../components/Rewards";
import type { RunState, StoryMap } from "../types";
import type { StoryEngine } from "./StoryEngine";

/** Финальный экран: название финала, к чему привели решения, награды и что дальше. */
export function StoryEnd({ run, engine }: { run: RunState; engine: StoryEngine }) {
  const navigate = useNavigate();
  const [map, setMap] = useState<StoryMap | null>(null);
  const [restarting, setRestarting] = useState(false);
  const final = run.final!;

  useEffect(() => {
    api.storyMap(run.scenario_id).then(setMap).catch(() => setMap(null));
  }, [run.scenario_id]);

  async function restart() {
    setRestarting(true);
    try {
      navigate(`/runs/${await engine.restartScenario()}`);
    } catch {
      setRestarting(false);
    }
  }

  const reached = map?.endings.filter((ending) => ending.reached).length;
  return (
    <section className={`story-end story-end--${final.outcome}`} aria-live="polite">
      <div className="story-end__card">
        <p className="story-end__eyebrow">Сценарий завершён</p>
        <h1 className="story-end__title">{final.ending}</h1>
        <p className="story-end__text">{final.text}</p>

        <div className="story-end__stats">
          <div>
            <span>Получено XP</span>
            <strong>
              +<AnimatedNumber value={final.xp_earned} />
            </strong>
          </div>
          <div>
            <span>Лояльность пассажира</span>
            <strong>{run.loyalty}%</strong>
          </div>
          <div>
            <span>Рейтинг безопасности</span>
            <strong>{run.safety}%</strong>
          </div>
          {map && (
            <div>
              <span>Открыто финалов</span>
              <strong>
                {reached} из {map.endings.length}
              </strong>
            </div>
          )}
        </div>
        <CompetenceList points={final.competence_points} />
        <Rewards achievements={run.new_achievements} levelUp={run.level_up} />

        <div className="story-end__actions">
          <Link className="button" to={`/scenarios/${run.scenario_id}/map`}>
            Посмотреть путь
          </Link>
          <Link className="button button--ghost" to={`/runs/${run.id}/debrief`}>
            Разбор решений
          </Link>
          <button className="button button--ghost" disabled={restarting} onClick={restart}>
            Пройти заново
          </button>
          <Link className="button button--ghost" to="/scenarios">
            Вернуться к сценариям
          </Link>
        </div>
      </div>
    </section>
  );
}
