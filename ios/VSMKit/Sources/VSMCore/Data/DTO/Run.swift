import Foundation

public enum RunStatus: String, Codable, Sendable {
    case inProgress = "in_progress"
    case success
    case partial
    case failure
}

/// Реплика: говорящий, тип (речь, мысль, рассказчик) и текст.
public struct Line: Codable, Hashable, Sendable {
    public let speaker: String
    public let name: String?
    public let role: String?
    public let kind: String
    public let text: String

    public init(speaker: String, name: String?, role: String?, kind: String, text: String) {
        self.speaker = speaker
        self.name = name
        self.role = role
        self.kind = kind
        self.text = text
    }
}

public struct Scene: Codable, Hashable, Sendable {
    public let background: String
}

public struct Choice: Codable, Identifiable, Hashable, Sendable {
    public let id: String
    public let text: String
}

/// Текущий шаг. Пока сцена не дочитана (`choicesShown == false`), вариантов нет и таймер не идёт.
public struct Node: Codable, Hashable, Sendable {
    public let id: String
    public let scene: Scene
    public let dialogue: [Line]
    public let choicesShown: Bool
    public let choices: [Choice]
    public let timerSeconds: Int?
    public let deadlineAt: Date?
    public let timeoutAt: Date?

    enum CodingKeys: String, CodingKey {
        case id, scene, dialogue, choices
        case choicesShown = "choices_shown"
        case timerSeconds = "timer_seconds"
        case deadlineAt = "deadline_at"
        case timeoutAt = "timeout_at"
    }
}

/// Итог шага: что выбрано (или истёк таймер), реакция персонажей и изменение шкал.
public struct Step: Codable, Hashable, Sendable {
    public let kind: String
    public let text: String
    public let reaction: [Line]
    public let loyaltyDelta: Int
    public let safetyDelta: Int
    public let competences: [String: Int]

    enum CodingKeys: String, CodingKey {
        case kind, text, reaction, competences
        case loyaltyDelta = "loyalty_delta"
        case safetyDelta = "safety_delta"
    }

    public var isTimeout: Bool { kind == "timeout" }
}

public struct Final: Codable, Hashable, Sendable {
    public let outcome: RunStatus
    public let reason: String
    public let ending: String
    public let text: String
    public let dialogue: [Line]
    public let xpEarned: Int
    public let competencePoints: [String: Int]

    enum CodingKeys: String, CodingKey {
        case outcome, reason, ending, text, dialogue
        case xpEarned = "xp_earned"
        case competencePoints = "competence_points"
    }
}

public struct Achievement: Codable, Hashable, Sendable {
    public let code: String
    public let title: String
    public let description: String
}

public struct Level: Codable, Hashable, Sendable {
    public let level: Int
    public let title: String
    public let xp: Int
    public let levelXp: Int
    public let nextLevelXp: Int?

    enum CodingKeys: String, CodingKey {
        case level, title, xp
        case levelXp = "level_xp"
        case nextLevelXp = "next_level_xp"
    }

    /// Доля пути до следующего уровня, 0…1; на последнем уровне — 1.
    public var progress: Double {
        guard let next = nextLevelXp, next > levelXp else { return 1 }
        return min(1, max(0, Double(xp - levelXp) / Double(next - levelXp)))
    }
}

/// Состояние прохождения — единственный источник правды о сценарии. Клиент его только показывает.
public struct RunState: Codable, Hashable, Sendable {
    public let id: String
    public let scenarioId: String
    public let scenarioTitle: String
    public let category: String
    public let status: RunStatus
    public let loyalty: Int
    public let safety: Int
    public let stepsTaken: Int
    public let stepsLeft: Int
    public let node: Node?
    public let final: Final?
    public let lastSteps: [Step]
    public let newAchievements: [Achievement]
    public let levelUp: Level?
    public let serverTime: Date

    enum CodingKeys: String, CodingKey {
        case id, category, status, loyalty, safety, node, final
        case scenarioId = "scenario_id"
        case scenarioTitle = "scenario_title"
        case stepsTaken = "steps_taken"
        case stepsLeft = "steps_left"
        case lastSteps = "last_steps"
        case newAchievements = "new_achievements"
        case levelUp = "level_up"
        case serverTime = "server_time"
    }
}

struct StartRunRequest: Encodable {
    let scenarioId: String
    enum CodingKeys: String, CodingKey { case scenarioId = "scenario_id" }
}

struct NodeRequest: Encodable {
    let nodeId: String
    enum CodingKeys: String, CodingKey { case nodeId = "node_id" }
}

struct ChoiceRequest: Encodable {
    let nodeId: String
    let choiceId: String
    enum CodingKeys: String, CodingKey {
        case nodeId = "node_id"
        case choiceId = "choice_id"
    }
}
