import BackgroundTasks
import SwiftUI
import UIKit
import VSMFeatures

@main
struct VSMTrainerApp: App {
    /// Идентификатор фоновой задачи — тот же, что в `BGTaskSchedulerPermittedIdentifiers` в Info.plist.
    private static let refreshTask = "ru.vsm.trainer.notifications"
    // Без сохранённого выбора тема следует системной, как на сайте
    @State private var container = AppContainer(baseURL: AppConfig.apiBaseURL, systemDark: UITraitCollection.current.userInterfaceStyle == .dark)
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { Self.scheduleRefresh() }
        }
        .backgroundTask(.appRefresh(Self.refreshTask)) {
            // Система будит приложение примерно раз в 15 минут и позже: забираем новые уведомления сервера
            Self.scheduleRefresh()
            await container.syncNotifications()
        }
    }

    private static func scheduleRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: refreshTask)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }
}

enum AppConfig {
    /// Адрес backend из настройки сборки `API_BASE_URL` (Debug — локальный сервер, Release — HTTPS).
    static var apiBaseURL: URL {
        let raw = Bundle.main.object(forInfoDictionaryKey: "APIBaseURL") as? String ?? ""
        guard let url = URL(string: raw), url.scheme != nil else {
            preconditionFailure("В Info.plist не задан APIBaseURL — проверьте настройку сборки API_BASE_URL")
        }
        return url
    }
}
