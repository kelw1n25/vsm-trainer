import { useId, type ReactNode } from "react";

// Векторные иллюстрации в стиле интерфейса: поезд на главной, картинки сценариев и персонажи новеллы.
// Фото из датасета кейсодержателя используются как фоны сцен (см. story/StoryStage.tsx).

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
  /** Насколько поезд проехал вперёд (в единицах viewBox) — задаётся ползунком hero. */
  offset?: number;
  moving?: boolean;
  /** Короткий плавный переход (клавиатура); при перетаскивании поезд следует без задержки. */
  smooth?: boolean;
}

// Путь уходит вправо и вверх: головка ближнего рельса — railY(x), дальний рельс выше на FAR_RAIL_GAP
const RAIL_SLOPE = 0.0198;
const railY = (x: number) => 281 - (x - 100) * RAIL_SLOPE;
const FAR_RAIL_GAP = 7;
const SLEEPERS = Array.from({ length: 40 }, (_, i) => 60 + i * 24);

// Колёса стоят точно на головке рельса
const WHEELS = [
  { cx: 408, r: 8.5 },
  { cx: 448, r: 8.5 },
  { cx: 897, r: 8 },
  { cx: 935, r: 8 },
].map(({ cx, r }) => ({ cx, r, cy: railY(cx) - r }));

/** Рельс в разрезе: подошва, шейка и светлая головка. */
function Rail({ lift = 0, scale = 1 }: { lift?: number; scale?: number }) {
  const line = (dy: number) => `M40 ${railY(40) - lift + dy}L1000 ${railY(1000) - lift + dy}`;
  return (
    <g strokeLinecap="round" fill="none">
      <path d={line(4.2 * scale)} stroke="#7F91AC" strokeWidth={2.6 * scale} />
      <path d={line(2.2 * scale)} stroke="#98A8BF" strokeWidth={1.8 * scale} />
      <path d={line(0.6 * scale)} stroke="#D9E2EE" strokeWidth={2.4 * scale} />
      <path d={line(0)} stroke="#FFFFFF" strokeWidth={0.9 * scale} opacity="0.9" />
    </g>
  );
}

