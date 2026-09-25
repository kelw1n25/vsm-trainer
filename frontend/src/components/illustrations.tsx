import { useId, type ReactNode } from "react";

// Векторные иллюстрации в стиле интерфейса: реальных фото в проекте нет,
// а чужие изображения в репозиторий класть нельзя.

function useSvgId(): string {
  return useId().replace(/:/g, "");
}

// Геометрия поезда в перспективе: корпус уходит вправо и вверх
const bandTop = (x: number) => 170 - (x - 440) * 0.07;
const bandHeight = (x: number) => 21 - (x - 440) * 0.006;
const WINDOW_SLOTS = Array.from({ length: 17 }, (_, i) => 468 + i * 46);

function windowPath(x: number, width: number): string {
  const x2 = x + width;
  return `M${x} ${bandTop(x)}L${x2} ${bandTop(x2)}L${x2} ${bandTop(x2) + bandHeight(x2)}L${x} ${bandTop(x) + bandHeight(x)}Z`;
}

interface HeroTrainProps {
  /** Насколько поезд проехал вперёд (в единицах viewBox) — меняется при смене слайда. */
  offset?: number;
  moving?: boolean;
}

/** Скоростной поезд на фоне города — правая часть hero-блока. */
export function HeroTrain({ offset = 0, moving = false }: HeroTrainProps) {
  const id = useSvgId();
  return (
    <svg
      className={`hero__train ${moving ? "hero__train--moving" : ""}`}
      viewBox="0 0 1000 300"
      preserveAspectRatio="xMaxYMax meet"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="0.55" stopColor="#F1F5FB" />
          <stop offset="1" stopColor="#C9D6E8" />
        </linearGradient>
        <linearGradient id={`${id}-nose`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#E3EAF4" />
          <stop offset="0.35" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
        <linearGradient id={`${id}-glass`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#3A5B92" />
          <stop offset="0.6" stopColor="#1A2B4E" />
          <stop offset="1" stopColor="#0E1830" />
        </linearGradient>
        <linearGradient id={`${id}-skirt`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#B7C5D9" />
          <stop offset="1" stopColor="#8C9DB6" />
        </linearGradient>
        <linearGradient id={`${id}-city`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#CADCF5" stopOpacity="0.95" />
          <stop offset="1" stopColor="#E4EEFB" stopOpacity="0.15" />
        </linearGradient>
        <linearGradient id={`${id}-far`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#DDE8F8" stopOpacity="0.9" />
          <stop offset="1" stopColor="#EAF1FB" stopOpacity="0.1" />
        </linearGradient>
        <linearGradient id={`${id}-wind`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity="0" />
          <stop offset="1" stopColor="#FFFFFF" stopOpacity="0.95" />
        </linearGradient>
        <radialGradient id={`${id}-glow`}>
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="0.4" stopColor="#FFF6D8" stopOpacity="0.7" />
          <stop offset="1" stopColor="#FFF6D8" stopOpacity="0" />
        </radialGradient>
        <filter id={`${id}-soft`} x="-20%" y="-50%" width="140%" height="200%">
          <feGaussianBlur stdDeviation="6" />
        </filter>
      </defs>

      {/* Дальний план: облака и город двигаются медленнее поезда — эффект глубины */}
      <g className="hero__parallax" style={{ transform: `translateX(${-offset * 0.35}px)` }}>
        <g className="hero__clouds" fill="#FFFFFF" opacity="0.7">
          <ellipse cx="560" cy="44" rx="60" ry="14" />
          <ellipse cx="610" cy="36" rx="38" ry="12" />
          <ellipse cx="880" cy="58" rx="70" ry="13" />
          <ellipse cx="930" cy="50" rx="36" ry="11" />
        </g>
        <g fill={`url(#${id}-far)`}>
          <rect x="480" y="140" width="40" height="130" />
          <rect x="690" y="120" width="34" height="150" />
          <rect x="880" y="130" width="46" height="140" />
          <rect x="1010" y="100" width="40" height="170" />
        </g>
        <g fill={`url(#${id}-city)`}>
          <rect x="520" y="120" width="34" height="150" />
          <rect x="562" y="80" width="42" height="190" />
          <path d="M618 270V40l10-22 10 22v230z" />
          <rect x="650" y="100" width="46" height="170" />
          <rect x="706" y="60" width="30" height="210" />
          <rect x="746" y="120" width="44" height="150" />
          <rect x="800" y="40" width="36" height="230" />
          <path d="M800 40l18-14 18 14z" />
          <rect x="846" y="96" width="54" height="174" />
          <rect x="910" y="70" width="40" height="200" />
          <rect x="958" y="110" width="42" height="160" />
          <rect x="1004" y="84" width="48" height="186" />
          <rect x="1060" y="126" width="40" height="144" />
        </g>
        <g fill="#FFFFFF" opacity="0.55">
          {[572, 584, 660, 672, 684, 810, 822, 856, 868, 880, 920, 932].map((x, i) => (
            <rect key={x} x={x} y={110 + (i % 4) * 22} width="6" height="9" rx="1" />
          ))}
        </g>
      </g>

      {/* Контактная сеть и опоры стоят на месте — поезд проезжает мимо них */}
      <g stroke="#C4D2E6" strokeLinecap="round">
        <path d="M0 74L1000 66" strokeWidth="1.5" />
        <path d="M0 82L1000 74" strokeWidth="1" opacity="0.6" />
        <path d="M150 58V282M150 70H182" strokeWidth="4" />
        <path d="M560 52V272M560 64H592" strokeWidth="4" opacity="0.8" />
      </g>

      <ellipse cx="700" cy="284" rx="520" ry="10" fill="#7E97BD" opacity="0.28" filter={`url(#${id}-soft)`} />
      <path d="M100 288L1000 272" stroke="#C3D2E6" strokeWidth="3" />
      <path d="M200 298L1000 284" stroke="#D3DFEE" strokeWidth="2" />
      <g className="hero__sleepers" stroke="#D8E2EF" strokeWidth="3">
        {Array.from({ length: 18 }, (_, i) => 120 + i * 50).map((x) => (
          <path key={x} d={`M${x} ${291 - (x - 100) * 0.0178}l-14 8`} />
        ))}
      </g>

      <g className="hero__speed" stroke={`url(#${id}-wind)`} strokeLinecap="round">
        <path d="M40 206H470" strokeWidth="3" />
        <path d="M0 232H420" strokeWidth="2.5" />
        <path d="M120 258H520" strokeWidth="3" />
        <path d="M200 184H440" strokeWidth="2" />
        <path d="M80 150H330" strokeWidth="1.5" />
      </g>

      <g className="hero__train-drive" style={{ transform: `translateX(${-offset}px)` }}>
        <g className="hero__train-bob">
          {/* Тележки с колёсами — под юбкой корпуса */}
          <g fill="#3C4B66">
            <rect x="390" y="250" width="76" height="18" rx="6" />
            <rect x="880" y="242" width="72" height="17" rx="6" />
          </g>
          <g fill="#243047" className="hero__wheels">
            <circle cx="406" cy="268" r="7" />
            <circle cx="450" cy="267" r="7" />
            <circle cx="895" cy="259" r="6.5" />
            <circle cx="937" cy="258" r="6.5" />
          </g>

          <path
            d="M225 232C228 205 262 176 335 160L520 128L1250 67.5V246L300 264C255 265 223 254 225 232Z"
            fill={`url(#${id}-body)`}
          />
          <path
            d="M225 232C228 205 262 176 335 160L520 128L1250 67.5V246L300 264C255 265 223 254 225 232Z"
            fill={`url(#${id}-nose)`}
          />
          <path d="M335 160L520 128L1250 67.5V75L520 136L345 166Z" fill="#FFFFFF" />
          <path d="M240 246C262 256 290 258 300 258L1250 238V246L300 264C268 264 248 258 240 246Z" fill={`url(#${id}-skirt)`} />

          {/* Пантограф на крыше касается провода */}
          <g stroke="#6E819E" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round">
            <path d="M748 108L772 88L794 74" />
            <path d="M784 106L770 88" />
            <path d="M778 73L814 70" strokeWidth="3" />
          </g>
          <rect x="740" y="104" width="50" height="6" rx="3" fill="#8CA0BE" transform="rotate(-4.7 765 107)" />

          <path d="M292 190C312 170 352 158 404 151L398 181C360 185 322 191 292 190Z" fill={`url(#${id}-glass)`} />
          <path d="M312 182C330 170 356 163 384 159" stroke="#FFFFFF" strokeWidth="3" strokeLinecap="round" opacity="0.45" fill="none" />
          <path d="M252 214C272 196 302 184 332 178" stroke="#D5DFEC" strokeWidth="1.5" fill="none" />

          <path d="M440 170L1250 113V132L440 192Z" fill="#1C2C4D" opacity="0.08" />
          {WINDOW_SLOTS.map((x, i) =>
            i % 6 === 5 ? (
              // Дверь вагона вместо окна
              <path
                key={x}
                d={`M${x + 4} ${bandTop(x + 4) - 5}V${236 - (x - 262) * 0.056}M${x + 30} ${bandTop(x + 30) - 5}V${236 - (x + 26 - 262) * 0.056}`}
                stroke="#C9D6E8"
                strokeWidth="2"
              />
            ) : (
              <path key={x} d={windowPath(x, 36)} fill={`url(#${id}-glass)`} />
            ),
          )}

          <text
            x="440"
            y="216"
            transform="rotate(-3.2 440 216)"
            fontFamily="Manrope, sans-serif"
            fontWeight="800"
            fontSize="17"
            letterSpacing="1.5"
            fill="#1E5FD6"
          >
            ВСМ
          </text>

          <path d="M250 240L1250 184V192L256 248Z" fill="#E1343C" />
          <path d="M282 252L1250 202V208L292 258Z" fill="#2A5FD8" opacity="0.85" />

          <ellipse className="hero__headlight-glow" cx="244" cy="230" rx="30" ry="14" fill={`url(#${id}-glow)`} />
          <ellipse cx="244" cy="230" rx="9" ry="4" fill="#FFFFFF" />
        </g>
      </g>
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

/** Окно вагона с пейзажем, пролетающим на скорости. */
function WindowView({ x, y, width, height }: { x: number; y: number; width: number; height: number }) {
  const id = useSvgId();
  return (
    <g>
      <defs>
        <clipPath id={`${id}-clip`}>
          <rect x={x} y={y} width={width} height={height} rx="9" />
        </clipPath>
        <linearGradient id={`${id}-sky`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#CFE3FF" />
          <stop offset="1" stopColor="#F4F9FF" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect x={x} y={y} width={width} height={height} fill={`url(#${id}-sky)`} />
        <path
          d={`M${x} ${y + height * 0.72}q${width * 0.25} -${height * 0.22} ${width * 0.5} -${height * 0.06}t${width * 0.5} -${height * 0.08}V${y + height}H${x}Z`}
          fill="#B9D3C6"
        />
        <g stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" opacity="0.9">
          <path d={`M${x + 4} ${y + height * 0.35}h${width * 0.45}`} />
          <path d={`M${x + width * 0.3} ${y + height * 0.52}h${width * 0.55}`} />
        </g>
      </g>
      <rect x={x} y={y} width={width} height={height} rx="9" fill="none" stroke="#C6D6EC" strokeWidth="2" />
    </g>
  );
}

/** Конфликт из-за места: пассажир с билетом и пассажирка, занявшая его кресло 5А. */
function SeatConflict() {
  return (
    <Frame top="#EFF5FD" bottom="#D6E4F5">
      <rect width="160" height="9" fill="#C9D6E8" />
      <rect y="9" width="160" height="3" fill="#B3C4DB" />
      <WindowView x={92} y={18} width={62} height={42} />
      <path d="M0 132L160 124V150H0Z" fill="#C5D3E6" />
      <path d="M0 140L160 133" stroke="#B2C3DA" strokeWidth="2" />

      <rect x="118" y="62" width="24" height="12" rx="3" fill="#1E5FD6" />
      <text x="130" y="71" textAnchor="middle" fontSize="8" fontWeight="800" fill="#FFFFFF" fontFamily="Manrope, sans-serif">
        5А
      </text>
      <rect x="98" y="72" width="50" height="62" rx="12" fill="#2A57C0" />
      <rect x="104" y="68" width="38" height="12" rx="5" fill="#F4F7FB" />
      <rect x="146" y="102" width="12" height="30" rx="5" fill="#1B3D8C" />

      <path d="M104 126C104 104 112 94 124 94S142 104 142 126Z" fill="#E8735A" />
      <path d="M109 110H139" stroke="#C85A44" strokeWidth="7" strokeLinecap="round" />
      <circle cx="124" cy="84" r="10" fill="#F2C9A5" />
      <path d="M113 86C112 72 136 70 135 86 133 78 116 78 113 86Z" fill="#6B3F2A" />
      <path d="M113 86C112 94 114 98 117 99" stroke="#6B3F2A" strokeWidth="4" strokeLinecap="round" fill="none" />
      <rect x="92" y="122" width="62" height="18" rx="8" fill="#1F459D" />

      <path d="M22 150L24 92C24 80 32 74 42 74S60 80 60 92L62 150Z" fill="#56657F" />
      <path d="M36 76L42 92 48 76Z" fill="#FFFFFF" />
      <circle cx="42" cy="60" r="12" fill="#F0C4A0" />
      <path d="M30 58C30 45 54 44 54 56 49 50 36 50 30 58Z" fill="#2E2622" />
      <path d="M60 94C70 92 78 88 84 84" stroke="#56657F" strokeWidth="8" strokeLinecap="round" fill="none" />
      <rect x="80" y="76" width="14" height="10" rx="2" fill="#FFFFFF" stroke="#9AA9C0" transform="rotate(-12 87 81)" />
      <path d="M83 80h8M83 83h5" stroke="#9AA9C0" strokeWidth="1" transform="rotate(-12 87 81)" />

      <g stroke="#E1343C" strokeWidth="2" strokeLinecap="round">
        <path d="M58 44l4-5M62 50l6-2M56 38l1-6" />
      </g>
      <path d="M70 22h22a6 6 0 0 1 6 6v8a6 6 0 0 1-6 6H80l-6 6v-6h-4a6 6 0 0 1-6-6v-8a6 6 0 0 1 6-6z" fill="#FFFFFF" />
      <text x="81" y="37" textAnchor="middle" fontSize="13" fontWeight="800" fill="#E1343C" fontFamily="Manrope, sans-serif">
        !
      </text>
    </Frame>
  );
}

/** Проводник и пассажир в вагоне. */
function ConductorAndPassenger() {
  return (
    <Frame top="#E9F1FB" bottom="#D3E2F4">
      <WindowView x={8} y={10} width={64} height={52} />
      <rect x="14" y="70" width="46" height="60" rx="10" fill="#2E5CC4" />
      <rect x="10" y="118" width="62" height="20" rx="6" fill="#244DA8" />
      <path d="M24 134C24 106 34 94 50 94S74 106 74 134Z" fill="#22315A" />
      <circle cx="50" cy="80" r="13" fill="#F0C4A0" />
      <path d="M37 78C37 66 63 64 63 76 58 70 44 70 37 78Z" fill="#3A2E2A" />
      <path d="M64 102C76 94 72 78 60 72" stroke="#22315A" strokeWidth="7" strokeLinecap="round" fill="none" />
      <rect x="76" y="112" width="8" height="18" rx="3" fill="#7BA05B" opacity="0.9" />
      <path d="M102 150L104 88C104 78 112 72 122 72S140 78 140 88L142 150Z" fill="#1F2E57" />
      <path d="M114 74L122 92 130 74Z" fill="#FFFFFF" />
      <path d="M121 80h2l2 16-3 4-3-4Z" fill="#D23A3A" />
      <rect x="106" y="98" width="10" height="7" rx="1.5" fill="#E7C15A" />
      <circle cx="122" cy="58" r="14" fill="#F2C9A5" />
      <path d="M108 56C108 42 136 40 136 54 130 48 116 48 108 56Z" fill="#2B2320" />
      <path d="M108 50H136V46C136 40 108 40 108 46Z" fill="#1F2E57" />
      <path d="M140 92C150 104 148 118 142 126" stroke="#1F2E57" strokeWidth="9" strokeLinecap="round" fill="none" />
    </Frame>
  );
}

/** Сотрудник оказывает помощь пассажиру. */
function MedicalHelp() {
  return (
    <Frame top="#EAF2FC" bottom="#D0E0F4">
      <WindowView x={92} y={8} width={62} height={54} />
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
      <rect x="12" y="84" width="22" height="16" rx="3" fill="#FFFFFF" stroke="#D8343C" strokeWidth="1.5" />
      <path d="M23 88v8M19 92h8" stroke="#D8343C" strokeWidth="2.5" strokeLinecap="round" />
    </Frame>
  );
}

/** Оставленная сумка на сиденье. */
function LeftBag() {
  return (
    <Frame top="#6A8FDC" bottom="#243F8F">
      <WindowView x={28} y={6} width={104} height={40} />
      <rect x="40" y="38" width="80" height="88" rx="16" fill="#2B56BF" />
      <rect x="52" y="30" width="56" height="18" rx="9" fill="#3A68D2" />
      <rect x="28" y="112" width="104" height="28" rx="10" fill="#1E428F" />
      <ellipse cx="82" cy="118" rx="26" ry="4" fill="#0E1A3A" opacity="0.35" />
      <path d="M72 64C72 52 92 52 92 64" stroke="#1B1F2A" strokeWidth="5" fill="none" />
      <rect x="60" y="62" width="44" height="56" rx="13" fill="#1B1F2A" />
      <rect x="67" y="92" width="30" height="20" rx="6" fill="#2A2F3C" />
      <path d="M68 78H96" stroke="#3A4150" strokeWidth="2" />
      <path d="M130 58l12 22h-24z" fill="#FFC53D" stroke="#E0A000" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M130 66v7M130 76v1" stroke="#5A4000" strokeWidth="2.2" strokeLinecap="round" />
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
      <circle className="blink" cx="134" cy="18" r="3" fill="#FF8A3D" />
      <text x="26" y="22" fontSize="9" fill="#9FB0CC" fontFamily="Manrope, sans-serif">
        15:43 · ВСМ-400
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
      <g fill="#56657F">
        <circle cx="30" cy="84" r="4" />
        <path d="M24 98c0-6 3-9 6-9s6 3 6 9z" />
        <circle cx="128" cy="86" r="3.5" />
        <path d="M123 98c0-5 2-8 5-8s5 3 5 8z" />
      </g>
    </Frame>
  );
}

const byScenario: Record<string, () => ReactNode> = {
  "business-seat-conflict": SeatConflict,
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
  const Image = byScenario[scenarioId] ?? byCategory[category] ?? SeatConflict;
  return <Image />;
}
