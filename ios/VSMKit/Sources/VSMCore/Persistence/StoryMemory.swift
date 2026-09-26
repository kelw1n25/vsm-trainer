import Foundation

/// Запись журнала новеллы: реплика, мысль, слова рассказчика, решение игрока или истёкший таймер.
public struct HistoryEntry: Codable, Hashable, Sendable {
    public let kind: String
    public let speaker: String?
    public let text: String

    public init(kind: String, speaker: String?, text: String) {
        self.kind = kind
        self.speaker = speaker
        self.text = text
    }
}

/// Где игрок остановился в прохождении: узел, прочитанные реплики, журнал.
public struct RunProgress: Codable, Hashable, Sendable {
    public var scenarioId: String
    public var nodeId: String
    public var lineIndex: Int
    public var history: [HistoryEntry]
    public var choices: [String]
    public var ending: String?
}

/// Что игрок уже видел в сценарии за все прохождения: сцены (можно пропустить) и открытые финалы.
public struct ScenarioMemory: Codable, Hashable, Sendable {
    public var seenNodes: [String] = []
    public var endings: [String] = []
    public var playthroughs = 0
}

/// Прогресс чтения новеллы на устройстве — как localStorage сайта (`story/persistence.ts`): место в истории,
/// журнал, виденные сцены, «Авто» и «Звук». Решения и шкалы хранит сервер: подделать их отсюда нельзя.
public final class StoryMemory: Sendable {
    private struct Store: Codable {
        var runs: [String: RunProgress] = [:]
        /// Порядок прохождений — чтобы хранить только последние журналы.
        var order: [String] = []
        var scenarios: [String: ScenarioMemory] = [:]
        var auto = false
        var muted = false
    }

    private static let maxRuns = 20
    private let store: KeyValueStore
    private let lock = NSLock()

    public init(store: KeyValueStore) {
        self.store = store
    }

    private func key(_ employeeId: Int) -> String { "vsm_story_v1:\(employeeId)" }

    private func read(_ employeeId: Int) -> Store {
        store.value(Store.self, forKey: key(employeeId)) ?? Store()
    }

    private func mutate(_ employeeId: Int, _ change: (inout Store) -> Void) {
        lock.withLock {
            var value = read(employeeId)
            change(&value)
            // Журналы старых прохождений не нужны вечно — держим последние
            while value.order.count > Self.maxRuns {
                value.runs[value.order.removeFirst()] = nil
            }
            store.setValue(value, forKey: key(employeeId))
        }
    }

    public func progress(_ employeeId: Int, runId: String) -> RunProgress? {
        lock.withLock { read(employeeId).runs[runId] }
    }

    public func saveProgress(_ employeeId: Int, runId: String, _ progress: RunProgress) {
        mutate(employeeId) { store in
            store.order.removeAll { $0 == runId }
            store.order.append(runId)
            store.runs[runId] = progress
        }
    }

    public func scenario(_ employeeId: Int, scenarioId: String) -> ScenarioMemory {
        lock.withLock { read(employeeId).scenarios[scenarioId] ?? ScenarioMemory() }
    }

    public func rememberScene(_ employeeId: Int, scenarioId: String, nodeId: String) {
        guard !scenario(employeeId, scenarioId: scenarioId).seenNodes.contains(nodeId) else { return }
        mutate(employeeId) { store in
            store.scenarios[scenarioId, default: ScenarioMemory()].seenNodes.append(nodeId)
        }
    }

    public func rememberEnding(_ employeeId: Int, scenarioId: String, runId: String, ending: String) {
        mutate(employeeId) { store in
            var memory = store.scenarios[scenarioId] ?? ScenarioMemory()
            // Финал одного прохождения засчитываем один раз, даже если экран открыли повторно
            if store.runs[runId]?.ending == nil { memory.playthroughs += 1 }
            if !memory.endings.contains(ending) { memory.endings.append(ending) }
            store.scenarios[scenarioId] = memory
            store.runs[runId]?.ending = ending
        }
    }

    public func auto(_ employeeId: Int) -> Bool { lock.withLock { read(employeeId).auto } }

    public func saveAuto(_ employeeId: Int, _ auto: Bool) { mutate(employeeId) { $0.auto = auto } }

    public func muted(_ employeeId: Int) -> Bool { lock.withLock { read(employeeId).muted } }

    public func saveMuted(_ employeeId: Int, _ muted: Bool) { mutate(employeeId) { $0.muted = muted } }
}