/** Скоростной поезд на фоне города — правая часть hero-блока. */
export function HeroTrain({ offset = 0, moving = false, smooth = false }: HeroTrainProps) {
  const id = useSvgId();
  return (
    <svg
      className={`hero__train ${moving ? "hero__train--moving" : ""} ${smooth ? "hero__train--smooth" : ""}`}
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
        <linearGradient id={`${id}-ballast`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#C3CFDF" stopOpacity="0.2" />
          <stop offset="0.45" stopColor="#B7C4D6" stopOpacity="0.75" />
          <stop offset="1" stopColor="#A9B8CC" stopOpacity="0.15" />
        </linearGradient>
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

      {/* Путь: щебёночная насыпь, шпалы с креплениями и два рельса */}
      <path
        d={`M40 ${railY(40) - FAR_RAIL_GAP - 5}L1000 ${railY(1000) - FAR_RAIL_GAP - 5}L1000 ${railY(1000) + 16}L40 ${railY(40) + 19}Z`}
        fill={`url(#${id}-ballast)`}
      />
      <ellipse cx="700" cy={railY(700) + 2} rx="520" ry="8" fill="#6F88AE" opacity="0.3" filter={`url(#${id}-soft)`} />
      <g fill="#A3B2C7" opacity="0.7">
        {SLEEPERS.map((x, i) => (
          <circle key={x} cx={x + 14 + (i % 3) * 5} cy={railY(x + 14) + 9 + (i % 2) * 4} r={1 + (i % 3) * 0.3} />
        ))}
      </g>
      {/* Шпала идёт поперёк пути: от дальнего рельса вниз-влево под ближний */}
      <g fill="#8E9FB6">
        {SLEEPERS.map((x) => {
          // Торцы шпалы параллельны рельсам
          const top = (px: number) => `${px} ${railY(px) - FAR_RAIL_GAP + 1.5}`;
          const bottom = (px: number) => `${px} ${railY(px) + 8}`;
          return <path key={x} d={`M${top(x + 0.5)}L${top(x + 6.5)}L${bottom(x - 2.5)}L${bottom(x - 8.5)}Z`} />;
        })}
      </g>
      <Rail lift={FAR_RAIL_GAP} scale={0.8} />
      <Rail />
      <g fill="#56677F">
        {SLEEPERS.map((x) => (
          <g key={x}>
            <rect x={x - 4.2} y={railY(x - 1.8) + 3.2} width="4.8" height="2.2" rx="0.6" />
            <rect x={x + 0.2} y={railY(x + 2.2) - FAR_RAIL_GAP + 2.6} width="4" height="1.8" rx="0.5" />
          </g>
        ))}
      </g>

      <g className="hero__speed" stroke={`url(#${id}-wind)`} strokeLinecap="round">
        <path d="M40 206H470" strokeWidth="3" />
        <path d="M0 232H420" strokeWidth="2.5" />
        <path d="M120 258H520" strokeWidth="3" />
        <path d="M200 184H440" strokeWidth="2" />
        <path d="M80 150H330" strokeWidth="1.5" />
      </g>

      <g className="hero__train-drive" style={{ transform: `translate(${-offset}px, ${offset * RAIL_SLOPE}px)` }}>
        {/* Тележки с колёсами — под юбкой корпуса */}
        <g fill="#3C4B66">
          <rect x="390" y="250" width="76" height="18" rx="6" />
          <rect x="880" y="242" width="72" height="17" rx="6" />
        </g>
        <g fill="#1B2438" opacity="0.35">
          {WHEELS.map(({ cx, r }) => (
            <ellipse key={cx} cx={cx} cy={railY(cx) + 1.5} rx={r * 0.9} ry="1.6" />
          ))}
        </g>
        {/* Колёса поворачиваются пропорционально пройденному пути */}
        {WHEELS.map(({ cx, cy, r }) => (
          <g key={cx} transform={`rotate(${-offset * 4} ${cx} ${cy})`}>
            <circle cx={cx} cy={cy} r={r} fill="#243047" />
            <path
              d={`M${cx - r + 1.5} ${cy}H${cx + r - 1.5}M${cx} ${cy - r + 1.5}V${cy + r - 1.5}`}
              stroke="#6E819E"
              strokeWidth="1.4"
            />
            <circle cx={cx} cy={cy} r="1.8" fill="#9FB0C8" />
          </g>
        ))}

        {/* Кузов слегка покачивается на подвеске */}
        <g className="hero__train-bob">
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

export type Mood =
  | "calm"
  | "happy"
  | "angry"
  | "annoyed"
  | "worried"
  | "pained"
  | "tipsy"
  | "surprised"
  | "sad"
  | "thinking"
  | "serious";

/** Лицо персонажа: глаза, брови и рот передают эмоцию — единый стиль для всех иллюстраций. */
function Face({ cx, cy, r, mood = "calm", ink = "#2B2320" }: { cx: number; cy: number; r: number; mood?: Mood; ink?: string }) {
  const ex = r * 0.36;
  const ey = cy + r * 0.08;
  const er = Math.max(0.9, r * 0.11);
  const by = cy - r * 0.16;
  const my = cy + r * 0.46;
  const mw = r * 0.28;
  const line = { stroke: ink, strokeWidth: Math.max(1, r * 0.1), strokeLinecap: "round", fill: "none" } as const;
  const eyes = [cx - ex, cx + ex];
  return (
    <g>
      {mood === "happy" || mood === "pained" || mood === "tipsy"
        ? eyes.map((x) => (
            <path
              key={x}
              {...line}
              d={
                mood === "happy"
                  ? `M${x - er * 1.4} ${ey}q${er * 1.4} ${-er * 1.8} ${er * 2.8} 0`
                  : mood === "pained"
                    ? `M${x - er * 1.4} ${ey}q${er * 1.4} ${er * 1.2} ${er * 2.8} 0`
                    : `M${x - er * 1.4} ${ey}h${er * 2.8}`
              }
            />
          ))
        : eyes.map((x) => <circle key={x} cx={x} cy={ey} r={mood === "surprised" ? er * 1.3 : er} fill={ink} />)}
      {mood === "angry" && <path {...line} d={`M${cx - ex - er * 2} ${by - er}l${er * 3} ${er * 1.6}M${cx + ex + er * 2} ${by - er}l${-er * 3} ${er * 1.6}`} />}
      {mood === "surprised" && (
        <path {...line} d={`M${cx - ex - er * 1.6} ${by - er * 1.2}q${er * 1.6} ${-er * 1.4} ${er * 3.2} 0M${cx + ex - er * 1.6} ${by - er * 1.2}q${er * 1.6} ${-er * 1.4} ${er * 3.2} 0`} />
      )}
      {mood === "thinking" && (
        <path {...line} d={`M${cx - ex - er * 1.8} ${by}h${er * 3}M${cx + ex - er * 1.4} ${by - er * 1.4}q${er * 1.6} ${-er} ${er * 3} ${er * 0.4}`} />
      )}
      {mood === "serious" && <path {...line} d={`M${cx - ex - er * 1.8} ${by + er * 0.4}h${er * 3.2}M${cx + ex + er * 1.8} ${by + er * 0.4}h${-er * 3.2}`} />}
      {(mood === "worried" || mood === "pained" || mood === "sad") && (
        <path {...line} d={`M${cx - ex - er * 2} ${by + er * 0.6}l${er * 3} ${-er * 1.4}M${cx + ex + er * 2} ${by + er * 0.6}l${-er * 3} ${-er * 1.4}`} />
      )}
      {mood === "annoyed" && <path {...line} d={`M${cx - ex - er * 1.8} ${by}h${er * 3}M${cx + ex + er * 1.8} ${by}h${-er * 3}`} />}
      {mood === "tipsy" && (
        <g fill="#F28B7D" opacity="0.7">
          <circle cx={cx - ex * 1.3} cy={ey + er * 2.6} r={er * 1.8} />
          <circle cx={cx + ex * 1.3} cy={ey + er * 2.6} r={er * 1.8} />
        </g>
      )}
      {mood === "calm" && <path {...line} d={`M${cx - mw} ${my}q${mw} ${mw * 0.7} ${mw * 2} 0`} />}
      {mood === "happy" && <path d={`M${cx - mw * 1.2} ${my - mw * 0.2}q${mw * 1.2} ${mw * 1.8} ${mw * 2.4} 0z`} fill={ink} />}
      {mood === "tipsy" && <path {...line} d={`M${cx - mw * 1.3} ${my - mw * 0.2}q${mw * 1.4} ${mw * 1.2} ${mw * 2.6} ${-mw * 0.5}`} />}
      {mood === "angry" && <ellipse cx={cx} cy={my} rx={mw * 0.8} ry={mw * 0.6} fill={ink} />}
      {mood === "worried" && <ellipse cx={cx} cy={my} rx={mw * 0.45} ry={mw * 0.4} fill={ink} />}
      {mood === "surprised" && <ellipse cx={cx} cy={my} rx={mw * 0.5} ry={mw * 0.65} fill={ink} />}
      {mood === "thinking" && <path {...line} d={`M${cx - mw * 0.6} ${my}h${mw * 1.4}`} />}
      {mood === "serious" && <path {...line} d={`M${cx - mw} ${my}h${mw * 2}`} />}
      {(mood === "annoyed" || mood === "pained" || mood === "sad") && (
        <path {...line} d={`M${cx - mw} ${my + mw * 0.3}q${mw} ${-mw * 0.6} ${mw * 2} 0`} />
      )}
    </g>
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

// ───────── Персонажи ─────────
// Все люди — объёмные фигурки-манекены из одного компонента: круглая голова с бликом, гладкое тело,
// одинаковые пропорции. Все манекены серые; сотрудники поезда — в одинаковой форме, пассажиры — с оттенком своей одежды.

export type Outfit = "uniform" | { top: string; bottom: string };
export type Hand = "down" | "point" | "hold" | "radio" | "hush" | "chest" | "throat";
type Item = "ticket" | "bottle" | "cup" | "extinguisher" | "radio";

const MANNEQUIN_GREY = "#C3C8CF";
// Форма сотрудника: тёмно-синий китель и брюки
const UNIFORM_COLOR = "#2A3B66";

// Куда тянется правая рука (координаты фигуры ростом ~92 с опорой в точке 0,0)
const HAND_POSITION: Record<Hand, [number, number]> = {
  down: [15, -36],
  point: [28, -54],
  hold: [25, -45],
  radio: [10, -73],
  hush: [2, -71],
  chest: [5, -52],
  throat: [3, -65],
};

function mix(from: string, to: string, share: number): string {
  const channel = (hex: string, i: number) => parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16);
  const parts = [0, 1, 2].map((i) => Math.round(channel(from, i) * (1 - share) + channel(to, i) * share));
  return `#${parts.map((value) => value.toString(16).padStart(2, "0")).join("")}`;
}

export interface PersonProps {
  x: number;
  /** Стоя — уровень пола под ногами, сидя — уровень сиденья. */
  y: number;
  s?: number;
  pose?: "stand" | "sit";
  /** Смотрит влево. */
  flip?: boolean;
  outfit: Outfit;
  mood?: Mood;
  hand?: Hand;
  item?: Item;
}

function HeldItem({ kind, x, y }: { kind: Item; x: number; y: number }) {
  switch (kind) {
    case "ticket":
      return (
        <g transform={`rotate(-12 ${x} ${y})`}>
          <rect x={x + 1} y={y - 5} width="12" height="8" rx="1.5" fill="#FFFFFF" stroke="#9AA9C0" />
          <path d={`M${x + 3} ${y - 2}h7M${x + 3} ${y + 0.5}h4`} stroke="#9AA9C0" strokeWidth="1" />
        </g>
      );
    case "bottle":
      return (
        <g>
          <rect x={x - 2.5} y={y - 14} width="5.5" height="13" rx="2" fill="#3F8F5A" />
          <rect x={x - 1} y={y - 18} width="2.5" height="5" rx="1" fill="#2F6E45" />
        </g>
      );
    case "cup":
      return (
        <g>
          <path d={`M${x - 1} ${y - 10}h9l-1.5 11h-6z`} fill="#FFFFFF" stroke="#9AA9C0" strokeWidth="0.8" />
          <path d={`M${x} ${y - 6}h7`} stroke="#8FC3F0" strokeWidth="3" />
        </g>
      );
    case "extinguisher":
      return (
        <g>
          <rect x={x - 5} y={y + 1} width="10" height="20" rx="4.5" fill="#E1343C" />
          <rect x={x - 3} y={y - 3} width="6" height="5" rx="1" fill="#243047" />
          <path d={`M${x + 3} ${y - 1}q8 0 9 7`} stroke="#243047" strokeWidth="2" fill="none" strokeLinecap="round" />
          <rect x={x - 3} y={y + 7} width="6" height="6" rx="1" fill="#FFFFFF" opacity="0.85" />
        </g>
      );
    case "radio":
      return (
        <g>
          <rect x={x - 2.5} y={y - 6} width="5.5" height="11" rx="1.5" fill="#1B1F2A" />
          <path d={`M${x + 1.5} ${y - 6}v-5`} stroke="#1B1F2A" strokeWidth="1.4" strokeLinecap="round" />
        </g>
      );
  }
}

export function Person({ x, y, s = 1, pose = "stand", flip = false, outfit, mood = "calm", hand = "down", item }: PersonProps) {
  const id = useSvgId();
  const uniform = outfit === "uniform";
  // Пассажира выдаёт лёгкий оттенок его одежды на сером манекене, сотрудника — одинаковая форма
  const base = uniform ? MANNEQUIN_GREY : mix(MANNEQUIN_GREY, outfit.top, 0.22);
  const cloth = uniform ? UNIFORM_COLOR : base;
  const dark = mix(cloth, "#000000", 0.32);
  const body = `url(#${id}-body)`;
  const lift = pose === "sit" ? 32 : 0;
  const [hx, hy] = HAND_POSITION[hand];
  return (
    <g transform={`translate(${x} ${y}) scale(${flip ? -s : s} ${s})`}>
      <defs>
        <radialGradient id={`${id}-head`} cx="0.36" cy="0.3" r="0.75">
          <stop offset="0" stopColor={mix(base, "#FFFFFF", 0.75)} />
          <stop offset="0.45" stopColor={base} />
          <stop offset="1" stopColor={mix(base, "#000000", 0.32)} />
        </radialGradient>
        {/* Свет слева: одна горизонтальная растяжка для всех частей тела в координатах фигуры */}
        <linearGradient id={`${id}-body`} gradientUnits="userSpaceOnUse" x1="-16" y1="0" x2="16" y2="0">
          <stop offset="0" stopColor={mix(cloth, "#FFFFFF", uniform ? 0.3 : 0.55)} />
          <stop offset="0.45" stopColor={cloth} />
          <stop offset="1" stopColor={dark} />
        </linearGradient>
      </defs>
      {pose === "stand" ? (
        <g stroke={body} strokeLinecap="round" fill="none">
          <path d="M-4.6 -32L-6 -4" strokeWidth="8.5" />
          <path d="M4.6 -32L6 -4" strokeWidth="8.5" />
          <ellipse cx="-7.5" cy="-1.8" rx="6.5" ry="3" fill={dark} stroke="none" />
          <ellipse cx="7.5" cy="-1.8" rx="6.5" ry="3" fill={dark} stroke="none" />
        </g>
      ) : null}
      <g transform={`translate(0 ${lift})`}>
        <path d="M-9.5 -58L-13.5 -37" stroke={body} strokeWidth="6.5" strokeLinecap="round" />
        <circle cx="-13.8" cy="-35.5" r="3.6" fill={base} />
        {/* Корпус манекена: плечи, грудь, талия, бёдра — без складок и деталей одежды */}
        <path
          d="M-9 -31C-10 -36 -8.5 -40 -8 -43C-9.5 -48 -11.5 -53 -11.5 -57Q-11 -63 -3 -63.5H3Q11 -63 11.5 -57C11.5 -53 9.5 -48 8 -43C8.5 -40 10 -36 9 -31Q0 -27.5 -9 -31Z"
          fill={body}
        />
        <rect x="-3.2" y="-69" width="6.4" height="8" rx="3" fill={base} />
        {uniform && (
          <g>
            <path d="M-4.5 -63.3L0 -55L4.5 -63.3Z" fill="#F4F6FA" />
            <path d="M-1.1 -60.5H1.1L2 -50.5L0 -48L-2 -50.5Z" fill="#D23A3A" />
            <rect x="4.5" y="-55.5" width="4.5" height="3.2" rx="1" fill="#E7C15A" />
          </g>
        )}
        <circle cx="0" cy="-79" r="12.5" fill={`url(#${id}-head)`} />
        <Face cx={0} cy={-78} r={12} mood={mood} ink={mix(base, "#000000", 0.62)} />
        {uniform && (
          <g>
            <path d="M-12.5 -85Q0 -99 12.5 -85Z" fill={UNIFORM_COLOR} />
            <rect x="-12" y="-87.5" width="24" height="2.6" rx="1" fill="#E7C15A" />
            <path d="M-12.5 -84.5H16" stroke="#18233F" strokeWidth="2.6" strokeLinecap="round" />
          </g>
        )}
        <path
          d={`M9.5 -58Q${(9.5 + hx) / 2 + 4} ${(-58 + hy) / 2} ${hx} ${hy}`}
          stroke={body}
          strokeWidth="6.5"
          strokeLinecap="round"
          fill="none"
        />
        {item && <HeldItem kind={item} x={hx} y={hy} />}
        <circle cx={hx} cy={hy} r="3.6" fill={base} />
      </g>
      {pose === "sit" && (
        <g stroke={body} strokeLinecap="round" strokeLinejoin="round" fill="none">
          <path d="M-4 -3H20L22 18" strokeWidth="9" />
          <ellipse cx="25" cy="21" rx="6.5" ry="3" fill={dark} stroke="none" />
        </g>
      )}
    </g>
  );
}

// ───────── Сцены сценариев ─────────

/** Конфликт из-за места: пассажир с билетом и пассажирка, занявшая его кресло 5А. */
function SeatConflict() {
  return (
    <Frame top="#EFF5FD" bottom="#D6E4F5">
      <rect width="160" height="9" fill="#C9D6E8" />
      <rect y="9" width="160" height="3" fill="#B3C4DB" />
      <WindowView x={92} y={18} width={62} height={42} />
      <path d="M0 132L160 124V150H0Z" fill="#C5D3E6" />
      <path d="M0 140L160 133" stroke="#B2C3DA" strokeWidth="2" />
      <rect x="100" y="72" width="50" height="62" rx="12" fill="#2A57C0" />
      <rect x="106" y="68" width="38" height="12" rx="5" fill="#F4F7FB" />
      <rect x="120" y="56" width="24" height="12" rx="3" fill="#1E5FD6" />
      <text x="132" y="65" textAnchor="middle" fontSize="8" fontWeight="800" fill="#FFFFFF" fontFamily="Manrope, sans-serif">
        5А
      </text>
      <rect x="92" y="116" width="64" height="18" rx="8" fill="#1F459D" />
      <Person x={126} y={118} s={0.78} pose="sit" flip outfit={{ top: "#E8735A", bottom: "#3A4A6B" }} mood="annoyed" />
      <rect x="148" y="104" width="10" height="28" rx="4" fill="#1B3D8C" />
      <Person x={42} y={144} s={0.95} outfit={{ top: "#56657F", bottom: "#34405A" }} mood="angry" hand="point" item="ticket" />
      <path d="M62 14h22a6 6 0 0 1 6 6v8a6 6 0 0 1-6 6H72l-6 6v-6h-4a6 6 0 0 1-6-6v-8a6 6 0 0 1 6-6z" fill="#FFFFFF" />
      <text x="73" y="29" textAnchor="middle" fontSize="13" fontWeight="800" fill="#E1343C" fontFamily="Manrope, sans-serif">
        !
      </text>
    </Frame>
  );
}

/** Нетрезвый пассажир и проводник. */
function ConductorAndPassenger() {
  return (
    <Frame top="#E9F1FB" bottom="#D3E2F4">
      <WindowView x={8} y={10} width={64} height={46} />
      <path d="M0 134L160 128V150H0Z" fill="#C5D3E6" />
      <rect x="10" y="66" width="46" height="62" rx="11" fill="#2E5CC4" />
      <rect x="16" y="62" width="34" height="11" rx="5" fill="#F4F7FB" />
      <rect x="6" y="114" width="64" height="18" rx="7" fill="#244DA8" />
      <Person x={30} y={116} s={0.8} pose="sit" outfit={{ top: "#4F7F52", bottom: "#34405A" }} mood="tipsy" hand="hold" item="bottle" />
      <g stroke="#9AA9C0" strokeWidth="1.5" fill="none" strokeLinecap="round">
        <path d="M52 50q4-4 8 0" />
        <path d="M58 42q3-3 6 0" />
      </g>
      <Person x={124} y={146} s={0.95} flip outfit="uniform" mood="calm" hand="point" />
    </Frame>
  );
}

/** Пассажиру плохо с сердцем, проводница вызывает помощь по рации. */
function MedicalHelp() {
  return (
    <Frame top="#EAF2FC" bottom="#D0E0F4">
      <WindowView x={92} y={8} width={62} height={46} />
      <path d="M0 134L160 128V150H0Z" fill="#C5D3E6" />
      <rect x="84" y="62" width="50" height="66" rx="12" fill="#2E5CC4" />
      <rect x="90" y="58" width="38" height="11" rx="5" fill="#F4F7FB" />
      <rect x="76" y="112" width="68" height="18" rx="7" fill="#244DA8" />
      <Person x={110} y={114} s={0.8} pose="sit" flip outfit={{ top: "#7DA7D9", bottom: "#34405A" }} mood="pained" hand="chest" />
      <Person x={38} y={146} s={0.95} outfit="uniform" mood="worried" hand="radio" item="radio" />
      <rect x="60" y="126" width="22" height="16" rx="3" fill="#FFFFFF" stroke="#D8343C" strokeWidth="1.5" />
      <path d="M71 130v8M67 134h8" stroke="#D8343C" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M66 126v-3h10v3" stroke="#D8343C" strokeWidth="1.5" fill="none" />
    </Frame>
  );
}

/** Бесхозная сумка на сиденье, проводник докладывает по рации. */
function LeftBag() {
  return (
    <Frame top="#6A8FDC" bottom="#243F8F">
      <WindowView x={52} y={6} width={100} height={40} />
      <path d="M0 134L160 128V150H0Z" fill="#1B3272" />
      <rect x="62" y="38" width="80" height="88" rx="16" fill="#2B56BF" />
      <rect x="74" y="30" width="56" height="18" rx="9" fill="#3A68D2" />
      <rect x="50" y="112" width="104" height="26" rx="10" fill="#1E428F" />
      <ellipse cx="104" cy="118" rx="26" ry="4" fill="#0E1A3A" opacity="0.35" />
      <path d="M94 64C94 52 114 52 114 64" stroke="#1B1F2A" strokeWidth="5" fill="none" />
      <rect x="82" y="62" width="44" height="56" rx="13" fill="#1B1F2A" />
      <rect x="89" y="92" width="30" height="20" rx="6" fill="#2A2F3C" />
      <path d="M90 78H118" stroke="#3A4150" strokeWidth="2" />
      <path d="M142 56l12 22h-24z" fill="#FFC53D" stroke="#E0A000" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M142 64v7M142 74v1" stroke="#5A4000" strokeWidth="2.2" strokeLinecap="round" />
      <Person x={26} y={146} s={0.92} outfit="uniform" mood="worried" hand="radio" item="radio" />
    </Frame>
  );
}

/** Табло задержки: недовольные пассажиры и проводник на платформе. */
function DelayBoard() {
  return (
    <Frame top="#EAF1FB" bottom="#CFDFF3">
      <rect x="40" y="58" width="4" height="12" fill="#6E7F99" />
      <rect x="116" y="58" width="4" height="12" fill="#6E7F99" />
      <rect x="16" y="6" width="128" height="54" rx="8" fill="#17223A" />
      <circle className="blink" cx="134" cy="16" r="3" fill="#FF8A3D" />
      <text x="26" y="20" fontSize="9" fill="#9FB0CC" fontFamily="Manrope, sans-serif">
        15:43 · ВСМ-400
      </text>
      <text x="80" y="46" fontSize="19" fontWeight="800" fill="#FF8A3D" textAnchor="middle" fontFamily="Manrope, sans-serif">
        Задержка
      </text>
      <rect y="74" width="160" height="40" fill="#F5F8FC" />
      <rect y="80" width="160" height="12" fill="#2B3F66" />
      <g stroke="#DCE6F4" strokeWidth="2">
        {[20, 44, 68, 92, 116, 140].map((x) => (
          <path key={x} d={`M${x} 80V92`} />
        ))}
      </g>
      <rect y="98" width="160" height="3" fill="#E1343C" />
      <rect y="103" width="160" height="3" fill="#2A5FD8" />
      <rect y="114" width="160" height="36" fill="#BFCDE0" />
      <path d="M0 118H160" stroke="#FFFFFF" strokeWidth="2" strokeDasharray="8 6" opacity="0.8" />
      <Person x={26} y={146} s={0.55} outfit={{ top: "#E8735A", bottom: "#3A4A6B" }} mood="annoyed" />
      <Person x={56} y={146} s={0.58} outfit={{ top: "#56657F", bottom: "#34405A" }} mood="angry" hand="point" />
      <Person x={124} y={146} s={0.6} flip outfit="uniform" mood="calm" hand="hold" />
    </Frame>
  );
}

/** Шумная компания ночью: за окном луна, проводник просит говорить тише. */
function NoisyGroup() {
  return (
    <Frame top="#3B4E7E" bottom="#7F95C2">
      <rect x="10" y="10" width="64" height="40" rx="9" fill="#15204A" stroke="#5A6E9C" strokeWidth="2" />
      <circle cx="58" cy="22" r="6" fill="#FFE9A8" />
      <circle cx="61" cy="20" r="5" fill="#15204A" />
      <g fill="#FFFFFF">
        <circle cx="22" cy="20" r="1" />
        <circle cx="36" cy="30" r="0.9" />
        <circle cx="30" cy="16" r="0.8" />
      </g>
      <rect x="98" y="10" width="54" height="18" rx="5" fill="#15204A" />
      <text x="125" y="23" textAnchor="middle" fontSize="9" fontWeight="700" fill="#FFB45A" fontFamily="Manrope, sans-serif">
        23:40
      </text>
      <path d="M0 134L160 128V150H0Z" fill="#5E7098" />
      <rect x="2" y="84" width="104" height="30" rx="10" fill="#2E5CC4" />
      <rect x="0" y="108" width="108" height="16" rx="7" fill="#244DA8" />
      {[
        { x: 16, top: "#E8735A" },
        { x: 46, top: "#7BA05B" },
        { x: 76, top: "#F2B84B" },
      ].map(({ x, top }) => (
        <Person key={x} x={x} y={110} s={0.62} pose="sit" outfit={{ top, bottom: "#34405A" }} mood="happy" />
      ))}
      <rect x="96" y="96" width="14" height="18" rx="4" fill="#1B1F2A" />
      <circle cx="103" cy="102" r="3.5" fill="#3A4150" />
      <g fill="#FFE9A8" className="notes">
        <path d="M104 72v12a3.5 3.5 0 1 1-2-3V72h7v3.5z" />
        <path d="M118 62v10a3 3 0 1 1-2-2.6V62h6v3z" />
      </g>
      <Person x={138} y={146} s={0.88} flip outfit="uniform" mood="worried" hand="hush" />
    </Frame>
  );
}

/** Посадка: пассажир показывает скриншот билета, проводник проверяет его у дверей вагона. */
function BoardingTicket() {
  return (
    <Frame top="#E6EEF8" bottom="#C9D6E8">
      <rect x="0" y="18" width="160" height="92" fill="#F4F7FB" />
      <rect x="0" y="34" width="160" height="16" fill="#2B3F66" />
      {[8, 40, 72].map((x) => (
        <rect key={x} x={x} y="36" width="24" height="12" rx="3" fill="#CFE3FF" />
      ))}
      <rect x="104" y="30" width="40" height="80" rx="4" fill="#3E567F" />
      <rect x="108" y="36" width="32" height="70" rx="3" fill="#56709A" />
      <rect x="0" y="96" width="160" height="4" fill="#E1343C" />
      <rect x="0" y="102" width="160" height="3" fill="#2A5FD8" />
      <path d="M0 112H160V150H0Z" fill="#BFCDE0" />
      <path d="M0 118H160" stroke="#FFFFFF" strokeWidth="2" strokeDasharray="8 6" opacity="0.8" />
      <rect x="10" y="6" width="44" height="18" rx="5" fill="#17223A" />
      <text x="32" y="19" textAnchor="middle" fontSize="9" fontWeight="700" fill="#FFB45A" fontFamily="Manrope, sans-serif">
        7 мин
      </text>
      <Person x={42} y={146} s={0.9} outfit={{ top: "#7A8699", bottom: "#2F3B55" }} mood="worried" hand="point" item="ticket" />
      <Person x={124} y={146} s={0.9} flip outfit="uniform" mood="thinking" hand="hold" item="radio" />
      <path d="M70 52h14a5 5 0 0 1 5 5v6a5 5 0 0 1-5 5h-6l-4 4v-4h-4a5 5 0 0 1-5-5v-6a5 5 0 0 1 5-5z" fill="#FFFFFF" />
      <text x="77" y="65" textAnchor="middle" fontSize="11" fontWeight="800" fill="#1E5FD6" fontFamily="Manrope, sans-serif">
        ?
      </text>
    </Frame>
  );
}

/** Посадка: пассажирка с собакой без переноски и велосипед, который перегородит проход. */
function PetAndBicycle() {
  return (
    <Frame top="#E6EEF8" bottom="#C9D6E8">
      <rect x="0" y="18" width="160" height="92" fill="#F4F7FB" />
      <rect x="0" y="34" width="160" height="16" fill="#2B3F66" />
      <rect x="0" y="96" width="160" height="4" fill="#E1343C" />
      <rect x="0" y="102" width="160" height="3" fill="#2A5FD8" />
      <path d="M0 112H160V150H0Z" fill="#BFCDE0" />
      <g stroke="#243047" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="92" cy="130" r="12" />
        <circle cx="128" cy="130" r="12" />
        <path d="M92 130l12-20h14l10 20M104 110l6 20M116 104h6M104 110l-4-8h-5" />
      </g>
      <Person x={40} y={146} s={0.88} outfit={{ top: "#7BA05B", bottom: "#3A3550" }} mood="happy" hand="down" />
      <path d="M55 110q10 12 20 22" stroke="#8A5A2B" strokeWidth="1.5" fill="none" />
      <g fill="#C98A3A">
        <ellipse cx="80" cy="136" rx="10" ry="6" />
        <circle cx="89" cy="130" r="5" />
        <path d="M86 126l2-5 3 4z" />
        <rect x="72" y="139" width="3" height="7" rx="1" />
        <rect x="84" y="139" width="3" height="7" rx="1" />
        <path d="M70 134q-5-3-4-8" stroke="#C98A3A" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      </g>
      <circle cx="91" cy="129" r="1" fill="#2B2320" />
      <Person x={140} y={146} s={0.72} flip outfit="uniform" mood="serious" hand="point" />
      <path d="M8 8l14 0 0 14-14 0z" fill="#FFC53D" />
      <path d="M15 11v6M15 19v1" stroke="#5A4000" strokeWidth="2" strokeLinecap="round" />
    </Frame>
  );
}

/** Паническая атака: проводник рядом с пассажиркой, дышат вместе, стакан воды. */
function PanicAttack() {
  return (
    <Frame top="#EDF3FC" bottom="#D3E1F4">
      <WindowView x={8} y={10} width={64} height={44} />
      <path d="M0 136L160 130V150H0Z" fill="#C5D3E6" />
      <rect x="12" y="60" width="50" height="66" rx="12" fill="#2A57C0" />
      <rect x="18" y="56" width="38" height="11" rx="5" fill="#F4F7FB" />
      <rect x="6" y="110" width="68" height="18" rx="8" fill="#1F459D" />
      <Person x={30} y={112} s={0.8} pose="sit" outfit={{ top: "#7B5EA7", bottom: "#3A3550" }} mood="worried" hand="chest" />
      <g stroke="#8FC3F0" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.9">
        <path d="M58 44q6-4 12 0" />
        <path d="M60 36q5-3 10 0" />
      </g>
      <Person x={118} y={146} s={0.92} flip outfit="uniform" mood="calm" hand="hold" item="cup" />
      <rect x="88" y="10" width="68" height="28" rx="8" fill="#FFFFFF" />
      <path d="M106 21h8m4 0h8m4 0h8" stroke="#1E5FD6" strokeWidth="3" strokeLinecap="round" />
      <text x="124" y="33" textAnchor="middle" fontSize="6" fill="#56657F" fontFamily="Manrope, sans-serif">
        вдох · пауза · выдох
      </text>
    </Frame>
  );
}

/** Потерявшийся ребёнок: проводник присел рядом в тамбуре. */
function LostChild() {
  return (
    <Frame top="#E3E9F2" bottom="#C3CEDD">
      <rect x="18" y="8" width="56" height="120" rx="6" fill="#F2F5FA" />
      <rect x="26" y="18" width="40" height="36" rx="5" fill="#CFE3FF" />
      <rect x="86" y="8" width="56" height="120" rx="6" fill="#F2F5FA" />
      <rect x="94" y="18" width="40" height="36" rx="5" fill="#CFE3FF" />
      <path d="M0 128H160V150H0Z" fill="#AFBDD2" />
      <Person x={52} y={146} s={0.95} outfit="uniform" mood="calm" hand="point" />
      <Person x={108} y={146} s={0.6} flip outfit={{ top: "#3E6FD8", bottom: "#2F3B55" }} mood="sad" hand="down" />
      <path d="M126 72c-4-5-11-1-8 4l8 7 8-7c3-5-4-9-8-4z" fill="#E1343C" opacity="0.85" />
    </Frame>
  );
}

/** Обед в бизнес-классе: тележка, блюдо под крышкой, пассажир за столиком. */
function BusinessCatering() {
  return (
    <Frame top="#F2EEE8" bottom="#E0D6C8">
      <WindowView x={92} y={8} width={62} height={42} />
      <path d="M0 134L160 128V150H0Z" fill="#CFC2AF" />
      <rect x="98" y="60" width="50" height="68" rx="12" fill="#2A57C0" />
      <rect x="104" y="56" width="38" height="11" rx="5" fill="#F4F7FB" />
      <rect x="90" y="112" width="66" height="18" rx="8" fill="#1F459D" />
      <Person x={124} y={114} s={0.78} pose="sit" flip outfit={{ top: "#2E3A4F", bottom: "#1E2636" }} mood="thinking" />
      <rect x="72" y="92" width="34" height="4" rx="2" fill="#8C7A66" />
      <ellipse cx="88" cy="90" rx="11" ry="3" fill="#FFFFFF" />
      <path d="M79 90q9-13 18 0z" fill="#D9DEE7" />
      <circle cx="88" cy="79" r="1.5" fill="#B8C1CF" />
      <rect x="10" y="100" width="46" height="30" rx="4" fill="#6E7F99" />
      <rect x="10" y="98" width="46" height="4" rx="2" fill="#56657F" />
      <circle cx="16" cy="134" r="3" fill="#243047" />
      <circle cx="50" cy="134" r="3" fill="#243047" />
      <rect x="18" y="90" width="10" height="8" rx="2" fill="#FFFFFF" />
      <rect x="32" y="88" width="8" height="10" rx="2" fill="#F2B84B" />
      <Person x={34} y={146} s={0.82} outfit="uniform" mood="calm" hand="hold" item="cup" />
    </Frame>
  );
}

/** Первый класс: неработающая розетка, недовольный пассажир и проводник с маской для сна. */
function FirstClassComfort() {
  return (
    <Frame top="#F4EFE6" bottom="#E3D8C6">
      <WindowView x={10} y={8} width={70} height={46} />
      <path d="M0 134L160 128V150H0Z" fill="#CFC2AF" />
      <rect x="18" y="58" width="60" height="70" rx="16" fill="#E9DCC3" />
      <rect x="24" y="54" width="48" height="12" rx="6" fill="#F7F1E6" />
      <rect x="10" y="110" width="76" height="20" rx="9" fill="#D8C7A8" />
      <Person x={42} y={112} s={0.8} pose="sit" outfit={{ top: "#1B1F2A", bottom: "#1E2636" }} mood="annoyed" />
      <rect x="84" y="96" width="14" height="10" rx="2" fill="#FFFFFF" stroke="#B8A58A" />
      <path d="M88 99v4M94 99v4" stroke="#56657F" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M100 88l6 6M106 88l-6 6" stroke="#E1343C" strokeWidth="2" strokeLinecap="round" />
      <Person x={128} y={146} s={0.9} flip outfit="uniform" mood="calm" hand="hold" />
      <path d="M100 96q6-4 12 0q-6 5-12 0z" fill="#2B3F66" />
      <rect x="120" y="10" width="30" height="24" rx="7" fill="#1E5FD6" />
      <text x="135" y="27" textAnchor="middle" fontSize="13" fontWeight="800" fill="#FFFFFF" fontFamily="Manrope, sans-serif">
        1
      </text>
    </Frame>
  );
}

/** Пассажир на кресле-коляске въезжает по пандусу, проводник встречает у двери. */
function WheelchairBoarding() {
  return (
    <Frame top="#EAF1FB" bottom="#CFDFF3">
      <rect x="92" y="6" width="68" height="124" rx="8" fill="#F5F8FC" />
      <rect x="102" y="16" width="46" height="106" rx="5" fill="#2B3F66" />
      <rect x="106" y="22" width="38" height="96" rx="3" fill="#3E567F" />
      <rect x="92" y="116" width="68" height="4" fill="#E1343C" />
      <rect x="92" y="122" width="68" height="3" fill="#2A5FD8" />
      <path d="M0 132H160V150H0Z" fill="#BFCDE0" />
      <path d="M0 132H160" stroke="#A9B9CF" strokeWidth="2" />
      <Person x={124} y={124} s={0.82} flip outfit="uniform" mood="calm" hand="point" />
      <path d="M60 134L104 124" stroke="#7E90AE" strokeWidth="5" strokeLinecap="round" />
      <path d="M60 134L104 124" stroke="#A6B6CD" strokeWidth="2" strokeLinecap="round" />
      <path d="M30 70L33 104H62" stroke="#243047" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" fill="none" />
      <path d="M26 69H35" stroke="#243047" strokeWidth="4" strokeLinecap="round" />
      <Person x={42} y={102} s={0.8} pose="sit" outfit={{ top: "#3E6FD8", bottom: "#34465F" }} mood="calm" />
      <path d="M62 104L64 121H76" stroke="#243047" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
      <circle cx="44" cy="114" r="18" fill="none" stroke="#243047" strokeWidth="3.5" />
      <circle cx="44" cy="114" r="14" fill="none" stroke="#8C9DB6" strokeWidth="1.5" />
      <path d="M44 96V132M26 114H62M31 101L57 127M57 101L31 127" stroke="#8C9DB6" strokeWidth="1" />
      <circle cx="44" cy="114" r="3" fill="#243047" />
      <circle cx="70" cy="128" r="5" fill="none" stroke="#243047" strokeWidth="3" />
      <rect x="6" y="10" width="28" height="28" rx="7" fill="#1E5FD6" />
      <circle cx="20" cy="16.5" r="2.4" fill="#FFFFFF" />
      <path d="M18.5 20V27H24L26.5 32" stroke="#FFFFFF" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M14.5 25.5A5.5 5.5 0 1 0 22.5 31.5" stroke="#FFFFFF" strokeWidth="2.2" fill="none" strokeLinecap="round" />
    </Frame>
  );
}

/** Задымление в тамбуре: встревоженный пассажир и проводник с огнетушителем. */
function SmokeVestibule() {
  const id = useSvgId();
  return (
    <Frame top="#E3E9F2" bottom="#C3CEDD">
      <defs>
        <filter id={`${id}-smoke`}>
          <feGaussianBlur stdDeviation="3" />
        </filter>
      </defs>
      <rect x="6" y="8" width="56" height="120" rx="6" fill="#F2F5FA" />
      <rect x="14" y="18" width="40" height="36" rx="5" fill="#CFE3FF" />
      <path d="M0 128H160V150H0Z" fill="#AFBDD2" />
      <rect x="74" y="96" width="20" height="32" rx="4" fill="#6E7F99" />
      <rect x="70" y="92" width="28" height="6" rx="2" fill="#56657F" />
      <g className="smoke" filter={`url(#${id}-smoke)`} fill="#8D97A8" opacity="0.8">
        <circle cx="84" cy="80" r="11" />
        <circle cx="96" cy="62" r="14" />
        <circle cx="78" cy="48" r="13" />
        <circle cx="100" cy="36" r="15" />
      </g>
      <path d="M81 94q3-8 6 0" stroke="#FF8A3D" strokeWidth="3" fill="none" strokeLinecap="round" />
      <circle cx="148" cy="14" r="5" fill="#FFC53D" className="blink" />
      <Person x={34} y={146} s={0.85} outfit={{ top: "#F2B84B", bottom: "#34405A" }} mood="worried" />
      <Person x={130} y={146} s={0.92} flip outfit="uniform" mood="worried" hand="hold" item="extinguisher" />
    </Frame>
  );
}

const byScenario: Record<string, () => ReactNode> = {
  "boarding-ticket": BoardingTicket,
  "pet-and-bicycle": PetAndBicycle,
  "business-seat-conflict": SeatConflict,
  "drunk-passenger": ConductorAndPassenger,
  "noisy-night": NoisyGroup,
  "passenger-unwell": MedicalHelp,
  "panic-attack": PanicAttack,
  "smoke-vestibule": SmokeVestibule,
  "unattended-item": LeftBag,
  "lost-child": LostChild,
  "business-catering": BusinessCatering,
  "first-class-comfort": FirstClassComfort,
  "train-delay": DelayBoard,
  "wheelchair-boarding": WheelchairBoarding,
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
