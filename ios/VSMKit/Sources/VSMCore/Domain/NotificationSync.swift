import Foundation

/// Какие уведомления ещё не показаны системным баннером. Запоминает наибольший показанный id,
/// чтобы одно и то же уведомление не всплывало при каждой фоновой синхронизации.
public final class NotificationSync: Sendable {
    private let repository: TrainerRepository
    private let store: KeyValueStore
    private let key = "last_notified_id"

    public init(repository: TrainerRepository, store: KeyValueStore) {
        self.repository = repository
        self.store = store
    }

    public func fresh() async throws -> [AppNotification] {
        let loaded = try await repository.notifications()
        // Сохранённый без сети список уже показывали — баннеров по нему не делаем
        guard loaded.staleSince == nil else { return [] }
        let lastShown = store.value(Int.self, forKey: key)
        let newest = loaded.value.items.map(\.id).max()
        if let newest { store.setValue(max(newest, lastShown ?? 0), forKey: key) }
        // При первом запуске не засыпаем баннерами за всю историю — только отмечаем, с чего начинать
        guard let lastShown else { return [] }
        return loaded.value.items.filter { !$0.read && $0.id > lastShown }.sorted { $0.id < $1.id }
    }
}
