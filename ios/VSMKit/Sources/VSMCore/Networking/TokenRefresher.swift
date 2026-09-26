import Foundation

/// Обновляет сессию по refresh-токену. Если на 401 одновременно наткнулись несколько запросов,
/// обмен выполняется один раз, а остальные ждут его результат: иначе второй запрос предъявил бы уже
/// обменянный токен, и сервер закрыл бы все сессии как при краже.
actor TokenRefresher {
    private let store: TokenStore
    private let exchange: @Sendable (String) async throws -> SessionTokens
    private var inFlight: Task<SessionTokens, Error>?

    init(store: TokenStore, exchange: @escaping @Sendable (String) async throws -> SessionTokens) {
        self.store = store
        self.exchange = exchange
    }

    /// Новый access-токен. `failedToken` — тот, с которым пришёл 401: если его уже заменили, повторно не обмениваем.
    func refresh(after failedToken: String?) async throws -> String {
        if let current = store.load(), current.accessToken != failedToken {
            return current.accessToken
        }
        if let inFlight {
            return try await inFlight.value.accessToken
        }
        guard let refreshToken = store.load()?.refreshToken else { throw APIError.sessionExpired }
        let task = Task { try await exchange(refreshToken) }
        inFlight = task
        defer { inFlight = nil }
        do {
            let tokens = try await task.value
            store.save(tokens)
            return tokens.accessToken
        } catch let error as APIError where error != .offline {
            // Сервер отказал в обмене — сессии больше нет. Офлайн сессию не сбрасывает
            store.clear()
            throw APIError.sessionExpired
        }
    }
}
