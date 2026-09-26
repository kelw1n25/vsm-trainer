import Foundation

/// Незавершённое прохождение на этом устройстве — для кнопки «Продолжить» после перезапуска.
/// Само состояние хранится на сервере; здесь только ссылка на него.
public struct ActiveRun: Codable, Equatable, Sendable {
    public let runId: String
    public let scenarioId: String
    public let title: String

    public init(runId: String, scenarioId: String, title: String) {
        self.runId = runId
        self.scenarioId = scenarioId
        self.title = title
    }
}

public final class ActiveRunStore: Sendable {
    private let store: KeyValueStore
    private let key = "active_run"

    public init(store: KeyValueStore) {
        self.store = store
    }

    public var current: ActiveRun? { store.value(ActiveRun.self, forKey: key) }

    public func save(_ run: ActiveRun) {
        store.setValue(run, forKey: key)
    }

    public func clear() {
        store.set(nil, forKey: key)
    }
}
