import XCTest
@testable import VSMCore

@MainActor
final class ScenarioPlayerTests: XCTestCase {
    let reading = Fixture.decode(RunState.self, "run_reading")
    let choosing = Fixture.decode(RunState.self, "run_choosing")
    let timed = Fixture.decode(RunState.self, "run_timed")
    let finished = Fixture.decode(RunState.self, "run_finished")
    var activeRuns = ActiveRunStore(store: InMemoryKeyValueStore())
    var events = FakeTrainerRepository()

    func player(_ runs: FakeRunRepository, now: @escaping () -> Date = Date.init) -> ScenarioPlayerViewModel {
        ScenarioPlayerViewModel(scenarioId: "business-seat-conflict", runs: runs, activeRuns: activeRuns, events: events, now: now)
    }

    func testReadingThenChoosingThenBranch() async {
        let runs = FakeRunRepository(start: reading)
        runs.revealResult = .success(choosing)
        runs.chooseResults = [.success(timed)]
        let vm = player(runs)

        await vm.load()
        XCTAssertEqual(vm.phase, .reading)
        XCTAssertEqual(activeRuns.current?.runId, reading.id, "прохождение запомнено для продолжения")
        await vm.revealChoices()
        XCTAssertEqual(vm.phase, .choosing)
        await vm.choose(vm.node!.choices[0])
        XCTAssertEqual(vm.node?.id, "argument", "переход в ветку, которую выбрал сервер")
    }

    func testDoubleTapSendsOneAnswer() async {
        let runs = FakeRunRepository(start: choosing)
        runs.chooseResults = [.success(timed), .success(timed)]
        let vm = player(runs)
        await vm.load()
        let choice = vm.node!.choices[0]

        async let first: Void = vm.choose(choice)
        async let second: Void = vm.choose(choice)
        _ = await (first, second)
        XCTAssertEqual(runs.chooseCalls, 1)
    }

    func testChoiceFromAnotherStepIsIgnored() async {
        let runs = FakeRunRepository(start: choosing)
        let vm = player(runs)
        await vm.load()
        await vm.choose(Choice(id: "raise_voice", text: "чужой вариант"))
        XCTAssertEqual(runs.chooseCalls, 0)
    }

    func testLateAnswerShowsServerTimeoutBranch() async {
        let runs = FakeRunRepository(start: timed)
        runs.chooseResults = [.failure(APIError.server(status: 409, code: "time_expired", message: "Время истекло"))]
        runs.stateResult = .success(finished)
        let vm = player(runs)
        await vm.load()
        await vm.choose(vm.node!.choices[0])
        XCTAssertEqual(vm.phase, .finished, "состояние перечитано с сервера")
        XCTAssertEqual(vm.notice, "Время на решение истекло — ответ не принят.")
    }

    func testDuplicateAfterReconnectRefreshesState() async {
        let runs = FakeRunRepository(start: choosing)
        runs.chooseResults = [.failure(APIError.server(status: 409, code: "stale_node", message: "Ответ уже принят"))]
        runs.stateResult = .success(timed)
        let vm = player(runs)
        await vm.load()
        await vm.choose(vm.node!.choices[0])
        XCTAssertEqual(vm.node?.id, "argument")
        XCTAssertEqual(vm.phase, .choosing, "на сервере варианты этого шага уже показаны")
    }

    func testOfflineAnswerCanBeRetried() async {
        let runs = FakeRunRepository(start: choosing)
        runs.chooseResults = [.failure(APIError.offline), .success(timed)]
        let vm = player(runs)
        await vm.load()
        let choice = vm.node!.choices[0]
        await vm.choose(choice)
        XCTAssertEqual(vm.phase, .choosing)
        XCTAssertEqual(vm.notice, APIError.offline.userMessage)
        await vm.choose(choice)
        XCTAssertEqual(vm.node?.id, "argument")
    }

    func testCountdownUsesServerClock() async {
        // Телефон отстаёт от сервера на 100 с: остаток всё равно считается по серверу
        let phoneTime = timed.serverTime.addingTimeInterval(-100)
        let vm = player(FakeRunRepository(start: timed)) { phoneTime }
        await vm.load()
        XCTAssertEqual(vm.remainingSeconds(at: phoneTime)!, 20, accuracy: 0.01)
        XCTAssertEqual(vm.remainingSeconds(at: phoneTime.addingTimeInterval(15))!, 5, accuracy: 0.01)
        XCTAssertEqual(vm.remainingFraction(at: phoneTime.addingTimeInterval(10))!, 0.5, accuracy: 0.01)
    }

    func testExpiredTimerAsksServerForConsequences() async {
        var current = timed.serverTime
        let runs = FakeRunRepository(start: timed)
        runs.stateResult = .success(finished)
        let vm = player(runs) { current }
        await vm.load()

        await vm.tick()
        XCTAssertEqual(runs.stateCalls, 0, "время ещё не вышло")
        current = timed.node!.timeoutAt!.addingTimeInterval(0.1)
        await vm.tick()
        XCTAssertEqual(runs.stateCalls, 1)
        XCTAssertEqual(vm.phase, .finished)
        XCTAssertNil(activeRuns.current, "завершённое прохождение больше не предлагается продолжить")
    }

    func testTimerWithoutServerTimeoutKeepsChoosing() async {
        var current = timed.serverTime
        let runs = FakeRunRepository(start: timed)
        runs.stateResult = .success(timed)  // сервер пока не применил таймаут
        let vm = player(runs) { current }
        await vm.load()
        current = timed.node!.timeoutAt!.addingTimeInterval(0.1)
        await vm.tick()
        XCTAssertEqual(vm.phase, .choosing)
    }

    func testExitIsReportedOnlyForUnfinishedRun() async {
        let vm = player(FakeRunRepository(start: reading))
        await vm.load()
        await vm.exit()
        XCTAssertEqual(events.events.map(\.type), ["run_exited"])

        let done = player(FakeRunRepository(start: finished))
        await done.load()
        await done.exit()
        XCTAssertEqual(events.events.count, 1)
    }

    func testStartFailureIsShown() async {
        let runs = FakeRunRepository(start: reading)
        runs.startResult = .failure(APIError.server(status: 404, code: "scenario_not_found", message: "Сценарий не найден"))
        let vm = player(runs)
        await vm.load()
        XCTAssertEqual(vm.phase, .failed("Сценарий не найден"))
    }
}
