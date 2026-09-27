import { useId } from "react";

// Небольшие линейные иконки интерфейса. Цвет берут из currentColor.

const common = { fill: "none", stroke: "currentColor", strokeWidth: 2, strokeLinecap: "round", strokeLinejoin: "round" } as const;

export function ArrowRightIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true" {...common}>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

export function ChevronDownIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true" {...common} strokeWidth={2.4}>
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

export function ChevronRightIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" {...common} strokeWidth={2.4}>
      <path d="M9 6l6 6-6 6" />
    </svg>
  );
}

export function ChevronLeftIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" {...common} strokeWidth={2.4}>
      <path d="M15 6l-6 6 6 6" />
    </svg>
  );
}

export function SunIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true" {...common}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  );
}

export function MoonIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true" {...common}>
      <path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z" />
    </svg>
  );
}

export function PinIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5z"
      />
    </svg>
  );
}

function starPoints(cx: number, cy: number, outer: number, inner: number): string {
  return Array.from({ length: 10 }, (_, i) => {
    const radius = i % 2 === 0 ? outer : inner;
    const angle = -Math.PI / 2 + (i * Math.PI) / 5;
    return `${(cx + radius * Math.cos(angle)).toFixed(2)},${(cy + radius * Math.sin(angle)).toFixed(2)}`;
  }).join(" ");
}

/** Объёмная золотая звезда в стиле эмодзи: толстая скруглённая обводка делает лучи «пухлыми». */
export function StarIcon({ size = 56 }: { size?: number }) {
  const id = useId().replace(/:/g, "");
  return (
    <svg className="star" width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
      <defs>
        <radialGradient id={`${id}-gold`} cx="0.36" cy="0.3" r="0.78">
          <stop offset="0" stopColor="#FFF6CF" />
          <stop offset="0.3" stopColor="#FFD863" />
          <stop offset="0.72" stopColor="#F0A92A" />
          <stop offset="1" stopColor="#C27910" />
        </radialGradient>
        <radialGradient id={`${id}-core`} cx="0.45" cy="0.4" r="0.6">
          <stop offset="0" stopColor="#FFEFA8" stopOpacity="0.9" />
          <stop offset="1" stopColor="#FFD863" stopOpacity="0" />
        </radialGradient>
        <filter id={`${id}-shadow`} x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="0" dy="2.2" stdDeviation="1.6" floodColor="#A45F00" floodOpacity="0.35" />
        </filter>
        <filter id={`${id}-blur`}>
          <feGaussianBlur stdDeviation="1.1" />
        </filter>
      </defs>
      <polygon
        points={starPoints(24, 25.5, 17.5, 9.4)}
        fill={`url(#${id}-gold)`}
        stroke={`url(#${id}-gold)`}
        strokeWidth="7"
        strokeLinejoin="round"
        filter={`url(#${id}-shadow)`}
      />
      <polygon points={starPoints(24, 25, 10, 5.4)} fill={`url(#${id}-core)`} filter={`url(#${id}-blur)`} />
      <ellipse cx="17.5" cy="15.5" rx="5.5" ry="2.6" fill="#FFFFFF" opacity="0.75" transform="rotate(-32 17.5 15.5)" filter={`url(#${id}-blur)`} />
      <circle cx="31" cy="12" r="1.2" fill="#FFFFFF" opacity="0.8" />
    </svg>
  );
}

export function LogoMark() {
  return (
    <svg width="52" height="34" viewBox="0 0 52 34" aria-hidden="true">
      <defs>
        <linearGradient id="logo-fill" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#3B8BFF" />
          <stop offset="1" stopColor="#1452D9" />
        </linearGradient>
      </defs>
      <path fill="url(#logo-fill)" d="M3 32 17 3h31l-5.5 10H24L15 32z" />
      <path fill="#1452D9" d="M22 32 30 17h20l-8 15z" />
    </svg>
  );
}
