import { useId } from "react";

/** Иллюстрированный аватар текущего пользователя: проводник в форме. */
export function Avatar({ size = 64 }: { size?: number }) {
  const id = useId().replace(/:/g, "");
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true" className="avatar">
      <defs>
        <clipPath id={`${id}-clip`}>
          <circle cx="32" cy="32" r="32" />
        </clipPath>
        <linearGradient id={`${id}-bg`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#EAF2FF" />
          <stop offset="1" stopColor="#C9DDFB" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect width="64" height="64" fill={`url(#${id}-bg)`} />
        <path d="M8 64c1-12 10-18 24-18s23 6 24 18z" fill="#1C2B55" />
        <path d="M25 46l7 10 7-10z" fill="#FFFFFF" />
        <path d="M31 49h2l1.5 9-2.5 3-2.5-3z" fill="#D8343C" />
        <rect x="27.5" y="36" width="9" height="10" rx="3" fill="#EDBE98" />
        <ellipse cx="32" cy="28" rx="10.5" ry="12" fill="#F3CCAA" />
        <path d="M21 27c-1-11 6-16 12-16 7 0 12 5 11 15-2-5-6-8-12-8-5 0-9 3-11 9z" fill="#2A2320" />
        <path d="M25.5 25.5l4-.8M38.5 25.5l-4-.8" stroke="#2A2320" strokeWidth="1.2" strokeLinecap="round" />
        <circle cx="28.3" cy="29" r="1.3" fill="#2A2320" />
        <circle cx="35.7" cy="29" r="1.3" fill="#2A2320" />
        <path d="M28.5 34.5q3.5 2.6 7 0" stroke="#8A4B3A" strokeWidth="1.3" strokeLinecap="round" fill="none" />
      </g>
    </svg>
  );
}

/** Аватар-инициалы для других сотрудников в списках. */
export function InitialsAvatar({ name, size = 40 }: { name: string; size?: number }) {
  const [last = "", first = ""] = name.split(" ");
  return (
    <span className="initials" style={{ width: size, height: size, fontSize: size * 0.38 }} aria-hidden="true">
      {(first[0] ?? "") + (last[0] ?? "")}
    </span>
  );
}
