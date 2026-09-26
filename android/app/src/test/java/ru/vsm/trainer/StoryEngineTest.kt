package ru.vsm.trainer

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.data.local.HistoryEntry
import ru.vsm.trainer.data.local.InMemoryKeyValueStore
import ru.vsm.trainer.data.local.RunProgress
import ru.vsm.trainer.data.local.StoryMemory
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.domain.story.StoryEngine
import ru.vsm.trainer.domain.story.StoryPhase

/** Часы, которые двигает тест. */
class MutableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?) = this
}

/**
 * Движок новеллы — те же правила, что у `StoryEngine.ts` сайта: заставка, печать, «Далее», показ вариантов,
 * решение, реакция персонажей, таймаут по серверным часам, восстановление после отказов, финал.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoryEngineTest {
    private val reading = Fixture.decode<RunState>("run_reading")
    private val choosing = Fixture.decode<RunState>("run_choosing")
    private val timed = Fixture.decode<RunState>("run_timed")
    private val finished = Fixture.decode<RunState>("run_finished")
    private val memory = StoryMemory(InMemoryKeyValueStore(), testJson)

    private fun TestScope.engine(runs: FakeRunRepository, reduceMotion: Boolean = true, clock: Clock = MutableClock(timed.serverTime)) =
        StoryEngine("business-seat-conflict", 2, "Юлия", runs, memory, backgroundScope, clock, { reduceMotion })

    /** Дочитать сцену до выбора. */
    private fun TestScope.readToChoices(engine: StoryEngine) {
        repeat(20) {
            if (engine.view.value.phase != StoryPhase.DIALOGUE) return
            engine.advance()
            runCurrent()
        }
    }

    @Test
    fun newRunShowsIntroThenFirstLine() = runTest {
        val engine = engine(FakeRunRepository(Result.success(reading)))
        engine.start()
        runCurrent()
        assertEquals(StoryPhase.INTRO, engine.view.value.phase)
        advanceTimeBy(301)
        assertEquals(StoryPhase.DIALOGUE, engine.view.value.phase)
        assertEquals(reading.node!!.dialogue.first().text, engine.view.value.line?.text)
    }

    @Test
    fun dialogueLeadsToChoicesThroughReveal() = runTest {
        val runs = FakeRunRepository(Result.success(reading)).apply { revealResult = Result.success(choosing) }
        val engine = engine(runs)
        engine.start()
        advanceTimeBy(301)
        readToChoices(engine)
        advanceTimeBy(1_000)
        assertEquals(StoryPhase.CHOICES, engine.view.value.phase)
        assertTrue("варианты пришли только после reveal", engine.view.value.run!!.node!!.choices.isNotEmpty())
        assertEquals(reading.node!!.dialogue.size, engine.view.value.history.size)
    }

    @Test
    fun typingPrintsCharacterByCharacterAndTapCompletesLine() = runTest {
        val engine = engine(FakeRunRepository(Result.success(reading)), reduceMotion = false)
        engine.start()
        advanceTimeBy(StoryEngine.INTRO_MS + 1)
        advanceTimeBy(StoryEngine.TYPE_INTERVAL_MS * 5 + 1)
        assertTrue(engine.view.value.typing)
        assertEquals(5, engine.view.value.shownChars)
        engine.advance()
        assertFalse("касание допечатывает реплику сразу", engine.view.value.typing)
        assertEquals(engine.view.value.line!!.text.length, engine.view.value.shownChars)
    }

    @Test
    fun choiceShowsReactionThenNextScene() = runTest {
        val runs = FakeRunRepository(Result.success(choosing)).apply { chooseResults += Result.success(finished) }
        val engine = engine(runs)
        engine.start()
        runCurrent()
        assertEquals(StoryPhase.CHOICES, engine.view.value.phase)
        engine.choose(choosing.node!!.choices.first().id)
        advanceTimeBy(100)
        val reaction = finished.lastSteps.first().reaction.first()
        assertEquals("сначала реакция персонажей на решение", reaction.text, engine.view.value.line?.text)
        assertTrue(engine.view.value.history.any { it.kind == "choice" && it.speaker == "Юлия" })
    }

    @Test
    fun doubleTapSendsOneAnswer() = runTest {
        val runs = FakeRunRepository(Result.success(choosing)).apply { repeat(2) { chooseResults += Result.success(timed) } }
        val engine = engine(runs)
        engine.start()
        runCurrent()
        val choice = choosing.node!!.choices.first().id
        engine.choose(choice)
        engine.choose(choice)
        advanceTimeBy(1_000)
        assertEquals(1, runs.chooseCalls)
    }

    @Test
    fun lateAnswerRefreshesFromServer() = runTest {
        val runs = FakeRunRepository(Result.success(timed)).apply {
            chooseResults += Result.failure(ApiError.Server(409, "time_expired", "Время на решение истекло"))
            stateResult = Result.success(finished)
        }
        val engine = engine(runs)
        engine.start()
        runCurrent()
        engine.choose(timed.node!!.choices.first().id)
        advanceTimeBy(100)
        assertEquals("состояние перечитано с сервера", 1, runs.stateCalls)
        assertEquals("показываются последствия, которые применил сервер", StoryPhase.DIALOGUE, engine.view.value.phase)
    }

    @Test
    fun offlineAnswerKeepsChoices() = runTest {
        val runs = FakeRunRepository(Result.success(choosing)).apply { chooseResults += Result.failure(ApiError.Offline) }
        val engine = engine(runs)
        engine.start()
        runCurrent()
        engine.choose(choosing.node!!.choices.first().id)
        advanceTimeBy(100)
        assertEquals(StoryPhase.CHOICES, engine.view.value.phase)
        assertEquals(ApiError.Offline.message, engine.view.value.notice)
        assertFalse(engine.view.value.busy)
    }

    @Test
    fun expiredTimerAsksServerAtDeadline() = runTest {
        val runs = FakeRunRepository(Result.success(timed)).apply { stateResult = Result.success(finished) }
        val engine = engine(runs)
        engine.start()
        runCurrent()
        val wait = timed.node!!.timeoutAt!!.toEpochMilli() - timed.serverTime.toEpochMilli() + 300
        advanceTimeBy(wait - 50)
        assertEquals("время ещё не вышло", 0, runs.stateCalls)
        advanceTimeBy(100)
        assertEquals(1, runs.stateCalls)
    }

    @Test
    fun countdownUsesServerClock() = runTest {
        // Телефон отстаёт от сервера на 100 с: остаток всё равно считается по серверу
        val clock = MutableClock(timed.serverTime.minusSeconds(100))
        val engine = engine(FakeRunRepository(Result.success(timed)), clock = clock)
        engine.start()
        runCurrent()
        assertEquals(20_000L, engine.remainingMs())
        clock.now = clock.now.plusSeconds(15)
        assertEquals(5_000L, engine.remainingMs())
    }

    @Test
    fun resumesFromSavedLineWithoutIntro() = runTest {
        memory.saveProgress(2, reading.id, RunProgress(reading.scenarioId, reading.node!!.id, 2, listOf(HistoryEntry("narration", null, "…")), emptyList()))
        val engine = engine(FakeRunRepository(Result.success(reading)))
        engine.start()
        runCurrent()
        assertEquals(StoryPhase.DIALOGUE, engine.view.value.phase)
        assertEquals(reading.node!!.dialogue[2].text, engine.view.value.line?.text)
    }

    @Test
    fun seenSceneCanBeSkipped() = runTest {
        memory.rememberScene(2, reading.scenarioId, reading.node!!.id)
        val runs = FakeRunRepository(Result.success(reading)).apply { revealResult = Result.success(choosing) }
        val engine = engine(runs)
        engine.start()
        advanceTimeBy(301)
        assertTrue(engine.view.value.canSkip)
        engine.skip()
        advanceTimeBy(1_000)
        assertEquals(StoryPhase.CHOICES, engine.view.value.phase)
    }

    @Test
    fun finishedRunShowsEndingAndCountsPlaythrough() = runTest {
        val engine = engine(FakeRunRepository(Result.success(finished)))
        engine.start()
        runCurrent()
        assertEquals(StoryPhase.ENDING, engine.view.value.phase)
        assertEquals(1, memory.scenario(2, finished.scenarioId).playthroughs)
        assertTrue(finished.final!!.ending in memory.scenario(2, finished.scenarioId).endings)
    }

    @Test
    fun startFailureIsShown() = runTest {
        val engine = engine(FakeRunRepository(Result.failure(ApiError.Server(404, "scenario_not_found", "Сценарий не найден"))))
        engine.start()
        runCurrent()
        assertEquals(StoryPhase.ERROR, engine.view.value.phase)
        assertEquals("Сценарий не найден", engine.view.value.notice)
    }
}
