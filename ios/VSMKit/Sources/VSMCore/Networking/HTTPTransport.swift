import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Отправка HTTP-запроса. Отделена от `APIClient`, чтобы в тестах подменять сеть заглушкой.
public protocol HTTPTransport: Sendable {
    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse)
}

public struct URLSessionTransport: HTTPTransport {
    private let session: URLSession

    /// Таймауты короче системных (60 с): в вагоне связь пропадает, и ждать минуту нет смысла.
    public init(requestTimeout: TimeInterval = 15) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = requestTimeout
        configuration.timeoutIntervalForResource = requestTimeout * 2
        session = URLSession(configuration: configuration)
    }

    public func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        return (data, http)
    }
}
