import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Клиент REST API тренажёра. Знает адреса, формат ошибок и авторизацию; бизнес-логики в нём нет.
public final class APIClient: Sendable {
    private let baseURL: URL
    private let transport: HTTPTransport
    private let tokens: TokenStore
    private let refresher: TokenRefresher
    /// Вызывается, когда сессию восстановить нельзя, — приложение показывает вход.
    private let onSessionExpired: @Sendable () -> Void

    public init(
        baseURL: URL,
        transport: HTTPTransport = URLSessionTransport(),
        tokens: TokenStore,
        onSessionExpired: @escaping @Sendable () -> Void = {}
    ) {
        self.baseURL = baseURL
        self.transport = transport
        self.tokens = tokens
        self.onSessionExpired = onSessionExpired
        refresher = TokenRefresher(store: tokens) { refreshToken in
            try await APIClient.exchange(refreshToken, baseURL: baseURL, transport: transport)
        }
    }

    // MARK: Аутентификация

    public func login(personnelNumber: String, password: String) async throws -> SessionTokens {
        let session: SessionTokens = try await send(
            "POST", "/api/auth/login", body: LoginRequest(personnelNumber: personnelNumber, password: password), authorized: false
        )
        tokens.save(session)
        return session
    }

    /// Выход: отзываем refresh-токен на сервере; локально сессия удаляется даже без сети.
    public func logout() async {
        if let refreshToken = tokens.load()?.refreshToken {
            let _: Status? = try? await send("POST", "/api/auth/logout", body: RefreshRequest(refreshToken: refreshToken), authorized: false)
        }
        tokens.clear()
    }

    // MARK: Запросы

    func get<Response: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> Response {
        try await send("GET", path, query: query, body: Optional<Empty>.none)
    }

    func post<Body: Encodable, Response: Decodable>(_ path: String, body: Body) async throws -> Response {
        try await send("POST", path, body: body)
    }

    private func send<Body: Encodable, Response: Decodable>(
        _ method: String, _ path: String, query: [URLQueryItem] = [], body: Body?, authorized: Bool = true
    ) async throws -> Response {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        components.queryItems = query.isEmpty ? nil : query
        var request = URLRequest(url: components.url!)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        // Сквозной идентификатор: по нему поддержка найдёт запрос в журнале сервера
        request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        if let body {
            request.httpBody = try JSONEncoder.api.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        guard authorized else { return try await perform(request) }

        let accessToken = tokens.load()?.accessToken
        guard let accessToken else {
            onSessionExpired()
            throw APIError.sessionExpired
        }
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        do {
            return try await perform(request)
        } catch let error as APIError where error.isExpiredToken {
            let renewed: String
            do {
                renewed = try await refresher.refresh(after: accessToken)
            } catch APIError.sessionExpired {
                onSessionExpired()
                throw APIError.sessionExpired
            }
            request.setValue("Bearer \(renewed)", forHTTPHeaderField: "Authorization")
            return try await perform(request)
        }
    }

    private func perform<Response: Decodable>(_ request: URLRequest) async throws -> Response {
        let data: Data
        let response: HTTPURLResponse
        do {
            (data, response) = try await transport.send(request)
        } catch {
            throw APIError.offline
        }
        guard (200..<300).contains(response.statusCode) else {
            if let envelope = try? JSONDecoder.api.decode(ErrorEnvelope.self, from: data) {
                throw APIError.server(status: response.statusCode, code: envelope.detail.code, message: envelope.detail.message)
            }
            throw APIError.server(status: response.statusCode, code: "http_\(response.statusCode)", message: "Ошибка сервера \(response.statusCode)")
        }
        do {
            return try JSONDecoder.api.decode(Response.self, from: data)
        } catch {
            throw APIError.invalidResponse
        }
    }

    private static func exchange(_ refreshToken: String, baseURL: URL, transport: HTTPTransport) async throws -> SessionTokens {
        var request = URLRequest(url: baseURL.appendingPathComponent("/api/auth/refresh"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder.api.encode(RefreshRequest(refreshToken: refreshToken))
        let data: Data
        let response: HTTPURLResponse
        do {
            (data, response) = try await transport.send(request)
        } catch {
            throw APIError.offline
        }
        guard response.statusCode == 200 else {
            throw APIError.server(status: response.statusCode, code: "invalid_refresh_token", message: "Сессия истекла")
        }
        guard let tokens = try? JSONDecoder.api.decode(SessionTokens.self, from: data) else { throw APIError.invalidResponse }
        return tokens
    }
}

struct Empty: Codable {}

struct Status: Decodable {
    let status: String
}

extension APIError {
    /// Access-токен истёк или отозван — стоит попробовать обновить сессию.
    var isExpiredToken: Bool {
        if case let .server(401, code, _) = self { return code == "invalid_token" }
        return false
    }
}
