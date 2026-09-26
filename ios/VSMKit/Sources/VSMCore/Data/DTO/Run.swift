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
    /** Эмоция говорящего с этой реплики. */
    public let expression: String?
    /** Звук реплики: вздох и т. п. */
    public let sound: String?

    public init(speaker: String, name: String?, role: String?, kind: String, text: String, expression: String? = nil, sound: String? = nil) {
        self.speaker = speaker
        self.name = name
        self.role = role
        self.kind = kind
        self.text = text
        self.expression = expression
        self.sound = sound
    }
}

/// Внешность персонажа: для экрана важны форма (сотрудник или пассажир), возраст и тембр голоса.
public struct Look: Codable, Hashable, Sendable {
    public let outfit: String
    public let child: Bool
    public let hairStyle: String

    enum CodingKeys: String, CodingKey {
        case outfit, child
        case hairStyle = "hair_style"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        outfit = (try? container.decode(String.self, forKey: .outfit)) ?? "casual"
        child = (try? container.decode(Bool.self, forKey: .child)) ?? false
        hairStyle = (try? container.decode(String.self, forKey: .hairStyle)) ?? "short"
    }

    public init(outfit: String = "casual", child: Bool = false, hairStyle: String = "short") {
        self.outfit = outfit
        self.child = child
        self.hairStyle = hairStyle
    }
}

public struct Character: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let name: String
    public let role: String
    public let look: Look
}

/// Персонаж в сцене: место, эмоция, поза, жест и предмет в руке.
public struct SceneCharacter: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let position: String
    public let expression: String
    public let pose: String
    public let hand: String?
    public let item: String?
}

public struct Scene: Codable, Hashable, Sendable {
    public let background: String
    public let characters: [SceneCharacter]

    public init(background: String, characters: [SceneCharacter] = []) {
        self.background = background
        self.characters = characters
    }
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
    public let scene: Scene
    public let dialogue: [Line]
    public let xpEarned: Int
    public let competencePoints: [String: Int]

    enum CodingKeys: String, CodingKey {
        case outcome, reason, ending, text, scene, dialogue
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
    public let difficulty: Int
    public let route: String
    public let serviceClass: String
    public let characters: [Character]
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
        case id, category, difficulty, route, characters, status, loyalty, safety, node, final
        case serviceClass = "service_class"
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
