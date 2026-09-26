package ru.vsm.trainer

import androidx.lifecycle.SavedStateHandle
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.data.local.InMemoryKeyValueStore
import ru.vsm.trainer.data.remote.dto.Choice
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.repository.ActiveRunStore
import ru.vsm.trainer.presentation.player.Phase
import ru.vsm.trainer.presentation.player.PlayerViewModel

/** Часы, которые двигает тест. */
class MutableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?) = this
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val reading = Fixture.decode<RunState>("run_reading")
    private val choosing = Fixture.decode<RunState>("run_choosing")
    private val timed = Fixture.decode<RunState>("run_timed")
    private val finished = Fixture.decode<RunState>("run_finished")
    private val activeRuns = ActiveRunStore(InMemoryKeyValueStore(), testJson)
    private val events = FakeTrainerRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.player(runs: FakeRunRepository, clock: Clock = MutableClock(timed.serverTime)): PlayerViewModel {
        val vm = PlayerViewModel(SavedStateHandle(mapOf("scenarioId" to "business-seat-conflict")), runs, activeRuns, events, clock)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun readingThenChoosingThenBranch() = runTest(dispatcher) {
        val runs = FakeRunRepository(Result.success(reading)).apply {
            revealResult = Result.success(choosing)
            chooseResults += Result.success(timed)
        }
        val vm = player(runs)
        assertEquals(Phase.READING, vm.state.value.phase)
        assertEquals("прохождение запомнено для продолжения", reading.id, activeRuns.current?.runId)

        vm.revealChoices()
        advanceUntilIdle()
        assertEquals(Phase.CHOOSING, vm.state.value.phase)
        vm.choose(vm.state.value.run!!.node!!.choices.first())
        advanceUntilIdle()
        assertEquals("переход в ветку, которую выбрал сервер", "argument", vm.state.value.run?.node?.id)
    }

    @Test
    fun doubleTapSendsOneAnswer() = runTest(dispatcher) {
        val runs = FakeRunRepository(Result.success(choosing)).apply { repeat(2) { chooseResults += Result.success(timed) } }
        val vm = player(runs)
        val choice = vm.state.value.run!!.node!!.choices.first()
        vm.choose(choice)
        vm.choose(choice)
        advanceUntilIdle()
        assertEquals(1, runs.chooseCalls)
    }

    @Test
    fun choiceFromAnotherStepIsIgnored() = runTest(dispatcher) {
        val runs = FakeRunRepository(Result.success(choosing))
        val vm = player(runs)
        vm.choose(Choice("raise_voice", "чужой вариант"))
        advanceUntilIdle()
        assertEquals(0, runs.chooseCalls)
    }

    @Test
    fun lateAnswerShowsServerTimeoutBranch() = runTest(dispatcher) {
        val runs = FakeRunRepository(Result.success(timed)).apply {
            chooseResults += Result.failure(ApiError.Server(409, "time_expired", "Время истекло"))
            stateResult = Result.success(finished)
        }
        val vm = player(runs)
        vm.choose(vm.state.value.run!!.node!!.choices.first())
        advanceUntilIdle()
        assertEquals("состояние перечитано с сервера", Phase.FINISHED, vm.state.value.phase)
        assertEquals("Время на решение истекло — ответ не принят.", vm.state.value.notice)
        assertNull("завершённое прохождение больше не предлагается продолжить", activeRuns.current)
    }

    @Test
    fun offlineAnswerCanBeRetried() = runTest(dispatcher) {
        val runs = FakeRunRepository(Result.success(choosing)).apply {
            chooseResults += Result.failure(ApiError.Offline)
            chooseResults += Result.success(timed)
        }
        val vm = player(runs)
        val choice = vm.state.value.run!!.node!!.choices.first()
        vm.choose(choice)
        advanceUntilIdle()
        assertEquals(Phase.CHOOSING, vm.state.value.phase)
        assertEquals(ApiError.Offline.message, vm.state.value.notice)
        vm.choose(choice)
        advanceUntilIdle()
        assertEquals("argument", vm.state.value.run?.node?.id)
    }

    @Test
    fun countdownUsesServerClock() = runTest(dispatcher) {
        // Телефон отстаёт от сервера на 100 с: остаток всё равно считается по серверу
        val clock = MutableClock(timed.serverTime.minusSeconds(100))
        val vm = player(FakeRunRepository(Result.success(timed)), clock)
        assertEquals(20.0, vm.remainingSeconds()!!, 0.01)
        clock.now = clock.now.plusSeconds(15)
        assertEquals(5.0, vm.remainingSeconds()!!, 0.01)
    }

    @Test
    fun expiredTimerAsksServerForConsequences() = runTest(dispatcher) {
        val clock = MutableClock(timed.serverTime)
        val runs = FakeRunRepository(Result.success(timed)).apply { stateResult = Result.success(finished) }
        val vm = player(runs, clock)
        vm.tick()
        advanceUntilIdle()
        assertEquals("время ещё не вышло", 0, runs.stateCalls)

        clock.now = timed.node!!.timeoutAt!!.plusMillis(100)
        vm.tick()
        advanceUntilIdle()
        assertEquals(1, runs.stateCalls)
        assertEquals(Phase.FINISHED, vm.state.value.phase)
    }

    @Test
    fun exitIsReportedOnlyForUnfinishedRun() = runTest(dispatcher) {
        val vm = player(FakeRunRepository(Result.success(reading)))
        vm.exit()
        advanceUntilIdle()
        assertEquals(listOf("run_exited"), events.events)

        val done = player(FakeRunRepository(Result.success(finished)))
        done.exit()
        advanceUntilIdle()
        assertEquals(1, events.events.size)
    }

    @Test
    fun startFailureIsShown() = runTest(dispatcher) {
        val vm = player(FakeRunRepository(Result.failure(ApiError.Server(404, "scenario_not_found", "Сценарий не найден"))))
        assertEquals(Phase.FAILED, vm.state.value.phase)
        assertEquals("Сценарий не найден", vm.state.value.error)
    }
}
