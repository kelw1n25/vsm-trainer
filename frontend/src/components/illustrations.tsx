import { useId, type ReactNode } from "react";

// Векторные иллюстрации в стиле интерфейса: реальных фото в проекте нет,
// а чужие изображения в репозиторий класть нельзя.

function useSvgId(): string {
  return useId().replace(/:/g, "");
}

/** Скоростной поезд на фоне города — правая часть hero-блока. */
export function HeroTrain() {
  const id = useSvgId();
  const bandTop = (x: number) => 170 - ((x - 440) * 38) / 560;
  return (
    <svg className="hero__train" viewBox="0 0 1000 300" preserveAspectRatio="xMaxYMax meet" aria-hidden="true">
      <defs>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="0.7" stopColor="#EEF3FA" />
          <stop offset="1" stopColor="#CBD8EA" />
        </linearGradient>
        <linearGradient id={`${id}-glass`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#2B4677" />
          <stop offset="1" stopColor="#13213F" />
        </linearGradient>
        <linearGradient id={`${id}-city`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#CFE0F7" stopOpacity="0.9" />
          <stop offset="1" stopColor="#E4EEFB" stopOpacity="0.2" />
        </linearGradient>
        <linearGradient id={`${id}-fade`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity="0" />
          <stop offset="1" stopColor="#FFFFFF" stopOpacity="0.9" />
        </linearGradient>
      </defs>

      <g fill={`url(#${id}-city)`}>
        <rect x="520" y="120" width="34" height="150" />
        <rect x="562" y="80" width="42" height="190" />
        <path d="M618 270V40l10-22 10 22v230z" />
        <rect x="650" y="100" width="46" height="170" />
        <rect x="706" y="60" width="30" height="210" />
        <rect x="746" y="120" width="44" height="150" />
        <rect x="800" y="40" width="36" height="230" />
        <rect x="846" y="96" width="54" height="174" />
        <rect x="910" y="70" width="40" height="200" />
        <rect x="958" y="110" width="42" height="160" />
      </g>

      <g stroke={`url(#${id}-fade)`} strokeWidth="3" strokeLinecap="round">
        <path d="M120 208H420" />
        <path d="M60 236H380" />
        <path d="M180 262H500" />
        <path d="M260 184H430" />
      </g>

      <ellipse cx="640" cy="282" rx="420" ry="12" fill="#9DB6D8" opacity="0.35" />
      <path d="M150 284L1000 268" stroke="#C3D2E6" strokeWidth="3" />
      <path d="M220 294L1000 282" stroke="#D3DFEE" strokeWidth="2" />

      <path
        d="M230 222C236 196 272 170 340 156L520 128C600 116 700 104 1000 88V250L290 262C250 262 226 246 230 222Z"
        fill={`url(#${id}-body)`}
      />
      <path d="M340 156L520 128C600 116 700 104 1000 88V96C700 112 600 124 520 136L350 162Z" fill="#FFFFFF" />
      <path d="M300 184C322 166 360 156 408 150L404 176C370 180 332 186 300 184Z" fill={`url(#${id}-glass)`} />
      <path d="M440 170L1000 132V156L440 192Z" fill={`url(#${id}-glass)`} opacity="0.92" />
      <g stroke="#DCE6F4" strokeWidth="2" opacity="0.8">
        {[500, 560, 620, 680, 740, 800, 860, 920, 980].map((x) => (
          <path key={x} d={`M${x} ${bandTop(x)}V${bandTop(x) + 22}`} />
        ))}
      </g>
      <path d="M262 236L1000 196V204L268 244Z" fill="#E1343C" />
      <path d="M290 250L1000 214V220L300 256Z" fill="#2A5FD8" opacity="0.8" />
      <path d="M290 262L1000 250V262L320 270Z" fill="#9FB0C8" />
      <ellipse cx="254" cy="226" rx="10" ry="4" fill="#FFFFFF" />
    </svg>
  );
}

function Frame({ children, top, bottom }: { children: ReactNode; top: string; bottom: string }) {
  const id = useSvgId();
  return (
    // slice: на узких экранах картинка заполняет всю рамку, а не сжимается с полями по бокам
    <svg className="scenario-image" viewBox="0 0 160 150" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <linearGradient id={`${id}-bg`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={top} />
          <stop offset="1" stopColor={bottom} />
        </linearGradient>
      </defs>
      <rect width="160" height="150" fill={`url(#${id}-bg)`} />
      {children}
    </svg>
  );
}

const windows = (
  <g fill="#F9FCFF" stroke="#C6D6EC" strokeWidth="2">
    <rect x="10" y="12" width="44" height="40" rx="8" />
    <rect x="64" y="10" width="48" height="42" rx="8" />
    <rect x="122" y="8" width="44" height="44" rx="8" />
  </g>
);

/** Салон бизнес-класса. */
function BusinessSeats() {
  return (
    <Frame top="#EEF4FC" bottom="#D2E1F4">
      {windows}
      <rect y="58" width="160" height="92" fill="#E3ECF7" />
      <path d="M0 128L160 116V150H0Z" fill="#C5D3E6" />
      <rect x="22" y="62" width="30" height="52" rx="9" fill="#3563C9" />
      <rect x="25" y="59" width="24" height="10" rx="4" fill="#F4F7FB" />
      <rect x="18" y="106" width="40" height="14" rx="6" fill="#274EA8" />
      <rect x="62" y="96" width="16" height="30" rx="3" fill="#F3F1EC" />
      <circle cx="70" cy="92" r="5" fill="#FFE7B0" />
      <rect x="86" y="52" width="46" height="74" rx="12" fill="#2A57C0" />
      <rect x="92" y="47" width="34" height="14" rx="6" fill="#F4F7FB" />
      <rect x="80" y="116" width="60" height="20" rx="8" fill="#1F459D" />
      <rect x="136" y="98" width="14" height="32" rx="5" fill="#1B3D8C" />
    </Frame>
  );
}

/** Проводник и пассажир в вагоне. */
function ConductorAndPassenger() {
  return (
    <Frame top="#E9F1FB" bottom="#D3E2F4">
      <rect x="8" y="10" width="64" height="52" rx="10" fill="#F8FBFF" stroke="#C6D6EC" strokeWidth="2" />
      <rect x="14" y="70" width="46" height="60" rx="10" fill="#2E5CC4" />
      <rect x="10" y="118" width="62" height="20" rx="6" fill="#244DA8" />
      <path d="M24 134C24 106 34 94 50 94S74 106 74 134Z" fill="#22315A" />
      <circle cx="50" cy="80" r="13" fill="#F0C4A0" />
      <path d="M37 78C37 66 63 64 63 76 58 70 44 70 37 78Z" fill="#3A2E2A" />
      <path d="M64 102C76 94 72 78 60 72" stroke="#22315A" strokeWidth="7" strokeLinecap="round" fill="none" />
      <path d="M102 150L104 88C104 78 112 72 122 72S140 78 140 88L142 150Z" fill="#1F2E57" />
      <path d="M114 74L122 92 130 74Z" fill="#FFFFFF" />
      <path d="M121 80h2l2 16-3 4-3-4Z" fill="#D23A3A" />
      <circle cx="122" cy="58" r="14" fill="#F2C9A5" />
      <path d="M108 56C108 42 136 40 136 54 130 48 116 48 108 56Z" fill="#2B2320" />
      <path d="M140 92C150 104 148 118 142 126" stroke="#1F2E57" strokeWidth="9" strokeLinecap="round" fill="none" />
    </Frame>
  );
}

/** Сотрудник оказывает помощь пассажиру. */
function MedicalHelp() {
  return (
    <Frame top="#EAF2FC" bottom="#D0E0F4">
      <rect x="92" y="8" width="62" height="54" rx="10" fill="#F8FBFF" stroke="#C6D6EC" strokeWidth="2" />
      <rect x="6" y="106" width="150" height="26" rx="8" fill="#2E5CC4" />
      <rect x="108" y="92" width="42" height="16" rx="8" fill="#FFFFFF" />
      <path d="M22 112C42 98 92 98 118 106V118L22 120Z" fill="#9CC0EE" />
      <circle cx="126" cy="96" r="12" fill="#F0C4A0" />
      <path d="M116 92C118 82 136 82 138 92 132 88 122 88 116 92Z" fill="#4A3426" />
      <path d="M34 150C30 118 44 78 70 72 92 68 98 86 94 106L84 150Z" fill="#213463" />
      <path d="M88 90C100 96 108 100 114 106" stroke="#FFFFFF" strokeWidth="8" strokeLinecap="round" fill="none" />
      <circle cx="116" cy="107" r="5" fill="#F2C9A5" />
      <path d="M62 70l10 8 10-8" stroke="#D8343C" strokeWidth="4" fill="none" strokeLinecap="round" />
      <circle cx="72" cy="52" r="13" fill="#F2C9A5" />
      <circle cx="61" cy="43" r="8" fill="#6B3F2A" />
      <path d="M59 52C59 38 85 36 85 50 79 44 66 44 59 52Z" fill="#6B3F2A" />
    </Frame>
  );
}

/** Оставленная сумка на сиденье. */
function LeftBag() {
  return (
    <Frame top="#6A8FDC" bottom="#243F8F">
      <rect x="28" y="6" width="104" height="44" rx="10" fill="#EEF4FE" opacity="0.95" />
      <rect x="40" y="38" width="80" height="88" rx="16" fill="#2B56BF" />
      <rect x="52" y="30" width="56" height="18" rx="9" fill="#3A68D2" />
      <rect x="28" y="112" width="104" height="28" rx="10" fill="#1E428F" />
      <path d="M72 64C72 52 92 52 92 64" stroke="#1B1F2A" strokeWidth="5" fill="none" />
      <rect x="60" y="62" width="44" height="56" rx="13" fill="#1B1F2A" />
      <rect x="67" y="92" width="30" height="20" rx="6" fill="#2A2F3C" />
      <path d="M68 78H96" stroke="#3A4150" strokeWidth="2" />
    </Frame>
  );
}

/** Табло задержки и поезд на платформе. */
function DelayBoard() {
  return (
    <Frame top="#EAF1FB" bottom="#CFDFF3">
      <g stroke="#B9CBE3" strokeWidth="2">
        <path d="M0 84L80 70 160 84" fill="none" />
        <path d="M20 80V96M140 80V96" />
      </g>
      <rect x="40" y="62" width="4" height="12" fill="#6E7F99" />
      <rect x="116" y="62" width="4" height="12" fill="#6E7F99" />
      <rect x="16" y="8" width="128" height="56" rx="8" fill="#17223A" />
      <text x="26" y="22" fontSize="9" fill="#9FB0CC" fontFamily="Manrope, sans-serif">
        15:43
      </text>
      <text x="80" y="50" fontSize="19" fontWeight="800" fill="#FF8A3D" textAnchor="middle" fontFamily="Manrope, sans-serif">
        Задержка
      </text>
      <rect y="98" width="160" height="38" fill="#F5F8FC" />
      <rect y="104" width="160" height="12" fill="#2B3F66" />
      <g stroke="#DCE6F4" strokeWidth="2">
        {[20, 44, 68, 92, 116, 140].map((x) => (
          <path key={x} d={`M${x} 104V116`} />
        ))}
      </g>
      <rect y="122" width="160" height="3" fill="#E1343C" />
      <rect y="127" width="160" height="3" fill="#2A5FD8" />
      <rect y="136" width="160" height="14" fill="#BFCDE0" />
    </Frame>
  );
}

const byScenario: Record<string, () => ReactNode> = {
  "business-seat-conflict": BusinessSeats,
  "drunk-passenger": ConductorAndPassenger,
  "medical-heart-attack": MedicalHelp,
  "suspicious-item": LeftBag,
  "train-delay-compensation": DelayBoard,
};

// Для новых сценариев без своей картинки — иллюстрация по категории
const byCategory: Record<string, () => ReactNode> = {
  conflict: ConductorAndPassenger,
  medical: MedicalHelp,
  safety: LeftBag,
  service: DelayBoard,
};

export function ScenarioImage({ scenarioId, category }: { scenarioId: string; category: string }) {
  const Image = byScenario[scenarioId] ?? byCategory[category] ?? BusinessSeats;
  return <Image />;
}
