import XCTest
@testable import VSMCore

final class APIClientTests: XCTestCase {
    func testServerErrorKeepsCodeAndMessage() async {
        let transport = StubTransport { _ in (409, errorBody("time_expired", "Время на решение истекло")) }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens()))
        do {
            let _: RunState = try await api.get("/api/runs/1")
            XCTFail("ожидалась ошибка")
        } catch let error as APIError {
            XCTAssertEqual(error, .server(status: 409, code: "time_expired", message: "Время на решение истекло"))
            XCTAssertEqual(error.userMessage, "Время на решение истекло")
        } catch {
            XCTFail("\(error)")
        }
    }

    func testNetworkFailureIsOffline() async {
        let transport = StubTransport { _ in throw URLError(.notConnectedToInternet) }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens()))
        await XCTAssertThrowsErrorAsync(try await api.get("/api/profile") as Profile) { XCTAssertEqual($0 as? APIError, .offline) }
    }

    func testUnexpectedBodyIsInvalidResponse() async {
        let transport = StubTransport { _ in (200, Data("{}".utf8)) }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens()))
        await XCTAssertThrowsErrorAsync(try await api.get("/api/profile") as Profile) { XCTAssertEqual($0 as? APIError, .invalidResponse) }
    }

    func testExpiredAccessTokenIsRefreshedOnceAndRequestRetried() async throws {
        let store = InMemoryTokenStore(tokens(access: "old", refresh: "r1"))
        let transport = StubTransport { request in
            if request.url!.path == "/api/auth/refresh" { return (200, tokensJSON(access: "new", refresh: "r2")) }
            let bearer = request.value(forHTTPHeaderField: "Authorization")
            return bearer == "Bearer new" ? (200, Fixture.data("profile")) : (401, errorBody("invalid_token"))
        }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: store)

        // Три запроса одновременно получают 401, но обмен токена должен пройти один раз:
        // второй обмен предъявил бы уже использованный токен, и сервер закрыл бы все сессии
        async let a: Profile = api.get("/api/profile")
        async let b: Profile = api.get("/api/profile")
        async let c: Profile = api.get("/api/profile")
        _ = try await (a, b, c)

        XCTAssertEqual(transport.paths.filter { $0 == "/api/auth/refresh" }.count, 1)
        XCTAssertEqual(store.load()?.refreshToken, "r2")
    }

    func testRejectedRefreshEndsSession() async {
        let store = InMemoryTokenStore(tokens())
        let expired = ExpectationFlag()
        let transport = StubTransport { request in
            request.url!.path == "/api/auth/refresh" ? (401, errorBody("invalid_refresh_token")) : (401, errorBody("invalid_token"))
        }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: store) { expired.raise() }
        await XCTAssertThrowsErrorAsync(try await api.get("/api/profile") as Profile) { XCTAssertEqual($0 as? APIError, .sessionExpired) }
        XCTAssertNil(store.load(), "токены удалены с устройства")
        XCTAssertTrue(expired.raised)
    }

    func testOfflineRefreshKeepsSession() async {
        let store = InMemoryTokenStore(tokens())
        let transport = StubTransport { request in
            if request.url!.path == "/api/auth/refresh" { throw URLError(.timedOut) }
            return (401, errorBody("invalid_token"))
        }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: store)
        await XCTAssertThrowsErrorAsync(try await api.get("/api/profile") as Profile) { XCTAssertEqual($0 as? APIError, .offline) }
        XCTAssertNotNil(store.load(), "из-за плохой связи из аккаунта не выкидываем")
    }

    func testLoginStoresSessionAndSendsRequestId() async throws {
        let store = InMemoryTokenStore()
        let transport = StubTransport { _ in (200, tokensJSON(access: "a", refresh: "r")) }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: store)
        _ = try await api.login(personnelNumber: "100002", password: "demo2026")
        XCTAssertEqual(store.load()?.accessToken, "a")
        XCTAssertNotNil(transport.requests.first?.value(forHTTPHeaderField: "X-Request-ID"))
        XCTAssertNil(transport.requests.first?.value(forHTTPHeaderField: "Authorization"))
    }

    func testLogoutClearsSessionEvenOffline() async {
        let store = InMemoryTokenStore(tokens())
        let api = APIClient(baseURL: baseURL, transport: StubTransport { _ in throw URLError(.notConnectedToInternet) }, tokens: store)
        await api.logout()
        XCTAssertNil(store.load())
    }
}

final class ClientEventTests: XCTestCase {
    func testClientEventCarriesPlatform() async throws {
        let transport = StubTransport { _ in (202, Data(#"{"status": "accepted"}"#.utf8)) }
        let api = APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens()))
        await RemoteTrainerRepository(api: api, cache: InMemoryResponseCache()).recordEvent("run_exited", runId: "r1", notificationId: nil)
        let body = try XCTUnwrap(transport.requests.first?.httpBody)
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: Any])
        XCTAssertEqual(json["platform"] as? String, "ios", "сервер отклоняет событие без платформы")
        XCTAssertEqual(json["run_id"] as? String, "r1")
    }
}

final class ExpectationFlag: @unchecked Sendable {
    private let lock = NSLock()
    private var value = false
    var raised: Bool { lock.withLock { value } }
    func raise() { lock.withLock { value = true } }
}

func XCTAssertThrowsErrorAsync<T>(
    _ expression: @autoclosure () async throws -> T, file: StaticString = #filePath, line: UInt = #line, _ handler: (Error) -> Void
) async {
    do {
        _ = try await expression()
        XCTFail("ожидалась ошибка", file: file, line: line)
    } catch {
        handler(error)
    }
}
