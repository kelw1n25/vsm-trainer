import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import XCTest
@testable import VSMCore

/// Сквозная проверка против запущенного backend: VSM_LIVE_API=http://localhost:8000 swift test.
/// Без переменной пропускается — в CI backend для iOS-job не поднимается.
@MainActor
final class LiveBackendTests: XCTestCase {
    func testFullScenarioAgainstRealServer() async throws {
        guard let raw = ProcessInfo.processInfo.environment["VSM_LIVE_API"], let url = URL(string: raw) else {
            throw XCTSkip("VSM_LIVE_API не задан")
        }
        let tokens = InMemoryTokenStore()
        let api = APIClient(baseURL: url, tokens: tokens)
        _ = try await api.login(personnelNumber: "100003", password: "demo2026")
        let runs = RemoteRunRepository(api: api)
        let events = RemoteTrainerRepository(api: api, cache: InMemoryResponseCache())
        let player = ScenarioPlayerViewModel(
            scenarioId: "business-seat-conflict", runs: runs, activeRuns: ActiveRunStore(store: InMemoryKeyValueStore()), events: events
        )

        await player.load()
        // Лучший путь: проверить оба билета → вызвать начальника поезда → законное повышение класса
        for choiceId in ["check_both", "call_chief", "proper_upgrade"] {
            if player.phase == .reading { await player.revealChoices() }
            XCTAssertEqual(player.phase, .choosing)
            let choice = try XCTUnwrap(player.node?.choices.first { $0.id == choiceId }, "нет варианта \(choiceId)")
            await player.choose(choice)
        }
        XCTAssertEqual(player.phase, .finished)
        XCTAssertEqual(player.run?.final?.outcome, .success)

        let debrief = try await runs.debrief(runId: player.run!.id)
        XCTAssertEqual(debrief.steps.count, 3)

        // Обмен refresh-токена: новый работает, старый повторно не принимается
        let before = try XCTUnwrap(tokens.load())
        var request = URLRequest(url: url.appendingPathComponent("/api/auth/refresh"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["refresh_token": before.refreshToken])
        let (_, first) = try await URLSessionTransport().send(request)
        let (_, reused) = try await URLSessionTransport().send(request)
        XCTAssertEqual(first.statusCode, 200)
        XCTAssertEqual(reused.statusCode, 401)
    }
}
