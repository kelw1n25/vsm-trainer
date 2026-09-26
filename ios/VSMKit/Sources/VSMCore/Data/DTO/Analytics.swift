import Foundation

public struct WeekPoint: Codable, Hashable, Sendable {
    public let weekStart: Date
    public let xp: Int
    public let runs: Int

    enum CodingKeys: String, CodingKey {
        case xp, runs
        case weekStart = "week_start"
    }
}

public struct CompetenceStat: Codable, Hashable, Identifiable, Sendable {
    public let code: String
    public let title: String
    public let points: Int

    public var id: String { code }
}

public struct Recommendation: Codable, Hashable, Sendable {
    public let scenarioId: String
    public let title: String
    public let reason: String

    enum CodingKeys: String, CodingKey {
        case title, reason
        case scenarioId = "scenario_id"
    }
}

public struct Analytics: Codable, Hashable, Sendable {
    public let totalRuns: Int
    public let unfinishedRuns: Int
    public let completionRate: Double?
    public let avgDecisionSeconds: Double?
    public let progress: [WeekPoint]
    public let competences: [CompetenceStat]
    public let strengths: [String]
    public let weaknesses: [String]
    public let mistakes: [String]
    public let recommendation: Recommendation?

    enum CodingKeys: String, CodingKey {
        case progress, competences, strengths, weaknesses, mistakes, recommendation
        case totalRuns = "total_runs"
        case unfinishedRuns = "unfinished_runs"
        case completionRate = "completion_rate"
        case avgDecisionSeconds = "avg_decision_seconds"
    }
}

/// Действие на клиенте, которое сервер сам увидеть не может (`POST /api/analytics/events`).
struct ClientEvent: Encodable {
    let type: String
    let runId: String?
    let notificationId: Int?
    let platform = "ios"

    enum CodingKeys: String, CodingKey {
        case type, platform
        case runId = "run_id"
        case notificationId = "notification_id"
    }
}
