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

/// Справочники для интерфейса `GET /api/meta`: названия компетенций по коду.
public struct Meta: Codable, Sendable {
    public let competences: [String: String]
}
