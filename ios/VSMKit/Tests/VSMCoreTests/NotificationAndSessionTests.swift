import XCTest
@testable import VSMCore

final class NotificationSyncTests: XCTestCase {
    func item(_ id: Int, read: Bool = false) -> AppNotification {
        AppNotification(id: id, type: "new_scenario", title: "Новый сценарий", body: "…", createdAt: Date(), read: read)
    }

    func testFirstSyncOnlyRemembersPosition() async throws {
        let repository = FakeTrainerRepository()
        repository.notificationsResult = .success(Loaded(value: NotificationList(unread: 2, items: [item(1), item(2)])))
        let sync = NotificationSync(repository: repository, store: InMemoryKeyValueStore())
        let shown = try await sync.fresh()
        XCTAssertEqual(shown, [])
    }

    func testOnlyNewUnreadAreShownOnce() async throws {
        let repository = FakeTrainerRepository()
        let sync = NotificationSync(repository: repository, store: InMemoryKeyValueStore())
        repository.notificationsResult = .success(Loaded(value: NotificationList(unread: 1, items: [item(1)])))
        _ = try await sync.fresh()

        repository.notificationsResult = .success(Loaded(value: NotificationList(unread: 3, items: [item(4), item(3, read: true), item(2), item(1)])))
        let firstIds = try await sync.fresh().map(\.id)
        XCTAssertEqual(firstIds, [2, 4])
        let secondIds = try await sync.fresh().map(\.id)
        XCTAssertEqual(secondIds, [])
    }

    func testCachedListProducesNoBanners() async throws {
        let repository = FakeTrainerRepository()
        let store = InMemoryKeyValueStore()
        let sync = NotificationSync(repository: repository, store: store)
        repository.notificationsResult = .success(Loaded(value: NotificationList(unread: 0, items: [item(1)])))
        _ = try await sync.fresh()
        repository.notificationsResult = .success(Loaded(value: NotificationList(unread: 1, items: [item(5)]), staleSince: Date()))
        let shown = try await sync.fresh()
        XCTAssertEqual(shown, [])
    }
}

@MainActor
final class SessionViewModelTests: XCTestCase {
    func testLoginAndExpiry() async {
        let store = InMemoryTokenStore()
        let transport = StubTransport { _ in (200, tokensJSON(access: "a", refresh: "r")) }
        let repository = FakeTrainerRepository()
        let vm = SessionViewModel(api: APIClient(baseURL: baseURL, transport: transport, tokens: store), tokens: store, repository: repository)

        XCTAssertEqual(vm.state, .signedOut)
        XCTAssertFalse(vm.canSubmit)
        vm.personnelNumber = " 100002 "
        vm.password = "demo2026"
        await vm.submit()
        XCTAssertEqual(vm.state, .signedIn(employeeId: 2, fullName: "Синтетический Проводник", role: .conductor))
        XCTAssertEqual(vm.password, "", "пароль не остаётся в памяти экрана")

        vm.sessionExpired()
        XCTAssertEqual(vm.state, .signedOut)
        XCTAssertTrue(repository.cacheCleared, "данные прежнего сотрудника удалены")
    }

    func testWrongPasswordMessage() async {
        let store = InMemoryTokenStore()
        let transport = StubTransport { _ in (401, errorBody("invalid_credentials", "Неверный табельный номер или пароль")) }
        let vm = SessionViewModel(api: APIClient(baseURL: baseURL, transport: transport, tokens: store), tokens: store, repository: FakeTrainerRepository())
        vm.personnelNumber = "100002"
        vm.password = "wrong"
        await vm.submit()
        XCTAssertEqual(vm.errorMessage, "Неверный табельный номер или пароль")
        XCTAssertEqual(vm.state, .signedOut)
    }
}
