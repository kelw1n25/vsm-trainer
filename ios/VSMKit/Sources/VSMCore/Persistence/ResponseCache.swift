import Foundation

/// Последний успешный ответ каждого экрана — чтобы без сети показать сохранённые данные, а не пустоту.
/// Лежит в каталоге кэшей: система может его очистить, и это не страшно — источник правды на сервере.
public protocol ResponseCache: Sendable {
    func read(_ key: String) -> (data: Data, savedAt: Date)?
    func write(_ data: Data, for key: String)
    func removeAll()
}

public final class FileResponseCache: ResponseCache, @unchecked Sendable {
    private let directory: URL
    private let fileManager = FileManager.default

    public init(directory: URL? = nil) {
        self.directory = directory
            ?? FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("api-cache", isDirectory: true)
        try? fileManager.createDirectory(at: self.directory, withIntermediateDirectories: true)
    }

    private func file(_ key: String) -> URL {
        directory.appendingPathComponent(key.replacingOccurrences(of: "/", with: "_") + ".json")
    }

    public func read(_ key: String) -> (data: Data, savedAt: Date)? {
        let url = file(key)
        guard let data = try? Data(contentsOf: url),
              let savedAt = (try? fileManager.attributesOfItem(atPath: url.path))?[.modificationDate] as? Date
        else { return nil }
        return (data, savedAt)
    }

    public func write(_ data: Data, for key: String) {
        try? data.write(to: file(key), options: .atomic)
    }

    public func removeAll() {
        try? fileManager.removeItem(at: directory)
        try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
    }
}

public final class InMemoryResponseCache: ResponseCache, @unchecked Sendable {
    private let lock = NSLock()
    private var entries: [String: (Data, Date)] = [:]
    private let now: @Sendable () -> Date

    public init(now: @escaping @Sendable () -> Date = { Date() }) {
        self.now = now
    }

    public func read(_ key: String) -> (data: Data, savedAt: Date)? {
        lock.withLock { entries[key].map { (data: $0.0, savedAt: $0.1) } }
    }

    public func write(_ data: Data, for key: String) {
        lock.withLock { entries[key] = (data, now()) }
    }

    public func removeAll() {
        lock.withLock { entries.removeAll() }
    }
}
