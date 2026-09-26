import Foundation

public struct ProfileAchievement: Codable, Hashable, Sendable {
    public let code: String
    public let title: String
    public let description: String
    public let earnedAt: Date?

    enum CodingKeys: String, CodingKey {
        case code, title, description
        case earnedAt = "earned_at"
    }
}

public struct HistoryItem: Codable, Hashable, Identifiable, Sendable {
    public let runId: String
    public let scenarioId: String
    public let scenarioTitle: String
    public let category: String
    public let outcome: RunStatus
    public let xpEarned: Int
    public let loyalty: Int
    public let safety: Int
    public let finishedAt: Date

    public var id: String { runId }

    enum CodingKeys: String, CodingKey {
        case category, outcome, loyalty, safety
        case runId = "run_id"
        case scenarioId = "scenario_id"
        case scenarioTitle = "scenario_title"
        case xpEarned = "xp_earned"
        case finishedAt = "finished_at"
    }
}

public struct Profile: Codable, Hashable, Sendable {
    public let id: Int
    public let fullName: String
    public let role: Role
    public let brigade: String
    public let depot: String
    public let level: Level
    public let runsCompleted: Int
    public let competencePoints: [String: Int]
    public let achievements: [ProfileAchievement]
    public let history: [HistoryItem]

    enum CodingKeys: String, CodingKey {
        case id, role, brigade, depot, level, achievements, history
        case fullName = "full_name"
        case runsCompleted = "runs_completed"
        case competencePoints = "competence_points"
    }

    public var earnedAchievements: [ProfileAchievement] {
        achievements.filter { $0.earnedAt != nil }.sorted { ($0.earnedAt ?? .distantPast) > ($1.earnedAt ?? .distantPast) }
    }
}
