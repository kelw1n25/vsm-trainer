import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from "react";

interface Props {
  /** Положение от 0 до 1. */
  value: number;
  steps: number;
  label: string;
  /** smooth — короткая плавность (клавиатура); при перетаскивании и колесе движение мгновенное. */
  onChange: (value: number, smooth: boolean) => void;
}

const KEY_STEP = 0.05;
const WHEEL_SENSITIVITY = 1 / 800;

/** Единый ползунок hero: от его положения зависят слайд и путь поезда. */
export function HeroScroller({ value, steps, label, onChange }: Props) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState(false);
  const valueRef = useRef(value);
  valueRef.current = value;

  function valueAt(clientX: number): number {
    const rect = trackRef.current!.getBoundingClientRect();
    return Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
  }

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
    setDragging(true);
    onChange(valueAt(event.clientX), false);
  }

  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (dragging) onChange(valueAt(event.clientX), false);
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

  // Колесо мыши и тачпад над ползунком двигают поезд; обработчик не пассивный, чтобы страница не прокручивалась
  useEffect(() => {
    const track = trackRef.current!;
    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const delta = (Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY) * WHEEL_SENSITIVITY;
      onChange(Math.min(1, Math.max(0, valueRef.current + delta)), false);
    };
    track.addEventListener("wheel", onWheel, { passive: false });
    return () => track.removeEventListener("wheel", onWheel);
  }, [onChange]);

  const percent = value * 100;
  return (
    <div
      ref={trackRef}
      className={`scroller ${dragging ? "scroller--dragging" : ""}`}
      role="slider"
      tabIndex={0}
      aria-label="Прокрутка слайдов"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(percent)}
      aria-valuetext={label}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={() => setDragging(false)}
      onPointerCancel={() => setDragging(false)}
      onKeyDown={onKeyDown}
    >
      <div className="scroller__track">
        <div className="scroller__fill" style={{ width: `${percent}%` }} />
        {Array.from({ length: steps }, (_, i) => (
          <span key={i} className="scroller__tick" style={{ left: `${steps > 1 ? (i / (steps - 1)) * 100 : 0}%` }} />
        ))}
        <div className="scroller__thumb" style={{ left: `${percent}%` }} />
      </div>
    </div>
  );
}
