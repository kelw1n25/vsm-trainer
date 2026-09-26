package ru.vsm.trainer.domain.story

import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.data.local.HistoryEntry
import ru.vsm.trainer.data.local.RunProgress
import ru.vsm.trainer.data.local.StoryMemory
import ru.vsm.trainer.data.remote.dto.Line
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.Scene
import ru.vsm.trainer.data.repository.RunRepository

enum class StoryPhase { LOADING, INTRO, DIALOGUE, CHOICES, ENDING, ERROR }

/** Всё, что нужно экрану новеллы, — снимок состояния движка. */
data class StoryView(
    val phase: StoryPhase = StoryPhase.LOADING,
    val run: RunState? = null,
    val scene: Scene? = null,
    /** Текущее выражение лица каждого персонажа сцены. */
    val expressions: Map<String, String> = emptyMap(),
    val line: Line? = null,
    /** Сколько символов реплики уже напечатано. */
    val shownChars: Int = 0,
    val typing: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val auto: Boolean = false,
    /** Сцену уже видели в прошлых прохождениях — её можно пропустить до выбора. */
    val canSkip: Boolean = false,
    val busy: Boolean = false,
    val notice: String? = null,
    /** Разница серверных и клиентских часов — таймер считается по серверному времени. */
    val clockOffsetMs: Long = 0,
)

/** Кто проигрывает звуки реплик — движок не знает, как именно. */
interface SoundPlayer {
    fun play(name: String)
    /** «Голос» персонажа во время печати реплики; soft — тише, для мыслей. */
    fun talk(speaker: String, soft: Boolean)
}

/**
 * Движок визуальной новеллы — перенос `frontend/src/story/StoryEngine.ts` один к одному.
 *
 * Сервер решает, что происходит в истории: какие варианты доступны, куда ведёт выбор, когда истекает таймер.
 * Движок отвечает за подачу: очередь реплик, печать текста, смену сцен, журнал, автопрокрутку,
 * пропуск уже виденного и сохранение места, где игрок остановился.
 */
