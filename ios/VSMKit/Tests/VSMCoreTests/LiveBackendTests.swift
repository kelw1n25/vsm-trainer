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
        let engine = StoryEngine(
            scenarioId: "business-seat-conflict", employeeId: 3, playerName: "Проводник", runs: runs,
            memory: StoryMemory(store: InMemoryKeyValueStore()), reduceMotion: { true }
        )

        await engine.start()
        // Лучший путь: проверить оба билета → вызвать начальника поезда → законное повышение класса
        for choiceId in ["check_both", "call_chief", "proper_upgrade"] {
            try await readUntilChoices(engine)
            XCTAssertEqual(engine.phase, .choices)
            await engine.choose(choiceId)
        }
        try await readUntilChoices(engine)
        XCTAssertEqual(engine.phase, .ending)
        XCTAssertEqual(engine.run?.final?.outcome, .success)

        let debrief = try await runs.debrief(runId: engine.run!.id)
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

    /// Дочитать сцену: касания «Далее», пока сервер не покажет варианты или не наступит финал.
    private func readUntilChoices(_ engine: StoryEngine) async throws {
        for _ in 0..<200 {
            if engine.phase == .ending || (engine.phase == .choices && !engine.busy) { return }
            engine.advance()
            try await Task.sleep(for: .milliseconds(50))
        }
        XCTFail("сцена не дошла до выбора")
    }
}
