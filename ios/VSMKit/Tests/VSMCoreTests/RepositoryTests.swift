import XCTest
@testable import VSMCore

final class RepositoryTests: XCTestCase {
    func testOfflineReturnsCachedResponseMarkedStale() async throws {
        let online = ExpectationFlag()
        let transport = StubTransport { _ in
            if online.raised { throw URLError(.notConnectedToInternet) }
            return (200, Fixture.data("profile"))
        }
        let cache = InMemoryResponseCache()
        let repository = RemoteTrainerRepository(api: APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens())), cache: cache)

        let fresh = try await repository.profile()
        XCTAssertNil(fresh.staleSince)
        online.raise()  // связь пропала
        let saved = try await repository.profile()
        XCTAssertEqual(saved.value, fresh.value)
        XCTAssertNotNil(saved.staleSince)
    }

    func testOfflineWithoutCacheFails() async {
        let transport = StubTransport { _ in throw URLError(.notConnectedToInternet) }
        let repository = RemoteTrainerRepository(api: APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens())), cache: InMemoryResponseCache())
        await XCTAssertThrowsErrorAsync(try await repository.scenarios()) { XCTAssertEqual($0 as? APIError, .offline) }
    }

    func testServerErrorIsNotHiddenByCache() async throws {
        let failing = ExpectationFlag()
        let transport = StubTransport { _ in failing.raised ? (500, errorBody("internal_error")) : (200, Fixture.data("analytics")) }
        let repository = RemoteTrainerRepository(api: APIClient(baseURL: baseURL, transport: transport, tokens: InMemoryTokenStore(tokens())), cache: InMemoryResponseCache())
        _ = try await repository.analytics()
        failing.raise()
        await XCTAssertThrowsErrorAsync(try await repository.analytics()) { XCTAssertEqual(($0 as? APIError)?.code, "internal_error") }
    }

    func testFileCacheSurvivesNewInstance() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        FileResponseCache(directory: directory).write(Data("[]".utf8), for: "scenarios")
        XCTAssertEqual(FileResponseCache(directory: directory).read("scenarios")?.data, Data("[]".utf8))
        try? FileManager.default.removeItem(at: directory)
    }
}
