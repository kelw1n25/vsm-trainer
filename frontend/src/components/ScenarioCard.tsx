import type { KeyboardEvent } from "react";
import { useNavigate } from "react-router";
import { categoryLabels } from "../labels";
import type { ScenarioSummary } from "../types";
import { DifficultyIndicator } from "./DifficultyIndicator";
import { ArrowRightIcon, PinIcon } from "./icons";
import { ScenarioImage } from "./illustrations";

interface Props {
  scenario: ScenarioSummary;
  onStart: (scenarioId: string) => void;
}

/** Карточка целиком открывает страницу сценария, кнопка «Начать» — сразу прохождение. */
export function ScenarioCard({ scenario, onStart }: Props) {
  const navigate = useNavigate();
  const open = () => navigate(`/scenarios/${scenario.id}`);

  function onKeyDown(event: KeyboardEvent<HTMLElement>) {
    // Enter на вложенной кнопке не должен открывать ещё и страницу сценария
    if (event.key === "Enter" && event.target === event.currentTarget) open();
  }

  return (
    <article
      className="scenario-card"
      role="link"
      tabIndex={0}
      aria-label={`Сценарий «${scenario.title}»`}
      onClick={open}
      onKeyDown={onKeyDown}
    >
      <div className="scenario-card__body">
        <div className="tags">
          <span className="tag tag--category">{categoryLabels[scenario.category] ?? scenario.category}</span>
          {scenario.demo && <span className="tag">Демо</span>}
        </div>
        <h3 className="scenario-card__title">{scenario.title}</h3>
        <p className="route">
          <PinIcon />
          {scenario.route} · {scenario.service_class}
        </p>
        <DifficultyIndicator level={scenario.difficulty} />
        <button
          className="button scenario-card__start"
          onClick={(event) => {
            event.stopPropagation();
            onStart(scenario.id);
          }}
        >
          Начать <ArrowRightIcon />
        </button>
      </div>
      <div className="scenario-card__image">
        <ScenarioImage scenarioId={scenario.id} category={scenario.category} />
      </div>
    </article>
  );
}
