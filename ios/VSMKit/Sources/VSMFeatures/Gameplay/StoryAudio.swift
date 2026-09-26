import AVFoundation
import VSMCore

/// Звук новеллы — как `story/audio.ts` сайта: тихая музыка по кругу с плавным нарастанием, вздохи персонажей
/// и «голос» при печати — короткий мягкий щелчок своей высоты у каждого персонажа.
@MainActor
final class StoryAudio: StorySoundPlayer {
    // Соотношение как на сайте (вздохи заметно громче музыки, «голос» тише вздохов), но уровни — под динамик
    // телефона: громкости сайта (0.008 / 0.05 / 0.018) рассчитаны на браузер и наушники и на телефоне не слышны
    private static let musicVolume: Float = 0.1
    private static let soundVolume: Float = 0.6
    private static let voiceVolume: Float = 0.12
    private static let voiceDecay = 0.07
    private static let fade: TimeInterval = 1.2

    private var music: AVAudioPlayer?
    private var sounds: [String: AVAudioPlayer] = [:]
    private let engine = AVAudioEngine()
    private let voice = AVAudioPlayerNode()
    private let format = AVAudioFormat(standardFormatWithSampleRate: 22050, channels: 1)
    private var voices: [String: Double] = [:]
    private(set) var muted: Bool

    init(muted: Bool) {
        self.muted = muted
        #if os(iOS)
        // «Окружающий» звук: не прерывает музыку пользователя и молчит при беззвучном режиме
        try? AVAudioSession.sharedInstance().setCategory(.ambient)
        #endif
        if let format {
            engine.attach(voice)
            engine.connect(voice, to: engine.mainMixerNode, format: format)
        }
    }

    private static func url(_ name: String) -> URL? {
        Bundle.module.url(forResource: name.replacingOccurrences(of: "_", with: "-"), withExtension: "mp3", subdirectory: "Resources/Audio")
    }

    /// Высота «голоса» персонажа в герцах: детский и женский выше, мужской ниже, у проводника — ровный средний.
    func setVoices(for characters: [Character]) {
        var voices = ["player": 190.0]
        for character in characters {
            voices[character.id] = character.look.child ? 330 : (character.look.hairStyle == "short" ? 150 : 250)
        }
        self.voices = voices
    }

    /// Музыка с плавным нарастанием до громкости сайта.
    func startMusic() {
        guard !muted, music == nil, let url = Self.url("music"), let player = try? AVAudioPlayer(contentsOf: url) else { return }
        player.numberOfLoops = -1
        player.volume = 0
        player.play()
        player.setVolume(Self.musicVolume, fadeDuration: Self.fade)
        music = player
    }

    func stopMusic() {
        guard let player = music else { return }
        music = nil
        player.setVolume(0, fadeDuration: Self.fade)
        Task {
            try? await Task.sleep(for: .seconds(Self.fade))
            player.stop()
        }
    }

    func setMuted(_ muted: Bool) {
        self.muted = muted
        if muted { stopMusic() } else { startMusic() }
    }

    func play(_ name: String) {
        guard !muted else { return }
        if sounds[name] == nil, let url = Self.url(name), let player = try? AVAudioPlayer(contentsOf: url) {
            player.volume = Self.soundVolume
            sounds[name] = player
        }
        sounds[name]?.currentTime = 0
        sounds[name]?.play()
    }

    func talk(speaker: String, soft: Bool) {
        guard !muted, let format else { return }
        if !engine.isRunning {
            guard (try? engine.start()) != nil else { return }
            voice.play()
        }
        // Лёгкий разброс высоты — речь не звучит как метроном
        let pitch = (voices[speaker] ?? 200) * Double.random(in: 0.94...1.06)
        guard let buffer = Self.blip(pitch: pitch, volume: soft ? Self.voiceVolume * 0.5 : Self.voiceVolume, format: format) else { return }
        voice.scheduleBuffer(buffer)
    }

    /// Треугольная волна с быстрой атакой и экспоненциальным затуханием, как осциллятор WebAudio на сайте.
    private static func blip(pitch: Double, volume: Float, format: AVAudioFormat) -> AVAudioPCMBuffer? {
        let rate = format.sampleRate
        let frames = AVAudioFrameCount(rate * (voiceDecay + 0.02))
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames), let samples = buffer.floatChannelData?[0] else { return nil }
        buffer.frameLength = frames
        for index in 0..<Int(frames) {
            let t = Double(index) / rate
            let triangle = 2 / Double.pi * asin(sin(2 * Double.pi * pitch * t))
            let envelope = t < 0.008 ? t / 0.008 : exp(-(t - 0.008) / (voiceDecay / 6))
            samples[index] = Float(triangle * envelope) * volume
        }
        return buffer
    }

    func release() {
        stopMusic()
        voice.stop()
        engine.stop()
    }
}
