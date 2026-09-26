import Foundation

/// Где лежит сессия. В приложении — Keychain, в тестах — память.
public protocol TokenStore: Sendable {
    func load() -> SessionTokens?
    func save(_ tokens: SessionTokens)
    func clear()
}

public final class InMemoryTokenStore: TokenStore, @unchecked Sendable {
    private let lock = NSLock()
    private var tokens: SessionTokens?

    public init(_ tokens: SessionTokens? = nil) {
        self.tokens = tokens
    }

    public func load() -> SessionTokens? {
        lock.withLock { tokens }
    }

    public func save(_ tokens: SessionTokens) {
        lock.withLock { self.tokens = tokens }
    }

    public func clear() {
        lock.withLock { tokens = nil }
    }
}
