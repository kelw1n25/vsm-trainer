import Foundation

/// Прохождение сценария. Кэша нет намеренно: каждый шаг решает сервер по своему таймеру,
/// поэтому ответ, «сохранённый на потом», был бы недействительным.
public protocol RunRepository: Sendable {
    func start(scenarioId: String) async throws -> RunState
    func state(runId: String) async throws -> RunState
    func reveal(runId: String, nodeId: String) async throws -> RunState
    func choose(runId: String, nodeId: String, choiceId: String) async throws -> RunState
    func debrief(runId: String) async throws -> Debrief
}

public final class RemoteRunRepository: RunRepository {
    private let api: APIClient

    public init(api: APIClient) {
        self.api = api
    }

    public func start(scenarioId: String) async throws -> RunState {
        try await api.post("/api/runs", body: StartRunRequest(scenarioId: scenarioId))
    }

    public func state(runId: String) async throws -> RunState {
        try await api.get("/api/runs/\(runId)")
    }

    public func reveal(runId: String, nodeId: String) async throws -> RunState {
        try await api.post("/api/runs/\(runId)/reveal", body: NodeRequest(nodeId: nodeId))
    }

    public func choose(runId: String, nodeId: String, choiceId: String) async throws -> RunState {
        try await api.post("/api/runs/\(runId)/choices", body: ChoiceRequest(nodeId: nodeId, choiceId: choiceId))
    }

    public func debrief(runId: String) async throws -> Debrief {
        try await api.get("/api/runs/\(runId)/debrief")
    }
}
