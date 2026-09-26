import XCTest
@testable import VSMCore

/// DTO клиента совпадают с ответами сервера: фикстуры сняты с backend, а не написаны руками.
final class ContractTests: XCTestCase {
    func testServerDatesWithMicroseconds() throws {
        let date = try XCTUnwrap(ServerDate.parse("2026-09-26T08:39:53.731861Z"))
        let whole = try XCTUnwrap(ServerDate.parse("2026-09-26T08:39:53Z"))
        XCTAssertEqual(date.timeIntervalSince(whole), 0.731861, accuracy: 0.000001)
        XCTAssertNotNil(ServerDate.parse("2026-09-26T11:39:53.5+03:00"))
        XCTAssertNil(ServerDate.parse("вчера"))
        XCTAssertEqual(ServerDate.parse(ServerDate.format(date)), date, "дата переживает кэш без потери точности")
    }

    func testRunStatesDecode() {
        let reading = Fixture.decode(RunState.self, "run_reading")
        XCTAssertEqual(reading.status, .inProgress)
        XCTAssertEqual(reading.node?.choicesShown, false)
        XCTAssertEqual(reading.node?.choices, [], "до показа вариантов сервер их не отдаёт")
        XCTAssertFalse(reading.node!.dialogue.isEmpty)

        let timed = Fixture.decode(RunState.self, "run_timed")
        XCTAssertEqual(timed.node?.timerSeconds, 20)
        XCTAssertEqual(timed.node!.timeoutAt!.timeIntervalSince(timed.node!.deadlineAt!), 1, accuracy: 0.01)

        let finished = Fixture.decode(RunState.self, "run_finished")
        XCTAssertEqual(finished.status, .success)
        XCTAssertNil(finished.node)
        XCTAssertGreaterThan(finished.final!.xpEarned, 0)
        XCTAssertFalse(finished.lastSteps.first!.reaction.isEmpty)
    }

    func testScreensDecode() {
        let profile = Fixture.decode(Profile.self, "profile")
        XCTAssertEqual(profile.competencePoints.keys.contains("first_aid"), true, "коды компетенций не переименовываются")
        XCTAssertGreaterThanOrEqual(profile.level.progress, 0)
        XCTAssertEqual(Fixture.decode([ScenarioSummary].self, "scenarios").count, 14)
        XCTAssertNotNil(Fixture.decode(Leaderboard.self, "leaderboard").me)
        XCTAssertFalse(Fixture.decode(NotificationList.self, "notifications").items.isEmpty)
        XCTAssertEqual(Fixture.decode(Analytics.self, "analytics").competences.count, 5)
        XCTAssertEqual(Fixture.decode(Meta.self, "meta").competences.count, 5)
        XCTAssertEqual(Fixture.decode(SessionTokens.self, "login").role, .conductor)
    }

    func testDebriefExplainsConsequences() {
        let debrief = Fixture.decode(Debrief.self, "debrief")
        XCTAssertEqual(debrief.outcome, .success)
        XCTAssertFalse(debrief.steps.isEmpty)
        XCTAssertTrue(debrief.steps.allSatisfy { $0.explanation != nil })
        XCTAssertFalse(debrief.situations.isEmpty, "разбор опирается на стандарт ситуации из справочника")
    }
}
