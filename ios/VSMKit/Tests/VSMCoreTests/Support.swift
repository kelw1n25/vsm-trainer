import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
@testable import VSMCore

/// Настоящие ответы backend, снятые с работающего сервера на синтетических данных.
enum Fixture {
    static func data(_ name: String) -> Data {
        let url = Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures")!
        return try! Data(contentsOf: url)
    }

    static func decode<T: Decodable>(_ type: T.Type, _ name: String) -> T {
        try! JSONDecoder.api.decode(T.self, from: data(name))
    }
}

/// Заглушка сети: отвечает по очереди заданными ответами и запоминает запросы.
final class StubTransport: HTTPTransport, @unchecked Sendable {
    typealias Handler = @Sendable (URLRequest) async throws -> (Int, Data)
    private let lock = NSLock()
    private var handler: Handler
    private(set) var requests: [URLRequest] = []

    init(_ handler: @escaping Handler) {
        self.handler = handler
    }

    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let current = lock.withLock { requests.append(request); return handler }
        let (status, data) = try await current(request)
        return (data, HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!)
    }

    var paths: [String] { lock.withLock { requests.map { $0.url!.path } } }
}

func errorBody(_ code: String, _ message: String = "сообщение") -> Data {
    try! JSONSerialization.data(withJSONObject: ["detail": ["code": code, "message": message]])
}

func tokens(access: String = "access-1", refresh: String = "refresh-1") -> SessionTokens {
    SessionTokens(accessToken: access, refreshToken: refresh, expiresIn: 43200, employeeId: 2, fullName: "Синтетический Проводник", role: .conductor)
}

func tokensJSON(access: String, refresh: String) -> Data {
    try! JSONEncoder().encode(tokens(access: access, refresh: refresh))
}

let baseURL = URL(string: "https://trainer.example")!

/// Прохождение по сценарию теста: каждое действие отдаёт следующее заданное состояние или ошибку.
final class FakeRunRepository: RunRepository, @unchecked Sendable {
    var startResult: Result<RunState, Error>
    var revealResult: Result<RunState, Error>?
    var chooseResults: [Result<RunState, Error>] = []
    var stateResult: Result<RunState, Error>?
    private(set) var chooseCalls = 0
    private(set) var stateCalls = 0

    init(start: RunState) {
        startResult = .success(start)
    }

    func start(scenarioId: String) async throws -> RunState { try startResult.get() }

    func state(runId: String) async throws -> RunState {
        stateCalls += 1
        return try stateResult!.get()
    }

    func reveal(runId: String, nodeId: String) async throws -> RunState { try revealResult!.get() }

    func choose(runId: String, nodeId: String, choiceId: String) async throws -> RunState {
        chooseCalls += 1
        // Пауза даёт шанс второму нажатию прийти, пока первый запрос «в пути»
        try await Task.sleep(nanoseconds: 20_000_000)
        return try chooseResults.removeFirst().get()
    }

    func debrief(runId: String) async throws -> Debrief { Fixture.decode(Debrief.self, "debrief") }
}

/// Репозиторий экранов в памяти: хватает для проверки событий аналитики и уведомлений.
final class FakeTrainerRepository: TrainerRepository, @unchecked Sendable {
    var notificationsResult: Result<Loaded<NotificationList>, Error> = .success(Loaded(value: Fixture.decode(NotificationList.self, "notifications")))
    private(set) var events: [(type: String, runId: String?, notificationId: Int?)] = []
    private(set) var cacheCleared = false

    func profile() async throws -> Loaded<Profile> { Loaded(value: Fixture.decode(Profile.self, "profile")) }
    func scenarios() async throws -> Loaded<[ScenarioSummary]> { Loaded(value: Fixture.decode([ScenarioSummary].self, "scenarios")) }
    func meta() async throws -> Loaded<Meta> { Loaded(value: Fixture.decode(Meta.self, "meta")) }
    func leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod) async throws -> Loaded<Leaderboard> {
        Loaded(value: Fixture.decode(Leaderboard.self, "leaderboard"))
    }
    func notifications() async throws -> Loaded<NotificationList> { try notificationsResult.get() }
    func markRead(_ id: Int) async throws {}
    func markAllRead() async throws {}
    func analytics() async throws -> Loaded<Analytics> { Loaded(value: Fixture.decode(Analytics.self, "analytics")) }
    func recordEvent(_ type: String, runId: String?, notificationId: Int?) async { events.append((type, runId, notificationId)) }
    func clearCache() { cacheCleared = true }
}
