import SwiftUI
import VSMCore

/// Корень приложения: вход или вкладки, в зависимости от сессии.
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
            case .signedIn:
                MainTabView(container: container)
                    .task { await container.notifications.requestPermission() }
            }
        }
        .tint(Palette.brand)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await container.syncNotifications() } }
        }
    }
}

struct MainTabView: View {
    let container: AppContainer

    var body: some View {
        TabView {
            NavigationStack { HomeView(container: container) }
                .tabItem { Label("Главная", systemImage: "house") }
            NavigationStack { ScenarioListView(container: container) }
                .tabItem { Label("Сценарии", systemImage: "tram") }
            NavigationStack { LeaderboardView(repository: container.trainer) }
                .tabItem { Label("Рейтинг", systemImage: "trophy") }
            NavigationStack { ProfileView(container: container) }
                .tabItem { Label("Профиль", systemImage: "person.crop.circle") }
        }
    }
}
