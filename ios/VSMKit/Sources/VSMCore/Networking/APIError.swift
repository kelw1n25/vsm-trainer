import Foundation

/// Ошибка обращения к backend. Сервер всегда отвечает `{"detail": {"code", "message"}}`:
/// по `code` клиент решает, что делать, а `message` показывает человеку.
public enum APIError: Error, Equatable, Sendable {
    /// Нет сети, таймаут, обрыв — запрос можно повторить.
    case offline
    /// Сервер ответил ошибкой со своим кодом.
    case server(status: Int, code: String, message: String)
    /// Сессия закончилась и обновить её не удалось — нужен вход.
    case sessionExpired
    /// Ответ не совпал с ожидаемой схемой — ошибка версии клиента или сервера.
    case invalidResponse

    public var code: String? {
        if case let .server(_, code, _) = self { return code }
        return nil
    }

    public var userMessage: String {
        switch self {
        case .offline:
            "Нет связи с сервером. Проверьте интернет и повторите."
        case let .server(status, _, message):
            status >= 500 ? "Сервер временно недоступен. \(message)" : message
        case .sessionExpired:
            "Сессия истекла, войдите заново."
        case .invalidResponse:
            "Не удалось прочитать ответ сервера. Обновите приложение."
        }
    }
}

struct ErrorEnvelope: Decodable {
    struct Detail: Decodable {
        let code: String
        let message: String
    }

    let detail: Detail
}
