import { useEffect, useRef, useState, type ReactNode } from "react";
import { Person, type Hand, type Mood } from "../components/illustrations";
import type { Character, Expression, Look, SceneCharacter } from "../types";

// Фоны: фотографии салона и поезда из материалов кейсодержателя; тамбур, бистро и служебное купе нарисованы
const PHOTOS: Record<string, { src: string; tone?: "evening" | "night" }> = {
  salon: { src: "/media/salon.jpg" },
  business: { src: "/media/salon-table.jpg" },
  salon_evening: { src: "/media/salon-table.jpg", tone: "evening" },
  salon_night: { src: "/media/salon.jpg", tone: "night" },
  platform: { src: "/media/train.jpg" },
};

const CROSSFADE_MS = 700;
const LEAVE_MS = 450;

/** Фон сцены с плавной сменой: новый кадр проявляется поверх предыдущего. */
export function StoryBackground({ name }: { name: string }) {
  const [layers, setLayers] = useState([{ name, key: 0 }]);
  const counter = useRef(0);

  useEffect(() => {
    setLayers((current) => {
      if (current[current.length - 1].name === name) return current;
      counter.current += 1;
      return [...current.slice(-1), { name, key: counter.current }];
    });
    const id = setTimeout(() => setLayers((current) => current.slice(-1)), CROSSFADE_MS);
    return () => clearTimeout(id);
  }, [name]);

  return (
    <div className="story-bg" aria-hidden="true">
      {layers.map((layer) => (
        <div key={layer.key} className="story-bg__layer">
          <BackgroundImage name={layer.name} />
        </div>
      ))}
    </div>
  );
}

function BackgroundImage({ name }: { name: string }) {
  const photo = PHOTOS[name];
  if (photo) {
    return <img className={`story-bg__photo ${photo.tone ? `story-bg__photo--${photo.tone}` : ""}`} src={photo.src} alt="" />;
  }
  const Drawn = DRAWN[name] ?? Vestibule;
  return (
    <svg className="story-bg__drawn" viewBox="0 0 1600 900" preserveAspectRatio="xMidYMid slice">
      <Drawn />
    </svg>
  );
}

/** Тамбур: двери вагона, поручни, датчик дыма. */
function Vestibule() {
  return (
    <g>
      <rect width="1600" height="900" fill="#DCE3EC" />
      <rect y="720" width="1600" height="180" fill="#9AA8BB" />
      <path d="M0 720H1600" stroke="#8595AA" strokeWidth="6" />
      <rect x="420" y="90" width="340" height="640" rx="18" fill="#EEF2F7" stroke="#B9C5D4" strokeWidth="6" />
      <rect x="840" y="90" width="340" height="640" rx="18" fill="#EEF2F7" stroke="#B9C5D4" strokeWidth="6" />
      <rect x="470" y="150" width="240" height="250" rx="16" fill="#9FC3EE" />
      <rect x="890" y="150" width="240" height="250" rx="16" fill="#9FC3EE" />
      <path d="M470 330q120-60 240-20V400H470Z M890 320q130-40 240 10V400H890Z" fill="#8FB59E" opacity="0.8" />
      <path d="M800 90V730" stroke="#AEBBCB" strokeWidth="10" />
      <rect x="380" y="300" width="14" height="300" rx="7" fill="#C2CCD9" />
      <rect x="1206" y="300" width="14" height="300" rx="7" fill="#C2CCD9" />
      <rect x="760" y="40" width="80" height="24" rx="12" fill="#F4F6F9" stroke="#B9C5D4" strokeWidth="4" />
      <circle cx="800" cy="52" r="6" fill="#F2B84B" />
      <rect x="1300" y="360" width="150" height="90" rx="10" fill="#1E2A44" />
      <text x="1375" y="415" textAnchor="middle" fontSize="34" fontWeight="800" fill="#FFB45A" fontFamily="Manrope, sans-serif">
        380
      </text>
      <rect x="130" y="380" width="120" height="160" rx="10" fill="#E1343C" opacity="0.9" />
      <rect x="150" y="410" width="80" height="50" rx="6" fill="#FFFFFF" opacity="0.85" />
    </g>
  );
}

/** Вагон-бистро: стойка, меню, высокие стулья. */
function Bistro() {
  return (
    <g>
      <rect width="1600" height="900" fill="#EFE7DC" />
      <rect y="0" width="1600" height="90" fill="#D9CDBB" />
      <rect x="120" y="140" width="1360" height="200" rx="20" fill="#9FC3EE" />
      <path d="M120 290q300-80 700-20t660-10V340H120Z" fill="#9CBF95" opacity="0.8" />
      <rect y="560" width="1600" height="340" fill="#B99C7A" />
      <rect x="0" y="470" width="1600" height="110" rx="10" fill="#6B4F36" />
      <rect x="0" y="470" width="1600" height="18" fill="#8A6A4B" />
      <rect x="1180" y="370" width="300" height="90" rx="12" fill="#1E2A44" />
      <text x="1330" y="428" textAnchor="middle" fontSize="40" fontWeight="800" fill="#FFFFFF" fontFamily="Manrope, sans-serif">
        БИСТРО
      </text>
      {[260, 520, 780].map((x) => (
        <g key={x}>
          <rect x={x - 6} y="600" width="12" height="220" fill="#4A3A2A" />
          <ellipse cx={x} cy="600" rx="60" ry="16" fill="#3A2E24" />
        </g>
      ))}
      <rect x="1000" y="410" width="60" height="60" rx="8" fill="#FFFFFF" />
      <rect x="1080" y="420" width="40" height="50" rx="8" fill="#E8735A" />
    </g>
  );
}

