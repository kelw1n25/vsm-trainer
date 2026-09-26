import Foundation

/// Данные экранов: профиль, каталог, рейтинг, уведомления, аналитика, справочник, архив веток, команда.
/// Сначала сеть; без сети — последний сохранённый ответ (Remote API → Repository → Local Cache → UI).
/// Ошибку сервера (4xx/5xx) кэшем не маскируем: она означает, что данные неверны, а не что их нет.
public protocol TrainerRepository: Sendable {
    func profile() async throws -> Loaded<Profile>
    func updateAvatar(_ avatar: Avatar) async throws -> Avatar
    func scenarios() async throws -> Loaded<[ScenarioSummary]>
    func meta() async throws -> Loaded<Meta>
    func leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod) async throws -> Loaded<Leaderboard>
    func notifications() async throws -> Loaded<NotificationList>
    func markRead(_ id: Int) async throws
    func markAllRead() async throws
    func analytics() async throws -> Loaded<Analytics>
    func employeeAnalytics(_ employeeId: Int) async throws -> Loaded<Analytics>
    func team() async throws -> Loaded<[TeamMember]>
    func storyMap(scenarioId: String) async throws -> Loaded<StoryMap>
    func handbook() async throws -> Loaded<Handbook>
    func recordEvent(_ type: String, runId: String?, notificationId: Int?) async
    func clearCache()
}

public final class RemoteTrainerRepository: TrainerRepository {
    private let api: APIClient
    private let cache: ResponseCache

    public init(api: APIClient, cache: ResponseCache) {
        self.api = api
        self.cache = cache
    }

    private func cached<T: Codable & Sendable>(_ key: String, fetch: () async throws -> T) async throws -> Loaded<T> {
        do {
            let value = try await fetch()
            if let data = try? JSONEncoder.api.encode(value) { cache.write(data, for: key) }
            return Loaded(value: value)
        } catch APIError.offline {
            guard let entry = cache.read(key), let value = try? JSONDecoder.api.decode(T.self, from: entry.data) else {
                throw APIError.offline
            }
            return Loaded(value: value, staleSince: entry.savedAt)
        }
    }

    public func profile() async throws -> Loaded<Profile> {
        try await cached("profile") { try await api.get("/api/profile") }
    }

    public func updateAvatar(_ avatar: Avatar) async throws -> Avatar {
        try await api.put("/api/profile/avatar", body: avatar)
    }

    public func scenarios() async throws -> Loaded<[ScenarioSummary]> {
        try await cached("scenarios") { try await api.get("/api/scenarios") }
    }

    public func meta() async throws -> Loaded<Meta> {
        try await cached("meta") { try await api.get("/api/meta") }
    }

    public func leaderboard(scope: LeaderboardScope, period: LeaderboardPeriod) async throws -> Loaded<Leaderboard> {
        try await cached("leaderboard-\(scope.rawValue)-\(period.rawValue)") {
            try await api.get("/api/leaderboard", query: [
                URLQueryItem(name: "scope", value: scope.rawValue), URLQueryItem(name: "period", value: period.rawValue),
            ])
        }
    }

    public func notifications() async throws -> Loaded<NotificationList> {
        try await cached("notifications") { try await api.get("/api/notifications") }
    }

    public func markRead(_ id: Int) async throws {
        let _: Status = try await api.post("/api/notifications/\(id)/read", body: Empty())
    }

    public func markAllRead() async throws {
        let _: Status = try await api.post("/api/notifications/read-all", body: Empty())
    }

    public func analytics() async throws -> Loaded<Analytics> {
        try await cached("analytics") { try await api.get("/api/analytics/me") }
    }

    public func employeeAnalytics(_ employeeId: Int) async throws -> Loaded<Analytics> {
        try await cached("analytics-\(employeeId)") { try await api.get("/api/analytics/employees/\(employeeId)") }
    }

    public func team() async throws -> Loaded<[TeamMember]> {
        try await cached("team") { try await api.get("/api/analytics/team") }
    }

    public func storyMap(scenarioId: String) async throws -> Loaded<StoryMap> {
        try await cached("story-map-\(scenarioId)") { try await api.get("/api/scenarios/\(scenarioId)/story-map") }
    }

    public func handbook() async throws -> Loaded<Handbook> {
        try await cached("handbook") { try await api.get("/api/handbook") }
    }

    /// Аналитическое событие не должно мешать пользователю: ошибку отправки не показываем.
    public func recordEvent(_ type: String, runId: String?, notificationId: Int?) async {
        let _: Status? = try? await api.post("/api/analytics/events", body: ClientEvent(type: type, runId: runId, notificationId: notificationId))
    }

    public func clearCache() {
        cache.removeAll()
    }
}
