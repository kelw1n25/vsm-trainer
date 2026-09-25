import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router";
import { api, ApiError } from "../api";
import { useScenarios } from "../hooks";
import { categoryLabels, stageLabels } from "../labels";
import type { Handbook } from "../types";

// Слайды кейсодержателя о подвижном составе и классах обслуживания ВСМ
const SLIDES = [
  { src: "/media/service-classes.jpg", caption: "Классы обслуживания в поездах ВСМ" },
  { src: "/media/service-classes-layout.jpg", caption: "Компоновка вагонов по классам" },
  { src: "/media/rolling-stock.jpg", caption: "Планировка вагонов комфорт и стандарт, предлагаемые сервисы" },
];

/** Справочник проводника: ролевая модель, классы обслуживания, стандарты и 51 ситуация на борту. */
export function HandbookPage() {
  const [handbook, setHandbook] = useState<Handbook | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [category, setCategory] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const { scenarios } = useScenarios();
  const { hash } = useLocation();

  useEffect(() => {
    api
      .handbook()
      .then(setHandbook)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить справочник"));
  }, []);

  // Ссылка вида /handbook#situation-12 — прокрутить к ситуации, когда справочник загрузится
  useEffect(() => {
    if (handbook && hash) document.getElementById(hash.slice(1))?.scrollIntoView({ block: "start" });
  }, [handbook, hash]);

  // В каких сценариях отрабатывается каждая ситуация
  const trainedIn = useMemo(() => {
    const map: Record<number, { id: string; title: string }[]> = {};
    for (const scenario of scenarios ?? []) {
      for (const number of scenario.situations) (map[number] ??= []).push({ id: scenario.id, title: scenario.title });
    }
    return map;
  }, [scenarios]);

  if (error) return <p className="error">{error}</p>;
  if (!handbook) return <p className="muted">Загрузка…</p>;

  const needle = query.trim().toLowerCase();
  const situations = handbook.situations.filter(
    (s) =>
      (category === null || s.category === category) &&
      (!needle || `${s.title} ${s.reaction} ${s.phrases.join(" ")}`.toLowerCase().includes(needle)),
  );

  return (
    <div className="stack">
      <h1 className="page-title">Справочник проводника</h1>
      <p className="muted handbook__lead">
        Материалы кейсодержателя: «Примеры ситуаций взаимодействия поездного персонала с пассажирами», стандарты
        СТО РЖД 03.011, 03.013 и 03.014, слайды о подвижном составе ВСМ. На эти материалы опираются все сценарии.
      </p>

      <section className="card">
        <h2>{handbook.role_model.title}</h2>
        <ol className="role-model">
          {handbook.role_model.steps.map((step) => (
            <li key={step.code}>
              <strong>{step.title}</strong>
              <ul>
                {step.phrases.map((phrase) => (
                  <li key={phrase}>{phrase}</li>
                ))}
              </ul>
            </li>
          ))}
        </ol>
      </section>

      <section className="card">
        <h2>Классы обслуживания</h2>
        <div className="service-classes">
          {handbook.service_classes.map((item) => (
            <article key={item.code} className="service-class">
              <h3>{item.title}</h3>
              <dl>
                <dt>Компоновка</dt>
                <dd>{item.layout}</dd>
                <dt>Шаг кресел</dt>
                <dd>{item.pitch_mm} мм</dd>
                <dt>Ширина кресла</dt>
                <dd>{item.seat_mm} мм</dd>
                <dt>Проход</dt>
                <dd>{item.aisle_mm} мм</dd>
                <dt>Ожидание</dt>
                <dd>до {item.max_wait_minutes} мин</dd>
              </dl>
              <p className="muted">{item.summary}</p>
            </article>
          ))}
        </div>
        <div className="slides">
          {SLIDES.map((slide) => (
            <figure key={slide.src}>
              <a href={slide.src} target="_blank" rel="noreferrer">
                <img src={slide.src} alt={slide.caption} loading="lazy" />
              </a>
              <figcaption>{slide.caption}</figcaption>
            </figure>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Стандарты обслуживания</h2>
        <div className="standards">
          {handbook.standards.map((item) => (
            <div key={item.title}>
              <strong>{item.title}</strong>
              <p className="muted">{item.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Ситуации на борту и на посадке · {handbook.situations.length}</h2>
        <div className="handbook__filters">
          <input
            type="search"
            placeholder="Поиск: билет, питомец, аллергия…"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            aria-label="Поиск по ситуациям"
          />
          <div className="chips" role="tablist" aria-label="Тип ситуации">
            <button role="tab" aria-selected={category === null} className="chip" onClick={() => setCategory(null)}>
              Все
            </button>
            {Object.entries(categoryLabels).map(([code, title]) => (
              <button key={code} role="tab" aria-selected={category === code} className="chip" onClick={() => setCategory(code)}>
                {title}
              </button>
            ))}
          </div>
        </div>
        {!situations.length && <p className="muted">Ничего не найдено.</p>}
        <div className="standard-list">
          {situations.map((situation) => (
            <article key={situation.number} className="standard" id={`situation-${situation.number}`}>
              <div className="tags">
                <span className="tag tag--category">{categoryLabels[situation.category]}</span>
                <span className="tag">{stageLabels[situation.stage]}</span>
              </div>
              <h3>
                {situation.number}. {situation.title}
              </h3>
              <p>
                <strong>Реакция.</strong> {situation.reaction}
              </p>
              <ul className="standard__phrases">
                {situation.phrases.map((phrase) => (
                  <li key={phrase}>{phrase}</li>
                ))}
              </ul>
              <ul className="standard__comment">
                {situation.comment.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
              {trainedIn[situation.number] && (
                <p className="standard__trained">
                  Отрабатывается:{" "}
                  {trainedIn[situation.number].map((scenario, index) => (
                    <span key={scenario.id}>
                      {index > 0 && ", "}
                      <Link to={`/scenarios/${scenario.id}`}>{scenario.title}</Link>
                    </span>
                  ))}
                </p>
              )}
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
