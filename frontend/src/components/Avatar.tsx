import { useId } from "react";

/** Аватар текущего пользователя: проводник — серый манекен с акцентами формы, как в сценах новеллы. */
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
        <radialGradient id={`${id}-head`} cx="0.42" cy="0.4" r="0.72">
          <stop offset="0" stopColor="#C9C9C9" />
          <stop offset="0.62" stopColor="#C9C9C9" />
          <stop offset="1" stopColor="#A4A4A4" />
        </radialGradient>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#C9C9C9" />
          <stop offset="0.55" stopColor="#C9C9C9" />
          <stop offset="1" stopColor="#A4A4A4" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect width="64" height="64" fill={`url(#${id}-bg)`} />
        <path d="M11 64c1-12 9-18 21-18s20 6 21 18z" fill={`url(#${id}-body)`} />
        <path d="M31 47h2l1.5 10-2.5 3-2.5-3z" fill="#D23A3A" />
        <rect x="38" y="52" width="6" height="4" rx="1" fill="#E7C15A" />
        <rect x="28.5" y="37" width="7" height="10" rx="3.5" fill="#C9C9C9" />
        <circle cx="32" cy="27" r="14" fill={`url(#${id}-head)`} />
        {/* Фуражка анфас: тулья, околыш с кантом, кокарда и симметричный козырёк */}
        <path d="M18.6 19.6C15 15.4 20.4 6.6 32 6.2C43.6 6.6 49 15.4 45.4 19.6Z" fill="#34487A" />
        <rect x="18.4" y="17.4" width="27.2" height="4" rx="1.3" fill="#1E2848" />
        <path d="M18.6 17.6H45.4" stroke="#E7C15A" strokeWidth="0.8" />
        <ellipse cx="32" cy="16" rx="2.4" ry="2.7" fill="#E7C15A" />
        <ellipse cx="32" cy="16" rx="1" ry="1.2" fill="#C9483E" />
        <path d="M17.8 21.2C25 23 39 23 46.2 21.2C44.6 25 39 26 32 26C25 26 19.4 25 17.8 21.2Z" fill="#141B30" />
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
