import { useId } from "react";

/** Аватар текущего пользователя: проводник — красная фигурка-манекен, как в сценах новеллы. */
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
        <radialGradient id={`${id}-head`} cx="0.36" cy="0.3" r="0.75">
          <stop offset="0" stopColor="#F7C6C8" />
          <stop offset="0.45" stopColor="#D8262E" />
          <stop offset="1" stopColor="#931A1F" />
        </radialGradient>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#EC9396" />
          <stop offset="0.45" stopColor="#D8262E" />
          <stop offset="1" stopColor="#931A1F" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect width="64" height="64" fill={`url(#${id}-bg)`} />
        <path d="M10 64c1-13 9-19 22-19s21 6 22 19z" fill={`url(#${id}-body)`} />
        <rect x="28" y="37" width="8" height="9" rx="4" fill="#D8262E" />
        <circle cx="32" cy="27" r="13" fill={`url(#${id}-head)`} />
        <circle cx="27.6" cy="27.5" r="1.4" fill="#5A1014" />
        <circle cx="36.4" cy="27.5" r="1.4" fill="#5A1014" />
        <path d="M28.3 32.5q3.7 2.6 7.4 0" stroke="#5A1014" strokeWidth="1.3" strokeLinecap="round" fill="none" />
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
