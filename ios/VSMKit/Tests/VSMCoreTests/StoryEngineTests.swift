import Foundation
import XCTest
@testable import VSMCore

/// Время под управлением теста: паузы движка ждут, пока тест не «прокрутит» часы.
final class VirtualTime: @unchecked Sendable {
    private struct Sleeper {
        let id: UUID
        let until: TimeInterval
        let continuation: CheckedContinuation<Void, Error>
    }

    private let lock = NSLock()
    private var elapsed: TimeInterval = 0
    private var sleepers: [Sleeper] = []
    let start: Date

    init(start: Date) {
        self.start = start
    }

    var now: Date { lock.withLock { start.addingTimeInterval(elapsed) } }

    func sleep(_ duration: Duration) async throws {
        let seconds = Double(duration.components.seconds) + Double(duration.components.attoseconds) / 1e18
        let id = UUID()
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                lock.withLock { sleepers.append(Sleeper(id: id, until: elapsed + seconds, continuation: continuation)) }
            }
        } onCancel: {
            let cancelled = lock.withLock { () -> Sleeper? in
                guard let index = sleepers.firstIndex(where: { $0.id == id }) else { return nil }
                return sleepers.remove(at: index)
            }
            cancelled?.continuation.resume(throwing: CancellationError())
        }
    }

    /// Прокрутить часы: будим паузы по порядку, давая разбуженному коду отработать.
    @MainActor
    func advance(by seconds: TimeInterval) async {
        let target = lock.withLock { elapsed + seconds }
        await settle()
        while true {
            let next = lock.withLock { () -> Sleeper? in
                guard let index = sleepers.indices.filter({ sleepers[$0].until <= target }).min(by: { sleepers[$0].until < sleepers[$1].until }) else {
                    return nil
                }
                let sleeper = sleepers.remove(at: index)
                elapsed = max(elapsed, sleeper.until)
                return sleeper
            }
            guard let next else { break }
            next.continuation.resume()
            await settle()
        }
        lock.withLock { elapsed = target }
        await settle()
    }

    @MainActor
    func settle() async {
        for _ in 0..<40 { await Task.yield() }
    }
}

@MainActor
final class StoryEngineTests: XCTestCase {
    private let reading = Fixture.decode(RunState.self, "run_reading")
    private let choosing = Fixture.decode(RunState.self, "run_choosing")
    private let finished = Fixture.decode(RunState.self, "run_finished")
    private let timed = Fixture.decode(RunState.self, "run_timed")

    private func engine(
        _ runs: FakeRunRepository,
        time: VirtualTime,
        memory: StoryMemory = StoryMemory(store: InMemoryKeyValueStore()),
        reduceMotion: Bool = false
    ) -> StoryEngine {
        StoryEngine(
            scenarioId: "business-seat-conflict", employeeId: 2, playerName: "Юлия", runs: runs, memory: memory,
            now: { time.now }, sleep: { try await time.sleep($0) }, reduceMotion: { reduceMotion }
        )
    }

    private func time(for run: RunState) -> VirtualTime {
        VirtualTime(start: run.serverTime)
    }

    func testNewRunShowsIntroThenTypesFirstLine() async {
        let clock = time(for: reading)
        let story = engine(FakeRunRepository(start: reading), time: clock)
        await story.start()
        XCTAssertEqual(story.phase, .intro)
        await clock.advance(by: 2.4)
        XCTAssertEqual(story.phase, .dialogue)
        XCTAssertEqual(story.line?.kind, "narration")
        XCTAssertTrue(story.typing)
        await clock.advance(by: 0.281)
        XCTAssertEqual(story.shownChars, 10, "по символу каждые 28 мс")
        XCTAssertEqual(story.shownText, String(story.line!.text.prefix(10)))
    }

    func testTapFinishesTypingThenAdvances() async {
        let clock = time(for: reading)
        let story = engine(FakeRunRepository(start: reading), time: clock)
        await story.start()
        story.advance()  // касание на заставке сразу открывает сцену
        XCTAssertEqual(story.phase, .dialogue)
        story.advance()
        XCTAssertFalse(story.typing)
        XCTAssertEqual(story.shownChars, story.line!.text.count)
        story.advance()
        XCTAssertEqual(story.line?.speaker, "sergey")
        XCTAssertEqual(story.history.count, 2)
    }

    func testExpressionChangesWithLine() async {
        let clock = time(for: reading)
        let story = engine(FakeRunRepository(start: reading), time: clock, reduceMotion: true)
        await story.start()
        await clock.advance(by: 0.3)
        XCTAssertEqual(story.expressions["sergey"], reading.node!.scene.characters.first { $0.id == "sergey" }!.expression)
        for _ in 0..<3 { story.advance() }
        XCTAssertEqual(story.expressions["sergey"], "annoyed")
    }

    func testEndOfSceneRevealsChoicesOnServer() async {
        let clock = time(for: reading)
        let runs = FakeRunRepository(start: reading)
        runs.revealResult = .success(choosing)
        let story = engine(runs, time: clock, reduceMotion: true)
        await story.start()
        await clock.advance(by: 0.3)
        for _ in 0..<5 { story.advance() }
        await clock.settle()
        XCTAssertEqual(runs.revealCalls, 1)
        XCTAssertEqual(story.phase, .choices)
        XCTAssertFalse(story.busy)
        XCTAssertEqual(story.run?.node?.choices.map(\.id), ["check_both", "side_with_sergey", "tell_wait"])
    }

