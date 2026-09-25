import { useEffect, useState } from "react";
import { api } from "../api";
import { useAuth } from "../auth";
import { useMeta, useScenarios } from "../hooks";
import { firstName } from "../labels";
import type { Analytics } from "../types";
import { StarIcon } from "./icons";
import { HeroTrain } from "./illustrations";

interface Slide {
  eyebrow: string;
  title: [string, string];
  text: string;
  note: string;
}

/** Hero с переключаемыми слайдами: приветствие, челлендж недели, персональная рекомендация. */
export function HeroCarousel() {
  const { session } = useAuth();
  const meta = useMeta();
  const { scenarios } = useScenarios();
  const [recommendation, setRecommendation] = useState<Analytics["recommendation"]>(null);
  const [index, setIndex] = useState(0);

  useEffect(() => {
    // Без рекомендации карусель просто покажет на один слайд меньше
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
  const current = slides[Math.min(index, slides.length - 1)];

  return (
    <section className="hero" aria-roledescription="карусель">
      <HeroTrain />
      <div className="hero__content" key={current.eyebrow}>
        <p className="hero__eyebrow">{current.eyebrow}</p>
        <h1 className="hero__title">
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
        <div className="hero__dots" role="tablist" aria-label="Слайды">
          {slides.map((slide, i) => (
            <button
              key={slide.eyebrow}
              role="tab"
              aria-selected={slide === current}
              aria-label={`Слайд ${i + 1}: ${slide.eyebrow}`}
              className={`hero__dot ${slide === current ? "hero__dot--active" : ""}`}
              onClick={() => setIndex(i)}
            />
          ))}
        </div>
      )}
    </section>
  );
}