/** Служебное купе бригады: столик, рация, схема поезда. */
function StaffRoom() {
  return (
    <g>
      <rect width="1600" height="900" fill="#E3E8F0" />
      <rect y="690" width="1600" height="210" fill="#A9B6C8" />
      <rect x="200" y="120" width="560" height="340" rx="22" fill="#9FC3EE" />
      <path d="M200 380q200-60 560-10V460H200Z" fill="#8FB59E" opacity="0.8" />
      <rect x="880" y="140" width="520" height="300" rx="14" fill="#FFFFFF" stroke="#C3CEDD" strokeWidth="6" />
      <path d="M920 290H1360" stroke="#1E5FD6" strokeWidth="10" strokeLinecap="round" />
      {[940, 1020, 1100, 1180, 1260, 1340].map((x) => (
        <rect key={x} x={x - 26} y="270" width="52" height="40" rx="8" fill="#DCE8FA" stroke="#1E5FD6" strokeWidth="4" />
      ))}
      <text x="1140" y="200" textAnchor="middle" fontSize="34" fontWeight="700" fill="#243152" fontFamily="Manrope, sans-serif">
        Схема состава
      </text>
      <rect x="120" y="560" width="900" height="36" rx="10" fill="#8C7A66" />
      <rect x="700" y="500" width="40" height="70" rx="8" fill="#1B1F2A" />
      <path d="M730 500v-40" stroke="#1B1F2A" strokeWidth="8" strokeLinecap="round" />
      <rect x="400" y="530" width="120" height="30" rx="6" fill="#FFFFFF" />
    </g>
  );
}

const DRAWN: Record<string, () => ReactNode> = {
  vestibule: Vestibule,
  bistro: Bistro,
  staff_room: StaffRoom,
};

const PLAYER_LOOK: Look = { outfit: "uniform", top: "#1F2E57", bottom: "#18233F", hair: "#2B2320", hair_style: "short", child: false };

const MOODS: Record<Expression, Mood> = {
  neutral: "calm",
  happy: "happy",
  angry: "angry",
  annoyed: "annoyed",
  worried: "worried",
  sad: "sad",
  surprised: "surprised",
  thinking: "thinking",
  serious: "serious",
  pained: "pained",
  tipsy: "tipsy",
};

// Поза подчёркивает эмоцию: задумчивость — рука у подбородка, боль — рука на груди
const HANDS: Partial<Record<Expression, Hand>> = { thinking: "throat", pained: "chest", worried: "chest" };

interface CastProps {
  characters: SceneCharacter[];
  expressions: Record<string, Expression>;
  cast: Record<string, Character>;
  /** Кто сейчас говорит — остальные чуть затемняются. */
  speaker: string | null;
}

/** Персонажи сцены: появляются и уходят плавно, говорящий выделен. */
export function StoryCast({ characters, expressions, cast, speaker }: CastProps) {
  const [leaving, setLeaving] = useState<SceneCharacter[]>([]);
  const previous = useRef<SceneCharacter[]>(characters);

  useEffect(() => {
    const ids = new Set(characters.map((character) => character.id));
    const gone = previous.current.filter((character) => !ids.has(character.id));
    previous.current = characters;
    if (!gone.length) return;
    setLeaving(gone);
    const id = setTimeout(() => setLeaving([]), LEAVE_MS);
    return () => clearTimeout(id);
  }, [characters]);

  const someoneSpeaks = speaker !== null && characters.some((character) => character.id === speaker);
  return (
    <div className="story-cast" aria-hidden="true">
      {leaving.map((character) => (
        <Sprite key={`leave-${character.id}`} character={character} look={lookOf(character.id, cast)} expression={character.expression} state="leaving" />
      ))}
      {characters.map((character) => (
        <Sprite
          key={character.id}
          character={character}
          look={lookOf(character.id, cast)}
          expression={expressions[character.id] ?? character.expression}
          state={!someoneSpeaks || speaker === character.id ? "active" : "dimmed"}
        />
      ))}
    </div>
  );
}

function lookOf(id: string, cast: Record<string, Character>): Look {
  return id === "player" ? PLAYER_LOOK : (cast[id]?.look ?? PLAYER_LOOK);
}

interface SpriteProps {
  character: SceneCharacter;
  look: Look;
  expression: Expression;
  state: "active" | "dimmed" | "leaving";
}

function Sprite({ character, look, expression, state }: SpriteProps) {
  const outfit = look.outfit === "uniform" ? "uniform" : { top: look.top, bottom: look.bottom };
  return (
    <div className={`story-sprite story-sprite--${character.position} story-sprite--${state} ${look.child ? "story-sprite--child" : ""}`}>
      <svg viewBox="-36 -100 72 104" className="story-sprite__svg">
        <ellipse cx="0" cy="1" rx="22" ry="3.5" fill="#0B1222" opacity="0.18" />
        <Person
          x={0}
          y={0}
          // Персонажи справа повёрнуты к центру сцены
          flip={character.position === "right"}
          outfit={outfit}
          hair={look.hair}
          hairStyle={look.hair_style}
          mood={MOODS[expression]}
          hand={HANDS[expression] ?? "down"}
        />
      </svg>
    </div>
  );
}
