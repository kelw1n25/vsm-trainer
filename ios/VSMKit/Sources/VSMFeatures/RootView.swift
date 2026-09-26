import SwiftUI
import VSMCore

/// Корень приложения: вход или приложение, в зависимости от сессии; тема и «Уменьшить анимацию» — из настроек.
public struct RootView: View {
    private let container: AppContainer
    @Environment(\.scenePhase) private var scenePhase

    public init(container: AppContainer) {
        self.container = container
    }

    public var body: some View {
        Group {
            switch container.session.state {
            case .signedOut:
                LoginView(session: container.session)
            case let .signedIn(_, fullName, role):
                AppShell(container: container, fullName: fullName, role: role)
                    .task { await container.notifications.requestPermission() }
            }
        }
        .modifier(VsmTheme(theme: container.preferences.theme, reduceMotion: container.preferences.reduceMotion))
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await container.syncNotifications() } }
        }
    }
}
