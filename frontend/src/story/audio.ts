// Звук новеллы: тихая фоновая музыка по кругу, звуки реплик (вздохи) и «голос» при печати текста —
// короткие мягкие щелчки своей высоты у каждого персонажа, как в визуальных новеллах.
// Браузер разрешает звук только после действия пользователя — поэтому play() может отказать,
// и тогда музыка запускается при первом клике или нажатии клавиши на экране истории.
// Громкость — через Web Audio (GainNode): Safari на iPhone не даёт менять audio.volume, поэтому
// затухание через volume там не срабатывало — музыка играла громко и не останавливалась.

const MUSIC_SRC = "/audio/music.mp3";
const SOUNDS: Record<string, string> = {
  sigh: "/audio/sigh.mp3",
  sigh_long: "/audio/sigh-long.mp3",
  sigh_male: "/audio/sigh-male.mp3",
};
// Музыка — фоном, чтобы не мешать чтению; вздохи заметно громче
const MUSIC_VOLUME = 0.008;
const SOUND_VOLUME = 0.05;
// Щелчки «голоса» — едва слышно, чтобы не утомлять за долгую сцену
const VOICE_VOLUME = 0.018;
const VOICE_DECAY_S = 0.07;
const FADE_MS = 1200;

export class StoryAudio {
  private readonly music = new Audio(MUSIC_SRC);
  private readonly sounds: Record<string, HTMLAudioElement> = {};
  private fadeTimer: ReturnType<typeof setTimeout> | undefined;
  private wantsMusic = false;
  private musicGain: GainNode | null = null;
  private voices: Record<string, number> = {};

  constructor(private muted: boolean) {
    this.music.loop = true;
    this.music.preload = "auto";
    for (const [name, src] of Object.entries(SOUNDS)) {
      const sound = new Audio(src);
      sound.preload = "auto";
      this.sounds[name] = sound;
    }
  }

  /** Запустить музыку с плавным нарастанием; если браузер не разрешил — повторить при следующем действии пользователя. */
  startMusic(): void {
    this.wantsMusic = true;
    if (this.muted) return;
    this.connect();
    sharedContext?.resume().catch(() => {});
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
    sharedContext?.resume().catch(() => {});
  }

  /** Высота «голоса» каждого персонажа в герцах. */
  setVoices(voices: Record<string, number>): void {
    this.voices = voices;
  }

  /** Короткий щелчок голоса при печати реплики; soft — тише, для мыслей. */
  talk(speaker: string, soft = false): void {
    if (this.muted) return;
    const context = audioContext();
    if (!context) return;
    if (context.state === "suspended") {
      context.resume().catch(() => {});
      return;
    }
    const now = context.currentTime;
    // Лёгкий разброс высоты — речь не звучит как метроном
    const pitch = (this.voices[speaker] ?? 200) * (0.94 + Math.random() * 0.12);
    const oscillator = context.createOscillator();
    const filter = context.createBiquadFilter();
    const gain = context.createGain();
    oscillator.type = "triangle";
    oscillator.frequency.setValueAtTime(pitch, now);
    filter.type = "lowpass";
    filter.frequency.setValueAtTime(pitch * 4, now);
    gain.gain.setValueAtTime(0, now);
    gain.gain.linearRampToValueAtTime(VOICE_VOLUME * (soft ? 0.5 : 1), now + 0.008);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + VOICE_DECAY_S);
    oscillator.connect(filter).connect(gain).connect(context.destination);
    oscillator.start(now);
    oscillator.stop(now + VOICE_DECAY_S + 0.02);
  }

  play(name: string): void {
    const sound = this.sounds[name];
    if (!sound || this.muted) return;
    this.connect();
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

  /** Уход с экрана истории: музыка затихает и встаёт на паузу, вздохи обрываются. */
  stop(): void {
    this.wantsMusic = false;
    this.fadeTo(0, () => this.music.pause());
    Object.values(this.sounds).forEach((sound) => sound.pause());
  }

  /**
   * Музыка и вздохи идут через узлы усиления общего AudioContext. Без Web Audio (очень старый браузер) —
   * громкость самих элементов. Подключение — один раз: элемент нельзя подключить к графу дважды.
   */
  private connect(): void {
    if (this.musicGain) return;
    const context = audioContext();
    if (!context) {
      this.music.volume = 0;
      Object.values(this.sounds).forEach((sound) => (sound.volume = SOUND_VOLUME));
      return;
    }
    this.musicGain = context.createGain();
    this.musicGain.gain.value = 0;
    context.createMediaElementSource(this.music).connect(this.musicGain).connect(context.destination);
    const soundGain = context.createGain();
    soundGain.gain.value = SOUND_VOLUME;
    soundGain.connect(context.destination);
    Object.values(this.sounds).forEach((sound) => context.createMediaElementSource(sound).connect(soundGain));
  }

  /** Плавно к громкости target; done — по окончании, даже если звук так и не был разрешён. */
  private fadeTo(target: number, done?: () => void): void {
    clearTimeout(this.fadeTimer);
    const gain = this.musicGain;
    if (gain) {
      const now = gain.context.currentTime;
      gain.gain.cancelScheduledValues(now);
      gain.gain.setValueAtTime(gain.gain.value, now);
      gain.gain.linearRampToValueAtTime(target, now + FADE_MS / 1000);
    } else {
      this.music.volume = target;
    }
    if (done) this.fadeTimer = setTimeout(done, gain ? FADE_MS : 0);
  }
}

// Один AudioContext на всё приложение: iOS ограничивает их число, а новелл за сессию проходят много
let sharedContext: AudioContext | null = null;

function audioContext(): AudioContext | null {
  if (!sharedContext) {
    try {
      sharedContext = new AudioContext();
    } catch {
      return null;
    }
  }
  return sharedContext;
}
