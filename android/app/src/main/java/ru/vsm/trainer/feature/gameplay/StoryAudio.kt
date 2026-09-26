package ru.vsm.trainer.feature.gameplay

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.vsm.trainer.R
import ru.vsm.trainer.domain.story.SoundPlayer

/**
 * Звук новеллы — как `story/audio.ts` сайта: тихая музыка по кругу с плавным нарастанием, вздохи персонажей
 * и «голос» при печати — короткий мягкий щелчок своей высоты у каждого персонажа. Громкости те же, что на сайте.
 */
class StoryAudio @Inject constructor(@ApplicationContext private val context: Context) : SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var music: MediaPlayer? = null
    private var fade: Job? = null
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    private val sounds = mapOf(
        "sigh" to pool.load(context, R.raw.sigh, 1),
        "sigh_long" to pool.load(context, R.raw.sigh_long, 1),
        "sigh_male" to pool.load(context, R.raw.sigh_male, 1),
    )
    private var muted = false
    private var voices: Map<String, Double> = emptyMap()

    /** Музыка с плавным нарастанием до MUSIC_VOLUME. */
    fun startMusic() {
        if (muted || music != null) return
        music = MediaPlayer.create(context, R.raw.music)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }
        fadeTo(MUSIC_VOLUME)
    }

    fun stopMusic() {
        fadeTo(0f) {
            music?.release()
            music = null
        }
    }

    /** Высота «голоса» персонажа в герцах: детский и женский выше, мужской ниже, у проводника — ровный средний. */
    fun setVoices(voices: Map<String, Double>) {
        this.voices = voices
    }

    fun setMuted(muted: Boolean) {
        this.muted = muted
        if (muted) stopMusic() else startMusic()
    }

    override fun play(name: String) {
        if (muted) return
        sounds[name]?.let { pool.play(it, SOUND_VOLUME, SOUND_VOLUME, 1, 0, 1f) }
    }

    override fun talk(speaker: String, soft: Boolean) {
        if (muted) return
        // Лёгкий разброс высоты — речь не звучит как метроном
        val pitch = (voices[speaker] ?: 200.0) * (0.94 + Random.nextDouble() * 0.12)
        scope.launch { blip(pitch, if (soft) VOICE_VOLUME * 0.5f else VOICE_VOLUME) }
    }

    /** Треугольная волна с быстрой атакой и экспоненциальным затуханием, как осциллятор WebAudio на сайте. */
    private fun blip(pitch: Double, volume: Float) {
        val samples = (SAMPLE_RATE * (VOICE_DECAY_S + 0.02)).toInt()
        val data = ShortArray(samples) { index ->
            val t = index / SAMPLE_RATE.toDouble()
            val triangle = 2 / PI * asin(sin(2 * PI * pitch * t))
            val envelope = if (t < 0.008) t / 0.008 else exp(-(t - 0.008) / (VOICE_DECAY_S / 6))
            (triangle * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(data, 0, samples)
        track.setNotificationMarkerPosition(samples - 1)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) = track.release()
            override fun onPeriodicNotification(track: AudioTrack) = Unit
        })
        track.play()
    }

    private fun fadeTo(target: Float, done: () -> Unit = {}) {
        fade?.cancel()
        fade = scope.launch {
            var volume = if (target > 0f) 0f else MUSIC_VOLUME
            val step = (target - volume) / (FADE_MS / 50)
            while (abs(target - volume) > abs(step) / 2 && step != 0f) {
                volume += step
                music?.setVolume(volume, volume)
                delay(50)
            }
            music?.setVolume(target, target)
            done()
        }
    }

    fun release() {
        stopMusic()
        scope.launch {
            delay(FADE_MS.toLong() + 100)
            pool.release()
        }
    }

    private companion object {
        const val MUSIC_VOLUME = 0.008f
        const val SOUND_VOLUME = 0.05f
        const val VOICE_VOLUME = 0.018f
        const val VOICE_DECAY_S = 0.07
        const val FADE_MS = 1200f
        const val SAMPLE_RATE = 22050
    }
}