    func testChoicePlaysReactionThenEnding() async {
        let clock = time(for: choosing)
        let runs = FakeRunRepository(start: choosing)
        runs.chooseResults = [.success(finished)]
        let memory = StoryMemory(store: InMemoryKeyValueStore())
        let story = engine(runs, time: clock, memory: memory, reduceMotion: true)
        await story.start()
        XCTAssertEqual(story.phase, .choices)

        await story.choose("check_both")
        XCTAssertEqual(story.phase, .dialogue, "сначала реакция персонажей")
        XCTAssertEqual(story.line?.text, "Спасибо, я подожду.")
        XCTAssertEqual(story.history.last { $0.kind == "choice" }?.speaker, "Юлия")
        let reactionLines = finished.lastSteps.flatMap(\.reaction).count
        for _ in 0..<reactionLines { story.advance() }
        XCTAssertEqual(story.phase, .ending)
        XCTAssertEqual(memory.scenario(2, scenarioId: "business-seat-conflict").endings, ["Оба пассажира довольны"])
        XCTAssertEqual(memory.scenario(2, scenarioId: "business-seat-conflict").playthroughs, 1)
    }

    func testSecondTapWhileChoosingIsIgnored() async {
        let clock = time(for: choosing)
        let runs = FakeRunRepository(start: choosing)
        runs.chooseResults = [.success(finished)]
        let story = engine(runs, time: clock, reduceMotion: true)
        await story.start()
        async let first: Void = story.choose("check_both")
        async let second: Void = story.choose("check_both")
        _ = await (first, second)
        XCTAssertEqual(runs.chooseCalls, 1)
    }

    func testTimeoutAsksServerAtDeadline() async {
        let clock = time(for: timed)
        let runs = FakeRunRepository(start: timed)
        runs.stateResult = .success(finished)
        let story = engine(runs, time: clock, reduceMotion: true)
        await story.start()
        XCTAssertEqual(story.phase, .choices)
        XCTAssertEqual(story.remaining() ?? 0, 20, accuracy: 0.01)
        await clock.advance(by: 20)
        XCTAssertEqual(runs.stateCalls, 0, "до дедлайна с поправкой сервер не опрашиваем")
        await clock.advance(by: 1.5)
        XCTAssertEqual(runs.stateCalls, 1)
        XCTAssertNotEqual(story.phase, .choices)
    }

    func testExpiredAnswerRereadsState() async {
        let clock = time(for: timed)
        let runs = FakeRunRepository(start: timed)
        runs.chooseResults = [.failure(APIError.server(status: 409, code: "time_expired", message: "Время вышло"))]
        runs.stateResult = .success(finished)
        let story = engine(runs, time: clock, reduceMotion: true)
        await story.start()
        await story.choose("separate")
        XCTAssertEqual(runs.stateCalls, 1)
        XCTAssertEqual(story.run?.status, .success)
    }

    func testResumeContinuesFromSavedLineWithoutIntro() async {
        let clock = time(for: reading)
        let memory = StoryMemory(store: InMemoryKeyValueStore())
        let first = engine(FakeRunRepository(start: reading), time: clock, memory: memory, reduceMotion: true)
        await first.start()
        await clock.advance(by: 0.3)
        first.advance()
        first.advance()
        XCTAssertEqual(first.line?.speaker, "lyudmila")
        first.dispose()

        let second = engine(FakeRunRepository(start: reading), time: clock, memory: memory, reduceMotion: true)
        await second.start()
        XCTAssertEqual(second.phase, .dialogue)
        XCTAssertEqual(second.line?.speaker, "lyudmila")
        XCTAssertEqual(second.history.count, 3, "реплика, на которой остановились, не задваивается")
    }

    func testSeenSceneCanBeSkipped() async {
        let clock = time(for: reading)
        let memory = StoryMemory(store: InMemoryKeyValueStore())
        memory.rememberScene(2, scenarioId: "business-seat-conflict", nodeId: "start")
        let runs = FakeRunRepository(start: reading)
        runs.revealResult = .success(choosing)
        let story = engine(runs, time: clock, memory: memory, reduceMotion: true)
        await story.start()
        await clock.advance(by: 0.3)
        XCTAssertTrue(story.canSkip)
        story.skip()
        await clock.settle()
        XCTAssertEqual(story.phase, .choices)
        XCTAssertEqual(story.history.count, reading.node!.dialogue.count, "пропущенные реплики попадают в журнал")
    }

    func testAutoAdvancesAfterPause() async {
        let clock = time(for: reading)
        let memory = StoryMemory(store: InMemoryKeyValueStore())
        let story = engine(FakeRunRepository(start: reading), time: clock, memory: memory, reduceMotion: true)
        await story.start()
        await clock.advance(by: 0.3)
        story.toggleAuto()
        XCTAssertTrue(memory.auto(2), "выбор «Авто» запоминается")
        XCTAssertEqual(story.line?.speaker, "sergey", "включение «Авто» сразу листает дочитанную реплику")
        await clock.advance(by: 4)
        XCTAssertEqual(story.line?.speaker, "lyudmila")
    }

    func testFinishedRunOpensEnding() async {
        let clock = time(for: finished)
        let story = engine(FakeRunRepository(start: finished), time: clock)
        await story.start()
        XCTAssertEqual(story.phase, .ending)
    }

    func testStartFailureShowsMessage() async {
        let clock = time(for: reading)
        let runs = FakeRunRepository(start: reading)
        runs.startResult = .failure(APIError.offline)
        let story = engine(runs, time: clock)
        await story.start()
        XCTAssertEqual(story.phase, .error)
        XCTAssertEqual(story.notice, APIError.offline.userMessage)
    }
}
