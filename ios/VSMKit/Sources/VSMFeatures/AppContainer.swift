import Foundation
import VSMCore

/// Сборка зависимостей приложения в одном месте: какой адрес API, где токены и кэш, какие репозитории.
/// ViewModel получают зависимости через init — поэтому в тестах их подменяют без магии.
@MainActor
public final class AppContainer {
    public let api: APIClient
    public let trainer: TrainerRepository
    public let runs: RunRepository
    public let activeRuns: ActiveRunStore
    public let session: SessionViewModel
    public let notificationSync: NotificationSync
    public let notifications = SystemNotifications()

    public init(baseURL: URL) {
        let tokens = KeychainTokenStore()
        let settings = UserDefaultsStore()
        let relay = SessionExpiryRelay()
        api = APIClient(baseURL: baseURL, tokens: tokens) { relay.fire() }
        trainer = RemoteTrainerRepository(api: api, cache: FileResponseCache())
        runs = RemoteRunRepository(api: api)
        activeRuns = ActiveRunStore(store: settings)
        session = SessionViewModel(api: api, tokens: tokens, repository: trainer, activeRuns: activeRuns)
        notificationSync = NotificationSync(repository: trainer, store: settings)
        relay.target = session
    }

    /// Новые уведомления сервера → системные баннеры. Вызывается при возврате в приложение и фоновой задачей.
    public func syncNotifications() async {
        guard case .signedIn = session.state, let fresh = try? await notificationSync.fresh() else { return }
        await notifications.show(fresh)
    }
}

/// Сессия истекла внутри сетевого слоя — сообщаем экрану на главном потоке.
final class SessionExpiryRelay: @unchecked Sendable {
    weak var target: SessionViewModel?

    func fire() {
        Task { @MainActor [weak self] in self?.target?.sessionExpired() }
    }
}
