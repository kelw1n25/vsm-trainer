import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../api";
import { useAuth } from "../auth";
import { useMeta, useScenarios } from "../hooks";
import { firstName } from "../labels";
import type { Analytics } from "../types";
import { HeroScroller } from "./HeroScroller";
import { StarIcon } from "./icons";
import { HeroTrain } from "./illustrations";

// Сколько поезд проезжает за весь ход ползунка (единицы viewBox иллюстрации)
const TRAIN_DISTANCE = 220;
// Сколько после последнего движения линии скорости ещё «дуют» сильнее
const WIND_AFTER_MS = 350;

interface Slide {
  eyebrow: string;
  title: [string, string];
  text: string;
  note: string;
}

/** Hero с ползунком: приветствие, челлендж недели, рекомендация; ползунок ведёт поезд. */
export function HeroCarousel() {
  const { session } = useAuth();
  const meta = useMeta();
  const { scenarios } = useScenarios();
  const [recommendation, setRecommendation] = useState<Analytics["recommendation"]>(null);
  const [progress, setProgress] = useState(0);
  const [smooth, setSmooth] = useState(false);
  const [moving, setMoving] = useState(false);
  const windTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const move = useCallback((value: number, animated: boolean) => {
    setProgress(value);
    setSmooth(animated);
    setMoving(true);
    clearTimeout(windTimer.current);
    windTimer.current = setTimeout(() => setMoving(false), WIND_AFTER_MS);
  }, []);

  useEffect(() => () => clearTimeout(windTimer.current), []);

  useEffect(() => {
    // Без рекомендации в карусели просто на один слайд меньше
    api
      .myAnalytics()
      .then((analytics) => setRecommendation(analytics.recommendation))
      .catch(() => {});
  }, []);

  const slides: Slide[] = [
    {
      eyebrow: `Привет, ${session ? firstName(session.fullName) : "коллега"}!`,
      title: ["Развивай навыки —", "строй будущее ВСМ!"],
      text: "Пройди сценарии, получай баллы, поднимайся в рейтинге и становись экспертом ВСМ.",
      note: "Твой прогресс влияет на общую безопасность!",
    },
  ];
  const challenge = meta?.weekly_challenge;
  const challengeTitle = scenarios?.find((s) => s.id === challenge?.scenario_id)?.title;
  if (challenge && challengeTitle) {
    slides.push({
      eyebrow: "Челлендж недели",
      title: ["Пройди на успех:", challengeTitle],
      text: `Заверши сценарий успешно до конца недели и получи бонус +${challenge.bonus_xp} XP.`,
      note: `+${challenge.bonus_xp} XP за успешное прохождение!`,
    });
  }
  if (recommendation) {
    slides.push({
      eyebrow: "Рекомендация для тебя",
      title: ["Следующий шаг:", recommendation.title],
      text: recommendation.reason,
      note: "Закрой пробел — и навык вырастет быстрее!",
    });
  }
  // Слайд — ближайший к положению ползунка
  const current = slides[Math.round(progress * (slides.length - 1))];
  const longTitle = current.title[1].length > 24;

  return (
    <section className="hero" aria-roledescription="карусель">
      <HeroTrain offset={progress * TRAIN_DISTANCE} moving={moving} smooth={smooth} />
      <div className="hero__content" key={current.eyebrow}>
        <p className="hero__eyebrow">{current.eyebrow}</p>
        <h1 className={`hero__title ${longTitle ? "hero__title--long" : ""}`}>
          {current.title[0]}
          <br />
          {current.title[1]}
        </h1>
        <p className="hero__text">{current.text}</p>
      </div>
      <div className="hero__note" key={`${current.eyebrow}-note`}>
        <StarIcon />
        <span>{current.note}</span>
      </div>
      {slides.length > 1 && (
        <div className="hero__scroller">
          <HeroScroller value={progress} steps={slides.length} label={current.eyebrow} onChange={move} />
        </div>
      )}
    </section>
  );
}
