import Foundation

/// Небольшие настройки устройства: последнее показанное уведомление, незавершённый сценарий.
/// Секреты сюда не кладутся — для них `TokenStore`.
public protocol KeyValueStore: Sendable {
    func data(forKey key: String) -> Data?
    func set(_ data: Data?, forKey key: String)
}

public final class UserDefaultsStore: KeyValueStore, @unchecked Sendable {
    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public func data(forKey key: String) -> Data? {
        defaults.data(forKey: key)
    }

    public func set(_ data: Data?, forKey key: String) {
        defaults.set(data, forKey: key)
    }
}

public final class InMemoryKeyValueStore: KeyValueStore, @unchecked Sendable {
    private let lock = NSLock()
    private var values: [String: Data] = [:]

    public init() {}

    public func data(forKey key: String) -> Data? {
        lock.withLock { values[key] }
    }

    public func set(_ data: Data?, forKey key: String) {
        lock.withLock { values[key] = data }
    }
}

extension KeyValueStore {
    func value<T: Decodable>(_ type: T.Type, forKey key: String) -> T? {
        data(forKey: key).flatMap { try? JSONDecoder.api.decode(T.self, from: $0) }
    }

    func setValue<T: Encodable>(_ value: T?, forKey key: String) {
        set(value.flatMap { try? JSONEncoder.api.encode($0) }, forKey: key)
    }
}
