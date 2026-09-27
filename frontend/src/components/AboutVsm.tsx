import { useEffect, useState } from "react";
import { Link } from "react-router";
import { api } from "../api";
import type { ServiceClass } from "../types";
import { ChevronRightIcon } from "./icons";

// Главное, что проводник держит в голове на каждом рейсе
const RULES = [
  "О прибытии объявлять за 10–15 минут",
  "Неотложные просьбы, например первая помощь, — вне очереди",
  "Говорить по ролевой модели: признать → правило → решение → заверить",
];

/**
 * «Про ВСМ» на главной — общая картина за полминуты чтения: цифры магистрали, классы обслуживания
 * и три правила, на которых держатся сценарии. Классы и нормы — из справочника кейсодержателя на сервере.
 */
export function AboutVsm() {
  const [classes, setClasses] = useState<ServiceClass[]>([]);
  useEffect(() => {
    api
      .handbook()
      .then((handbook) => setClasses(handbook.service_classes))
      .catch(() => {
        // без справочника остаются цифры магистрали и правила — блок не пустеет
      });
  }, []);

  return (
    <section className="section about-vsm">
      <div className="section__head">
        <h2 className="section__title">Про ВСМ</h2>
      </div>
      <p className="muted about-vsm__lead">
        Высокоскоростная магистраль Москва — Санкт-Петербург: премиальный сервис, где решения принимаются за секунды.
      </p>
      <div className="kpis">
        <div className="card kpi">
          <span className="kpi__label">Скорость</span>
          <strong className="kpi__value">до 400 км/ч</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Москва — СПб</span>
          <strong className="kpi__value">≈ 2 ч 15 мин</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Классы сервиса</span>
          <strong className="kpi__value">{classes.length || "—"}</strong>
        </div>
        <div className="card kpi">
          <span className="kpi__label">Стоянка на станции</span>
          <strong className="kpi__value">≈ 1 мин</strong>
        </div>
      </div>
      <div className="two-columns about-vsm__cards">
        {classes.length > 0 && (
          <div className="card">
            <h3>Классы и ожидание сервиса</h3>
            <ul className="about-vsm__classes">
              {classes.map((item) => (
                <li key={item.code}>
                  <strong>{item.title}</strong>
                  <span className="tag">{item.layout}</span>
                  <span className="muted">до {item.max_wait_minutes} мин</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="card">
          <h3>Что важно проводнику</h3>
          <ul className="mistakes">
            {RULES.map((rule) => (
              <li key={rule}>{rule}</li>
            ))}
          </ul>
        </div>
      </div>
      <Link to="/handbook" className="link-action">
        Подробнее — в справочнике
        <ChevronRightIcon />
      </Link>
    </section>
  );
}
