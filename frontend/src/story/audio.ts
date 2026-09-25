// Звук новеллы: тихая фоновая музыка по кругу и звуки реплик (вздохи).
// Браузер разрешает звук только после действия пользователя — поэтому play() может отказать,
// и тогда музыка запускается при первом клике или нажатии клавиши на экране истории.

const MUSIC_SRC = "/audio/music.mp3";
const SOUNDS: Record<string, string> = {
  sigh: "/audio/sigh.mp3",
  sigh_long: "/audio/sigh-long.mp3",
  sigh_male: "/audio/sigh-male.mp3",
};
// Музыка — фоном, чтобы не мешать чтению; вздохи заметно громче
const MUSIC_VOLUME = 0.12;
const SOUND_VOLUME = 0.75;
const FADE_MS = 1200;

export class StoryAudio {
  private readonly music = new Audio(MUSIC_SRC);
  private readonly sounds: Record<string, HTMLAudioElement> = {};
  private fadeTimer: ReturnType<typeof setInterval> | undefined;
  private wantsMusic = false;

  constructor(private muted: boolean) {
    this.music.loop = true;
    this.music.volume = 0;
    this.music.preload = "auto";
    for (const [name, src] of Object.entries(SOUNDS)) {
      const sound = new Audio(src);
      sound.preload = "auto";
      sound.volume = SOUND_VOLUME;
      this.sounds[name] = sound;
    }
  }

  /** Запустить музыку с плавным нарастанием; если браузер не разрешил — повторить при следующем действии пользователя. */
  startMusic(): void {
    this.wantsMusic = true;
    if (this.muted) return;
    this.music
      .play()
      .then(() => this.fadeTo(MUSIC_VOLUME))
      .catch(() => {
        // звук ещё не разрешён — resume() вызовет обработчик первого клика
      });
  }

  /** Повторная попытка после действия пользователя. */
  resume(): void {
    if (this.wantsMusic && !this.muted && this.music.paused) this.startMusic();
  }

  play(name: string): void {
    const sound = this.sounds[name];
    if (!sound || this.muted) return;
    sound.currentTime = 0;
    sound.play().catch(() => {
      // звук без разрешения браузера просто пропускается
    });
  }

  setMuted(muted: boolean): void {
    this.muted = muted;
    if (muted) {
      this.fadeTo(0, () => this.music.pause());
      Object.values(this.sounds).forEach((sound) => sound.pause());
    } else if (this.wantsMusic) {
      this.startMusic();
    }
  }

  stop(): void {
    this.wantsMusic = false;
    this.fadeTo(0, () => this.music.pause());
  }

  private fadeTo(target: number, done?: () => void): void {
    clearInterval(this.fadeTimer);
    const step = (target - this.music.volume) / (FADE_MS / 50);
    if (step === 0) {
      done?.();
      return;
    }
    this.fadeTimer = setInterval(() => {
      const next = this.music.volume + step;
      if ((step > 0 && next >= target) || (step < 0 && next <= target)) {
        this.music.volume = target;
        clearInterval(this.fadeTimer);
        done?.();
        return;
      }
      this.music.volume = Math.min(1, Math.max(0, next));
    }, 50);
  }
}
