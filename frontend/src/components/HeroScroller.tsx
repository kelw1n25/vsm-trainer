import { useRef, useState, type KeyboardEvent, type PointerEvent } from "react";

interface Props {
  /** Положение от 0 до 1. */
  value: number;
  steps: number;
  label: string;
  /** smooth — короткая плавность (клавиатура); за курсором и пальцем движение мгновенное. */
  onChange: (value: number, smooth: boolean) => void;
  /** Курсор ушёл с ползунка или палец отпущен — ползунок докатывается до ближайшего слайда. */
  onRelease: () => void;
}

const KEY_STEP = 0.05;
// Сенсорный экран: ни курсора, ни колеса — подсказка про палец
const TOUCH = typeof window !== "undefined" && window.matchMedia("(pointer: coarse)").matches;

/**
 * Единый ползунок hero. Мышью управляется наведением — достаточно вести курсор над ним,
 * без клика. На сенсорных экранах — перетаскиванием пальцем. Колесо обрабатывает весь hero.
 */
export function HeroScroller({ value, steps, label, onChange, onRelease }: Props) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState(false);

  function valueAt(clientX: number): number {
    const rect = trackRef.current!.getBoundingClientRect();
    return Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
  }

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    if (event.pointerType !== "mouse") {
      event.currentTarget.setPointerCapture(event.pointerId);
      setDragging(true);
    }
    onChange(valueAt(event.clientX), false);
  }

  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (event.pointerType === "mouse" || dragging) onChange(valueAt(event.clientX), false);
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const next = {
      ArrowRight: value + KEY_STEP,
      ArrowUp: value + KEY_STEP,
      ArrowLeft: value - KEY_STEP,
      ArrowDown: value - KEY_STEP,
      Home: 0,
      End: 1,
    }[event.key];
    if (next === undefined) return;
    event.preventDefault();
    onChange(Math.min(1, Math.max(0, next)), true);
  }

  const percent = value * 100;
  return (
    <div
      className={`scroller ${dragging ? "scroller--dragging" : ""}`}
      role="slider"
      tabIndex={0}
      aria-label={TOUCH ? "Прокрутка слайдов: проведите пальцем" : "Прокрутка слайдов: ведите курсором или крутите колесо мыши"}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(percent)}
      aria-valuetext={label}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerLeave={(event) => event.pointerType === "mouse" && onRelease()}
      onPointerUp={(event) => {
        setDragging(false);
        if (event.pointerType !== "mouse") onRelease();
      }}
      onPointerCancel={() => {
        setDragging(false);
        onRelease();
      }}
      onKeyDown={onKeyDown}
    >
      <div className="scroller__track" ref={trackRef}>
        <div className="scroller__fill" style={{ width: `${percent}%` }} />
        {Array.from({ length: steps }, (_, i) => (
          <span key={i} className="scroller__tick" style={{ left: `${steps > 1 ? (i / (steps - 1)) * 100 : 0}%` }} />
        ))}
        <div className="scroller__thumb" style={{ left: `${percent}%` }} />
      </div>
      <span className="scroller__hint">{TOUCH ? "⇆ проведите пальцем" : "⇆ ведите курсором или крутите колесо"}</span>
    </div>
  );
}
