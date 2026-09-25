import type { CSSProperties, KeyboardEvent, ReactNode } from "react";
import { useNavigate } from "react-router";
import { categoryLabels } from "../labels";
import type { ScenarioSummary } from "../types";
import { DifficultyIndicator } from "./DifficultyIndicator";
import { ArrowRightIcon, PinIcon } from "./icons";
import { ScenarioImage } from "./illustrations";

interface Props {
  scenario: ScenarioSummary;
  index: number;
  onStart: (scenarioId: string) => void;
}

/** Два последних слова держатся вместе: на новой строке не остаётся одинокое «км/ч». */
function withoutOrphan(title: string): ReactNode {
  const words = title.split(" ");
  if (words.length < 3) return title;
  const tail = words.splice(-2).join("\u00a0");
  return (
    <>
      {words.join(" ")} <span className="nowrap">{tail}</span>
    </>
  );
}

/** Карточка целиком открывает страницу сценария, кнопка «Начать» — сразу прохождение. */
export function ScenarioCard({ scenario, index, onStart }: Props) {
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
      // Порядковый номер задаёт задержку появления: карточки выезжают по очереди
      style={{ "--i": index } as CSSProperties}
    >
      <div className="scenario-card__body">
        <div className="tags">
          <span className="tag tag--category">{categoryLabels[scenario.category] ?? scenario.category}</span>
          {scenario.demo && <span className="tag">Демо</span>}
        </div>
        <h3 className="scenario-card__title">{withoutOrphan(scenario.title)}</h3>
        <p className="route">
          <PinIcon />
          <span className="nowrap">{scenario.route}</span> · <span className="nowrap">{scenario.service_class}</span>
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
