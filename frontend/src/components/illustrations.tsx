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

type Mood = "calm" | "happy" | "angry" | "annoyed" | "worried" | "pained" | "tipsy";

/** Лицо персонажа: глаза, брови и рот передают эмоцию — единый стиль для всех иллюстраций. */
function Face({ cx, cy, r, mood = "calm" }: { cx: number; cy: number; r: number; mood?: Mood }) {
  const ex = r * 0.36;
  const ey = cy + r * 0.08;
  const er = Math.max(0.9, r * 0.11);
  const by = cy - r * 0.16;
  const my = cy + r * 0.46;
  const mw = r * 0.28;
  const line = { stroke: "#2B2320", strokeWidth: Math.max(1, r * 0.1), strokeLinecap: "round", fill: "none" } as const;
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
        : eyes.map((x) => <circle key={x} cx={x} cy={ey} r={er} fill="#2B2320" />)}
      {mood === "angry" && <path {...line} d={`M${cx - ex - er * 2} ${by - er}l${er * 3} ${er * 1.6}M${cx + ex + er * 2} ${by - er}l${-er * 3} ${er * 1.6}`} />}
      {(mood === "worried" || mood === "pained") && (
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
      {mood === "happy" && <path d={`M${cx - mw * 1.2} ${my - mw * 0.2}q${mw * 1.2} ${mw * 1.8} ${mw * 2.4} 0z`} fill="#8A3B34" />}
      {mood === "tipsy" && <path {...line} d={`M${cx - mw * 1.3} ${my - mw * 0.2}q${mw * 1.4} ${mw * 1.2} ${mw * 2.6} ${-mw * 0.5}`} />}
      {mood === "angry" && <ellipse cx={cx} cy={my} rx={mw * 0.8} ry={mw * 0.6} fill="#8A3B34" />}
      {mood === "worried" && <ellipse cx={cx} cy={my} rx={mw * 0.45} ry={mw * 0.4} fill="#8A3B34" />}
      {(mood === "annoyed" || mood === "pained") && (
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
// Все люди на иллюстрациях собираются из одного компонента: одинаковые пропорции,
// голова соединена с корпусом шеей, у всех сотрудников одна форма.

type Outfit = "uniform" | { top: string; bottom: string };
type Hand = "down" | "point" | "hold" | "radio" | "hush" | "chest" | "throat";
type Item = "ticket" | "bottle" | "injector" | "extinguisher" | "radio";

const UNIFORM = { top: "#1F2E57", bottom: "#18233F" };
const SKIN = "#F2C9A5";
const SHOE = "#141B2E";

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

interface PersonProps {
  x: number;
  /** Стоя — уровень пола под ногами, сидя — уровень сиденья. */
  y: number;
  s?: number;
  pose?: "stand" | "sit";
  /** Смотрит влево. */
  flip?: boolean;
  outfit: Outfit;
  hair?: string;
  hairStyle?: "short" | "bun" | "long";
  mood?: Mood;
  hand?: Hand;
  item?: Item;
  rash?: boolean;
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
    case "injector":
      return (
        <g>
          <rect x={x - 1} y={y - 3} width="17" height="6" rx="3" fill="#FFC53D" />
          <rect x={x + 12} y={y - 3} width="5" height="6" rx="1.5" fill="#F07A2A" />
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

function Person({
  x,
  y,
  s = 1,
  pose = "stand",
  flip = false,
  outfit,
  hair = "#2B2320",
  hairStyle = "short",
  mood = "calm",
  hand = "down",
  item,
  rash = false,
}: PersonProps) {
  const uniform = outfit === "uniform";
  const { top, bottom } = uniform ? UNIFORM : outfit;
  // Сидя верхняя часть тела опускается к сиденью, ноги уходят вперёд
  const lift = pose === "sit" ? 32 : 0;
  const [hx, hy] = HAND_POSITION[hand];
  return (
    <g transform={`translate(${x} ${y}) scale(${flip ? -s : s} ${s})`}>
      {pose === "stand" && (
        <g fill={bottom}>
          <rect x="-10" y="-34" width="8.5" height="33" rx="3.5" />
          <rect x="1.5" y="-34" width="8.5" height="33" rx="3.5" />
          <ellipse cx="-6" cy="0" rx="6.5" ry="2.8" fill={SHOE} />
          <ellipse cx="6" cy="0" rx="6.5" ry="2.8" fill={SHOE} />
        </g>
      )}
      <g transform={`translate(0 ${lift})`}>
        <path d="M-11 -58L-15 -36" stroke={top} strokeWidth="6.5" strokeLinecap="round" />
        <circle cx="-15" cy="-35" r="3" fill={SKIN} />
        <path d="M-13 -31L-14.5 -54Q-14.5 -63.5 -5 -63.5H5Q14.5 -63.5 14.5 -54L13 -31Z" fill={top} />
        <rect x="-3.4" y="-70" width="6.8" height="9" rx="2.5" fill="#E6B894" />
        {uniform ? (
          <g>
            <path d="M-5 -63.5L0 -54L5 -63.5Z" fill="#FFFFFF" />
            <path d="M-1.2 -60.5H1.2L2.2 -50L0 -47.5L-2.2 -50Z" fill="#D23A3A" />
            <path d="M0 -54V-32" stroke="#2A3B6B" strokeWidth="1" />
            <rect x="5.5" y="-55" width="5" height="3.5" rx="1" fill="#E7C15A" />
          </g>
        ) : (
          <path d="M-4.5 -63.5L0 -57L4.5 -63.5Z" fill="#FFFFFF" opacity="0.85" />
        )}
        {hairStyle === "bun" && <circle cx="-8" cy="-86" r="5" fill={hair} />}
        <circle cx="0" cy="-77" r="11" fill={SKIN} />
        {rash && <circle cx="0" cy="-74" r="9" fill="#F29C8A" opacity="0.35" />}
        <path d="M-11.3 -78C-12.5 -92.5 12.5 -92.5 11.3 -78C8 -85.5 -8 -85.5 -11.3 -78Z" fill={hair} />
        {hairStyle === "long" && (
          <path d="M-11 -78C-12 -70 -11 -66 -8 -63M11 -78C12 -70 11 -66 8 -63" stroke={hair} strokeWidth="4" strokeLinecap="round" fill="none" />
        )}
        {uniform && (
          <g>
            <path d="M-12 -83Q0 -96 12 -83Z" fill={UNIFORM.top} />
            <rect x="-11.5" y="-85.5" width="23" height="2.6" rx="1" fill="#E7C15A" />
            <path d="M-12 -82.5H15.5" stroke="#0F1A38" strokeWidth="2.6" strokeLinecap="round" />
          </g>
        )}
        <Face cx={0} cy={-77} r={11} mood={mood} />
        {rash && (
          <g fill="#E1343C" opacity="0.6">
            <circle cx="-7" cy="-72" r="1.5" />
            <circle cx="7" cy="-72" r="1.5" />
            <circle cx="-3" cy="-66" r="1.2" />
            <circle cx="3" cy="-64" r="1.3" />
          </g>
        )}
        <path
          d={`M11 -58Q${(11 + hx) / 2 + 4} ${(-58 + hy) / 2} ${hx} ${hy}`}
          stroke={top}
          strokeWidth="6.5"
          strokeLinecap="round"
          fill="none"
        />
        {item && <HeldItem kind={item} x={hx} y={hy} />}
        <circle cx={hx} cy={hy} r="3" fill={SKIN} />
      </g>
      {/* Сидя бедро лежит поверх корпуса, голень опущена к полу */}
      {pose === "sit" && (
        <g>
          <path d="M-4 -4H20L22 19" stroke={bottom} strokeWidth="10" strokeLinecap="round" strokeLinejoin="round" fill="none" />
          <ellipse cx="25" cy="22" rx="6.5" ry="2.8" fill={SHOE} />
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
      <Person x={126} y={118} s={0.78} pose="sit" flip outfit={{ top: "#E8735A", bottom: "#3A4A6B" }} hair="#6B3F2A" hairStyle="long" mood="annoyed" />
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
      <Person x={30} y={116} s={0.8} pose="sit" outfit={{ top: "#4F7F52", bottom: "#34405A" }} hair="#3A2E2A" mood="tipsy" hand="hold" item="bottle" />
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
      <Person x={110} y={114} s={0.8} pose="sit" flip outfit={{ top: "#7DA7D9", bottom: "#34405A" }} hair="#4A3426" mood="pained" hand="chest" />
      <Person x={38} y={146} s={0.95} outfit="uniform" hairStyle="bun" hair="#6B3F2A" mood="worried" hand="radio" item="radio" />
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
      <Person x={26} y={146} s={0.55} outfit={{ top: "#E8735A", bottom: "#3A4A6B" }} hair="#6B3F2A" hairStyle="long" mood="annoyed" />
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
        { x: 16, top: "#E8735A", hair: "#3A2E2A", style: "short" as const },
        { x: 46, top: "#7BA05B", hair: "#6B3F2A", style: "long" as const },
        { x: 76, top: "#F2B84B", hair: "#2B2320", style: "short" as const },
      ].map(({ x, top, hair, style }) => (
        <Person key={x} x={x} y={110} s={0.62} pose="sit" outfit={{ top, bottom: "#34405A" }} hair={hair} hairStyle={style} mood="happy" />
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

/** Пассажир с острой аллергической реакцией; спутница помогает с автоинжектором. */
function AllergyPassenger() {
  return (
    <Frame top="#EDF3FC" bottom="#D3E1F4">
      <WindowView x={98} y={8} width={56} height={40} />
      <path d="M0 138L160 132V150H0Z" fill="#C5D3E6" />
      <rect x="12" y="50" width="50" height="70" rx="12" fill="#2A57C0" />
      <rect x="18" y="46" width="38" height="12" rx="5" fill="#F4F7FB" />
      <rect x="6" y="104" width="68" height="20" rx="8" fill="#1F459D" />
      <Person x={30} y={106} s={0.8} pose="sit" outfit={{ top: "#3D8C9E", bottom: "#2F3B55" }} hair="#3A2E2A" mood="worried" hand="throat" rash />
      <g stroke="#E1343C" strokeWidth="1.6" strokeLinecap="round">
        <path d="M44 52l3-4M48 58l5-1M42 47l0-5" />
      </g>
      <rect x="126" y="92" width="30" height="4" rx="2" fill="#D6DFEB" />
      <path d="M141 96V108" stroke="#B8C6D8" strokeWidth="2" />
      <ellipse cx="140" cy="90" rx="10" ry="3.2" fill="#FFFFFF" />
      <path d="M133 89q3-5 6-1q3-5 7 1z" fill="#7BB35E" />
      <circle cx="137" cy="87.5" r="1.3" fill="#B07A3A" />
      <circle cx="143" cy="88" r="1.2" fill="#B07A3A" />
      <Person x={100} y={146} s={0.9} flip outfit={{ top: "#7B5EA7", bottom: "#3A3550" }} hair="#6B3F2A" hairStyle="bun" mood="worried" hand="hold" item="injector" />
      <circle cx="24" cy="20" r="12" fill="#FFFFFF" stroke="#E1343C" strokeWidth="2.5" />
      <ellipse cx="24" cy="20" rx="4.5" ry="6" fill="#C98A3A" />
      <path d="M24 15v10" stroke="#9A6522" strokeWidth="1" />
      <path d="M16 12L32 28" stroke="#E1343C" strokeWidth="2.5" strokeLinecap="round" />
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
      <Person x={42} y={102} s={0.8} pose="sit" outfit={{ top: "#3E6FD8", bottom: "#34465F" }} hair="#2E2622" mood="calm" />
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
      <Person x={34} y={146} s={0.85} outfit={{ top: "#F2B84B", bottom: "#34405A" }} hair="#3A2E2A" mood="worried" />
      <Person x={130} y={146} s={0.92} flip outfit="uniform" mood="worried" hand="hold" item="extinguisher" />
    </Frame>
  );
}

const byScenario: Record<string, () => ReactNode> = {
  "business-seat-conflict": SeatConflict,
  "drunk-passenger": ConductorAndPassenger,
  "medical-heart-attack": MedicalHelp,
  "suspicious-item": LeftBag,
  "train-delay-compensation": DelayBoard,
  "noisy-group-night": NoisyGroup,
  "allergy-attack": AllergyPassenger,
  "wheelchair-boarding": WheelchairBoarding,
  "smoke-vestibule": SmokeVestibule,
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
