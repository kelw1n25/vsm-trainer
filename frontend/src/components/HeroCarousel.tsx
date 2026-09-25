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
// Один «щелчок» колеса (deltaY ≈ 100) сдвигает ползунок на 1/8 пути
const WHEEL_SENSITIVITY = 1 / 800;
// Доля оставшегося пути, которую поезд проходит за кадр при прокрутке колесом
const GLIDE_FACTOR = 0.2;

function motionReduced(): boolean {
  return (
    document.documentElement.hasAttribute("data-reduce-motion") ||
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

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
  const heroRef = useRef<HTMLElement>(null);
  const progressRef = useRef(0);
  const targetRef = useRef(0);
  const glideFrame = useRef(0);

  const show = useCallback((value: number) => {
    progressRef.current = value;
    setProgress(value);
    setMoving(true);
    clearTimeout(windTimer.current);
    windTimer.current = setTimeout(() => setMoving(false), WIND_AFTER_MS);
  }, []);

  // Курсор, палец и клавиатура: поезд сразу встаёт в нужное место
  const move = useCallback(
    (value: number, animated: boolean) => {
      cancelAnimationFrame(glideFrame.current);
      glideFrame.current = 0;
      targetRef.current = value;
      setSmooth(animated);
      show(value);
    },
    [show],
  );

  // Колесо: цель сдвигается «щелчками», а поезд плавно доезжает до неё по кадрам
  const glideTo = useCallback(
    (target: number) => {
      targetRef.current = target;
      setSmooth(false);
      if (motionReduced()) {
        show(target);
        return;
      }
      if (glideFrame.current) return;
      const step = () => {
        const remaining = targetRef.current - progressRef.current;
        if (Math.abs(remaining) < 0.001) {
          show(targetRef.current);
          glideFrame.current = 0;
          return;
        }
        show(progressRef.current + remaining * GLIDE_FACTOR);
        glideFrame.current = requestAnimationFrame(step);
      };
      glideFrame.current = requestAnimationFrame(step);
    },
    [show],
  );

  useEffect(() => {
    const hero = heroRef.current!;
    const onWheel = (event: WheelEvent) => {
      const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY;
      const target = Math.min(1, Math.max(0, targetRef.current + delta * WHEEL_SENSITIVITY));
      // Поезд упёрся в начало или конец пути — отдаём колесо странице, чтобы она прокручивалась дальше
      if (target === targetRef.current) return;
      event.preventDefault();
      glideTo(target);
    };
    hero.addEventListener("wheel", onWheel, { passive: false });
    return () => hero.removeEventListener("wheel", onWheel);
  }, [glideTo]);

  useEffect(
    () => () => {
      clearTimeout(windTimer.current);
      cancelAnimationFrame(glideFrame.current);
    },
    [],
  );

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
    <section className="hero" aria-roledescription="карусель" ref={heroRef}>
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
