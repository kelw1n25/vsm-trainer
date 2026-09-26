import { useId } from "react";

/** Аватар текущего пользователя: проводник — манекен в форме, как в сценах новеллы. */
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
          <stop offset="0" stopColor="#FAE6D6" />
          <stop offset="0.45" stopColor="#E9BE9C" />
          <stop offset="1" stopColor="#B98463" />
        </radialGradient>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#6A7694" />
          <stop offset="0.45" stopColor="#2A3B66" />
          <stop offset="1" stopColor="#1C2845" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect width="64" height="64" fill={`url(#${id}-bg)`} />
        <path d="M10 64c1-13 9-19 22-19s21 6 22 19z" fill={`url(#${id}-body)`} />
        <path d="M25 46l7 10 7-10z" fill="#F4F6FA" />
        <path d="M31 49h2l1.5 9-2.5 3-2.5-3z" fill="#D23A3A" />
        <rect x="28" y="37" width="8" height="10" rx="4" fill="#E9BE9C" />
        <circle cx="32" cy="28" r="13" fill={`url(#${id}-head)`} />
        <circle cx="27.6" cy="29" r="1.4" fill="#3A2A22" />
        <circle cx="36.4" cy="29" r="1.4" fill="#3A2A22" />
        <path d="M28.3 34q3.7 2.6 7.4 0" stroke="#3A2A22" strokeWidth="1.3" strokeLinecap="round" fill="none" />
        <path d="M18.5 21Q32 5 45.5 21Z" fill="#2A3B66" />
        <rect x="19" y="18.5" width="26" height="2.8" rx="1" fill="#E7C15A" />
        <path d="M18.5 21.5H49" stroke="#18233F" strokeWidth="2.8" strokeLinecap="round" />
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
