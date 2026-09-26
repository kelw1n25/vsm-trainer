import Foundation

/// Архив веток `GET /api/scenarios/{id}/story-map`: тексты неисследованных вариантов и финалов сервер не отдаёт.
public struct StoryMap: Codable, Hashable, Sendable {
    public let scenarioId: String
    public let startNode: String
    public let playthroughs: Int
    public let choicesTotal: Int
    public let choicesExplored: Int
    public let nodes: [MapNode]
    public let endings: [MapEnding]

    enum CodingKeys: String, CodingKey {
        case playthroughs, nodes, endings
        case scenarioId = "scenario_id"
        case startNode = "start_node"
        case choicesTotal = "choices_total"
        case choicesExplored = "choices_explored"
    }
}

public struct MapNode: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let situation: String
    public let choices: [MapChoice]
    public let timer: Bool
    public let timeoutExplored: Bool
    public let timeoutNext: String?

    enum CodingKeys: String, CodingKey {
        case id, situation, choices, timer
        case timeoutExplored = "timeout_explored"
        case timeoutNext = "timeout_next"
    }
}

public struct MapChoice: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let explored: Bool
    public let text: String?
    public let next: String?
}

public struct MapEnding: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let outcome: RunStatus
    public let reached: Bool
    public let ending: String?
}

/// Справочник `GET /api/handbook`: ролевая модель, классы обслуживания, стандарты, 51 ситуация.
public struct Handbook: Codable, Hashable, Sendable {
    public let roleModel: RoleModel
    public let serviceClasses: [ServiceClass]
    public let standards: [Standard]
    public let situations: [Situation]

    enum CodingKeys: String, CodingKey {
        case standards, situations
        case roleModel = "role_model"
        case serviceClasses = "service_classes"
    }
}

public struct RoleModel: Codable, Hashable, Sendable {
    public let title: String
    public let steps: [RoleModelStep]
}

public struct RoleModelStep: Codable, Hashable, Sendable, Identifiable {
    public let code: String
    public let title: String
    public let phrases: [String]

    public var id: String { code }
}

public struct ServiceClass: Codable, Hashable, Sendable, Identifiable {
    public let code: String
    public let title: String
    public let layout: String
    public let aisleMm: Int
    public let pitchMm: Int
    public let seatMm: Int
    public let maxWaitMinutes: Int
    public let summary: String

    public var id: String { code }

    enum CodingKeys: String, CodingKey {
        case code, title, layout, summary
        case aisleMm = "aisle_mm"
        case pitchMm = "pitch_mm"
        case seatMm = "seat_mm"
        case maxWaitMinutes = "max_wait_minutes"
    }
}

public struct Standard: Codable, Hashable, Sendable, Identifiable {
    public let title: String
    public let text: String

    public var id: String { title }
}

/// Проводник депо для инструктора `GET /api/analytics/team`.
public struct TeamMember: Codable, Hashable, Sendable, Identifiable {
    public let employeeId: Int
    public let fullName: String
    public let brigade: String
    public let levelTitle: String
    public let xp: Int
    public let lastActivityAt: Date?
    public let weakestCompetence: String?

    public var id: Int { employeeId }

    enum CodingKeys: String, CodingKey {
        case brigade, xp
        case employeeId = "employee_id"
        case fullName = "full_name"
        case levelTitle = "level_title"
        case lastActivityAt = "last_activity_at"
        case weakestCompetence = "weakest_competence"
    }
}
