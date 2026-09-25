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

export function GridIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true" {...common}>
      <rect x="4" y="4" width="6.5" height="6.5" rx="1.8" />
      <rect x="13.5" y="4" width="6.5" height="6.5" rx="1.8" />
      <rect x="4" y="13.5" width="6.5" height="6.5" rx="1.8" />
      <path d="M13.5 16.75h6.5M16.75 13.5v6.5" />
    </svg>
  );
}

export function StarIcon({ size = 46 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
      <defs>
        <linearGradient id="star-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFD85A" />
          <stop offset="1" stopColor="#F5A623" />
        </linearGradient>
      </defs>
      <path
        fill="url(#star-fill)"
        stroke="#E8961A"
        strokeWidth="1.2"
        strokeLinejoin="round"
        d="M24 4.5l5.9 12 13.2 1.9-9.6 9.3 2.3 13.1L24 34.6l-11.8 6.2 2.3-13.1-9.6-9.3 13.2-1.9z"
      />
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