class StoryEngine(
    private val scenarioId: String,
    private val employeeId: Int,
    private val playerName: String,
    private val runs: RunRepository,
    private val memory: StoryMemory,
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val reduceMotion: () -> Boolean,
    private val sounds: SoundPlayer? = null,
) {
    private class Segment(val scene: Scene, val nodeId: String?, val lines: List<Line>)

    private val _view = MutableStateFlow(StoryView(auto = memory.auto(employeeId)))
    val view: StateFlow<StoryView> = _view.asStateFlow()

    private var queue: List<Segment> = emptyList()
    private var segmentIndex = 0
    private var lineIndex = 0
    private var seenBefore: Set<String> = emptySet()
    private var choices: List<String> = emptyList()
    private var typingJob: Job? = null
    private var autoJob: Job? = null
    private var introJob: Job? = null
    private var timeoutJob: Job? = null

    private val runId: String? get() = _view.value.run?.id

    private fun update(block: (StoryView) -> StoryView) = _view.update(block)

    private fun setRun(run: RunState) {
        update { it.copy(run = run, clockOffsetMs = run.serverTime.toEpochMilli() - clock.millis()) }
    }

    /** Старт или продолжение: сервер вернёт незавершённое прохождение сценария, если оно есть. */
    fun start() {
        scope.launch {
            val run = try {
                runs.start(scenarioId)
            } catch (error: ApiError) {
                update { it.copy(phase = StoryPhase.ERROR, notice = error.message) }
                return@launch
            }
            open(run)
        }
    }

    private fun open(run: RunState) {
        setRun(run)
        seenBefore = memory.scenario(employeeId, run.scenarioId).seenNodes.toSet()
        val progress = memory.progress(employeeId, run.id)
        choices = progress?.choices ?: emptyList()
        update { it.copy(history = progress?.history ?: emptyList()) }

        if (run.status != RunStatus.IN_PROGRESS) {
            finish()
            return
        }
        val node = run.node ?: return
        if (node.choicesShown) {
            // Варианты уже показаны и таймер идёт — сразу к выбору
            update { it.copy(scene = node.scene, expressions = expressionsAfter(node.scene, node.dialogue)) }
            showChoices()
            return
        }
        queue = listOf(Segment(node.scene, node.id, node.dialogue))
        val resumeAt = if (progress?.nodeId == node.id) minOf(progress.lineIndex, node.dialogue.size - 1).coerceAtLeast(0) else 0
        if (progress != null) {
            // Реплика, на которой остановились, уже есть в журнале — покажем её снова, но не задвоим запись
            if (progress.history.lastOrNull()?.text == node.dialogue.getOrNull(resumeAt)?.text) {
                update { it.copy(history = progress.history.dropLast(1)) }
            }
            playSegment(0, resumeAt)
            return
        }
        // Новое прохождение: затемнение, название сценария, затем первая сцена
        update { it.copy(phase = StoryPhase.INTRO) }
        introJob = scope.launch {
            delay(if (reduceMotion()) 300 else INTRO_MS)
            playSegment(0, 0)
        }
    }

    /** Очередь показа из ответа сервера: реакция на решение, затем новая сцена или финал. */
    private fun loadScene(run: RunState, previousScene: Scene?) {
        setRun(run)
        val segments = mutableListOf<Segment>()
        for (step in run.lastSteps) {
            if (step.isTimeout) log(HistoryEntry("timeout", null, "Время на решение истекло"))
            if (step.reaction.isNotEmpty() && previousScene != null) segments += Segment(previousScene, null, step.reaction)
        }
        run.node?.let { segments += Segment(it.scene, it.id, it.dialogue) }
            ?: run.final?.let { segments += Segment(it.scene, null, it.dialogue) }
        queue = segments
        playSegment(0, 0)
    }

    private fun playSegment(index: Int, fromLine: Int) {
        introJob?.cancel()
        segmentIndex = index
        val segment = queue.getOrNull(index)
        if (segment == null) {
            endOfQueue()
            return
        }
        val skippable = segment.nodeId != null && segment.nodeId in seenBefore
        val run = _view.value.run
        if (segment.nodeId != null && run != null) memory.rememberScene(employeeId, run.scenarioId, segment.nodeId)
        update {
            it.copy(
                phase = StoryPhase.DIALOGUE,
                scene = segment.scene,
                expressions = expressionsAfter(segment.scene, segment.lines.take(fromLine)),
                canSkip = skippable,
                notice = null,
            )
        }
        if (segment.lines.isEmpty()) {
            playSegment(index + 1, 0)
            return
        }
        showLine(fromLine)
    }

    private fun showLine(index: Int) {
        val segment = queue[segmentIndex]
        lineIndex = index
        val line = segment.lines[index]
        log(HistoryEntry(line.kind, speakerName(line), line.text))
        update {
            val expressions = line.expression?.let { e -> it.expressions + (line.speaker to e) } ?: it.expressions
            it.copy(line = line, expressions = expressions, shownChars = 0, typing = true)
        }
        saveProgress()
        line.sound?.let { sounds?.play(it) }

        typingJob?.cancel()
        autoJob?.cancel()
        if (reduceMotion()) {
            finishTyping()
            return
        }
        typingJob = scope.launch {
            var shown = 0
            while (true) {
                delay(TYPE_INTERVAL_MS)
                shown += 1
                if (shown >= line.text.length) break
                val char = line.text[shown - 1]
                if (line.kind != "narration" && shown % TALK_EVERY_CHARS == 0 && char.isLetterOrDigit()) {
                    sounds?.talk(line.speaker, line.kind == "thought")
                }
                update { it.copy(shownChars = shown) }
                // Пауза после конца предложения — реплика «дышит», как в живой речи
                if (char in ".!?…" && line.text.getOrNull(shown) == ' ') delay(TYPE_INTERVAL_MS * SENTENCE_PAUSE_TICKS)
            }
            finishTyping()
        }
    }

    private fun finishTyping() {
        typingJob?.cancel()
        val line = _view.value.line
        update { it.copy(shownChars = line?.text?.length ?: 0, typing = false) }
        if (_view.value.auto && line != null) {
            autoJob = scope.launch {
                delay(minOf(4000L, AUTO_BASE_MS + line.text.length * AUTO_PER_CHAR_MS))
                advance()
            }
        }
    }

    /** «Далее» или касание сцены: допечатать реплику сразу, а если она уже напечатана — следующая. */
    fun advance() {
        val view = _view.value
        if (view.phase == StoryPhase.INTRO) {
            playSegment(0, lineIndex)
            return
        }
        if (view.phase != StoryPhase.DIALOGUE) return
        if (view.typing) {
            finishTyping()
            return
        }
        autoJob?.cancel()
        val segment = queue[segmentIndex]
        if (lineIndex + 1 < segment.lines.size) showLine(lineIndex + 1) else playSegment(segmentIndex + 1, 0)
    }

    /** Пропустить уже знакомую сцену до момента выбора. */
    fun skip() {
        if (!_view.value.canSkip || _view.value.phase != StoryPhase.DIALOGUE) return
        typingJob?.cancel()
        autoJob?.cancel()
        val segment = queue[segmentIndex]
        segment.lines.drop(lineIndex + 1).forEach { log(HistoryEntry(it.kind, speakerName(it), it.text)) }
        update { it.copy(expressions = expressionsAfter(segment.scene, segment.lines), typing = false) }
        lineIndex = segment.lines.size - 1
        playSegment(segmentIndex + 1, 0)
    }

    fun toggleAuto() {
        val auto = !_view.value.auto
        memory.saveAuto(employeeId, auto)
        update { it.copy(auto = auto) }
        if (auto && _view.value.phase == StoryPhase.DIALOGUE && !_view.value.typing) advance()
        if (!auto) autoJob?.cancel()
    }

    private fun endOfQueue() {
        val run = _view.value.run
        when {
            run?.node != null -> showChoices()
            run?.final != null -> finish()
        }
    }

    /** Сцена дочитана: сервер показывает варианты и запускает таймер. */
    private fun showChoices() {
        val run = _view.value.run ?: return
        val node = run.node ?: return
        autoJob?.cancel()
        update { it.copy(phase = StoryPhase.CHOICES, line = null, typing = false, canSkip = false) }
        if (node.choicesShown) {
            saveProgress()
            watchTimeout()
            return
        }
        update { it.copy(busy = true) }
        scope.launch {
            try {
                setRun(runs.reveal(run.id, node.id))
                update { it.copy(busy = false) }
                saveProgress()
                watchTimeout()
            } catch (error: ApiError) {
                update { it.copy(busy = false) }
                recover(error)
            }
        }
    }

    /** Истечение таймера применяет сервер — в момент дедлайна спрашиваем, что произошло. */
    private fun watchTimeout() {
        timeoutJob?.cancel()
        val timeoutAt = _view.value.run?.node?.timeoutAt ?: return
        val serverNow = clock.instant().plusMillis(_view.value.clockOffsetMs)
        val wait = Duration.between(serverNow, timeoutAt).toMillis() + 300
        timeoutJob = scope.launch {
            delay(wait.coerceAtLeast(0))
            refresh()
        }
    }

    private suspend fun refresh() {
        val before = _view.value.run ?: return
        try {
            val run = runs.state(before.id)
            if (run.node?.id == before.node?.id && run.status == RunStatus.IN_PROGRESS) {
                // Сервер ещё не считает время истёкшим (задержка сети) — подождём ещё
                setRun(run)
                watchTimeout()
            } else {
                loadScene(run, _view.value.scene)
            }
        } catch (error: ApiError) {
            update { it.copy(notice = error.message) }
        }
    }

    /** Решение игрока: сервер применяет последствия, история продолжается реакцией персонажей. */
    fun choose(choiceId: String) {
        val view = _view.value
        val run = view.run ?: return
        val node = run.node ?: return
        if (view.busy || view.phase != StoryPhase.CHOICES) return
        val choice = node.choices.firstOrNull { it.id == choiceId } ?: return
        timeoutJob?.cancel()
        // Флаг «занят» ставится до запроса: второе касание ничего не отправит
        update { it.copy(busy = true) }
        scope.launch {
            try {
                val next = runs.choose(run.id, node.id, choiceId)
                choices = choices + choiceId
                log(HistoryEntry("choice", playerName, choice.text))
                update { it.copy(busy = false) }
                loadScene(next, _view.value.scene)
            } catch (error: ApiError) {
                update { it.copy(busy = false) }
                recover(error)
            }
        }
    }

    /** Время вышло или шаг сменился (повтор после обрыва, второе устройство) — перечитываем состояние с сервера. */
    private suspend fun recover(error: ApiError) {
        update { it.copy(notice = error.message) }
        if (error.code in setOf("time_expired", "stale_node", "run_finished", "choices_not_shown")) refresh()
    }

    private fun finish() {
        timeoutJob?.cancel()
        autoJob?.cancel()
        val run = _view.value.run
        run?.final?.let { memory.rememberEnding(employeeId, run.scenarioId, run.id, it.ending) }
        update { it.copy(phase = StoryPhase.ENDING, line = null, typing = false, canSkip = false) }
    }

    /** «Пройти заново»: сервер создаёт новое прохождение того же сценария, и история начинается сначала. */
    fun restart() {
        dispose()
        queue = emptyList()
        update { StoryView(auto = it.auto) }
        start()
    }

    private fun saveProgress() {
        val run = _view.value.run ?: return
        memory.saveProgress(
            employeeId, run.id,
            RunProgress(
                scenarioId = run.scenarioId,
                nodeId = run.node?.id.orEmpty(),
                lineIndex = if (queue.getOrNull(segmentIndex)?.nodeId != null) lineIndex else 0,
                history = _view.value.history,
                choices = choices,
                ending = memory.progress(employeeId, run.id)?.ending,
            ),
        )
    }

    fun dispose() {
        typingJob?.cancel()
        autoJob?.cancel()
        introJob?.cancel()
        timeoutJob?.cancel()
    }

    /** Остаток таймера в миллисекундах по серверным часам; null — таймера нет. */
    fun remainingMs(): Long? {
        val deadline = _view.value.run?.node?.deadlineAt ?: return null
        return (deadline.toEpochMilli() - _view.value.clockOffsetMs - clock.millis()).coerceAtLeast(0)
    }

    private fun log(entry: HistoryEntry) = update { it.copy(history = it.history + entry) }

    private fun speakerName(line: Line): String? = when {
        line.kind == "narration" -> null
        line.speaker == "player" -> playerName
        else -> line.name
    }

    private fun expressionsAfter(scene: Scene, lines: List<Line>): Map<String, String> {
        val expressions = scene.characters.associate { it.id to it.expression }.toMutableMap()
        for (line in lines) line.expression?.let { expressions[line.speaker] = it }
        return expressions
    }

    companion object {
        const val TYPE_INTERVAL_MS = 28L
        const val SENTENCE_PAUSE_TICKS = 9
        const val INTRO_MS = 2400L
        const val AUTO_BASE_MS = 1100L
        const val AUTO_PER_CHAR_MS = 32L
        // Щелчок голоса — на каждый третий напечатанный символ, кроме пробелов и знаков препинания
        const val TALK_EVERY_CHARS = 3
    }
}
