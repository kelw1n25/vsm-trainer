import Foundation

/// Карточка каталога `GET /api/scenarios`.
public struct ScenarioSummary: Codable, Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let category: String
    public let difficulty: Int
    public let serviceClass: String
    public let route: String
    public let description: String
    public let situations: [Int]
    public let endingsTotal: Int

    enum CodingKeys: String, CodingKey {
        case id, title, category, difficulty, route, description, situations
        case serviceClass = "service_class"
        case endingsTotal = "endings_total"
    }
}

public struct WeeklyChallenge: Codable, Hashable, Sendable {
    public let scenarioId: String
    public let bonusXp: Int

    enum CodingKeys: String, CodingKey {
        case scenarioId = "scenario_id"
        case bonusXp = "bonus_xp"
    }
}

/// Справочники для интерфейса `GET /api/meta`: названия компетенций по коду и челлендж недели.
public struct Meta: Codable, Sendable {
    public let competences: [String: String]
    public let weeklyChallenge: WeeklyChallenge?

    enum CodingKeys: String, CodingKey {
        case competences
        case weeklyChallenge = "weekly_challenge"
    }
}
