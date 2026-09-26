import Foundation

/// Разбор одного шага: что выбрал, почему это повлияло на шкалы и как было лучше.
public struct DebriefStep: Codable, Hashable, Sendable {
    public let kind: String
    public let situation: String
    public let chosenText: String?
    public let explanation: String?
    public let wasBest: Bool
    public let bestText: String?
    public let bestExplanation: String?
    public let loyaltyDelta: Int
    public let safetyDelta: Int
    public let loyaltyAfter: Int
    public let safetyAfter: Int
    public let competences: [String: Int]
    public let elapsedSeconds: Double?
    public let timerSeconds: Int?

    enum CodingKeys: String, CodingKey {
        case kind, situation, explanation, competences
        case chosenText = "chosen_text"
        case wasBest = "was_best"
        case bestText = "best_text"
        case bestExplanation = "best_explanation"
        case loyaltyDelta = "loyalty_delta"
        case safetyDelta = "safety_delta"
        case loyaltyAfter = "loyalty_after"
        case safetyAfter = "safety_after"
        case elapsedSeconds = "elapsed_seconds"
        case timerSeconds = "timer_seconds"
    }
}

/// Ситуация из справочника, на которую опирается сценарий, — со стандартом реакции.
public struct Situation: Codable, Hashable, Sendable {
    public let number: Int
    public let title: String
    public let reaction: String
    public let phrases: [String]
}

public struct Debrief: Codable, Hashable, Sendable {
    public let runId: String
    public let scenarioId: String
    public let scenarioTitle: String
    public let outcome: RunStatus
    public let ending: String
    public let finalText: String
    public let xpEarned: Int
    public let competencePoints: [String: Int]
    public let initialLoyalty: Int
    public let initialSafety: Int
    public let loyalty: Int
    public let safety: Int
    public let decisions: Int
    public let bestDecisions: Int
    public let timeouts: Int
    public let averageReactionSeconds: Double?
    public let steps: [DebriefStep]
    public let situations: [Situation]

    enum CodingKeys: String, CodingKey {
        case outcome, ending, loyalty, safety, decisions, timeouts, steps, situations
        case runId = "run_id"
        case scenarioId = "scenario_id"
        case scenarioTitle = "scenario_title"
        case finalText = "final_text"
        case xpEarned = "xp_earned"
        case competencePoints = "competence_points"
        case initialLoyalty = "initial_loyalty"
        case initialSafety = "initial_safety"
        case bestDecisions = "best_decisions"
        case averageReactionSeconds = "average_reaction_seconds"
    }

    /// Шаги, где выбор был не лучшим или истёк таймер, — «что можно было сделать иначе».
    public var missedSteps: [DebriefStep] { steps.filter { !$0.wasBest && $0.bestText != nil } }
}
