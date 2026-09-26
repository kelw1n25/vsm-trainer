import Observation
import SwiftUI
import VSMCore

enum Tab: CaseIterable {
    case home, scenarios, rating, analytics, handbook

    var title: String {
        switch self {
        case .home: "Главная"
        case .scenarios: "Сценарии"
        case .rating: "Рейтинг"
        case .analytics: "Аналитика"
        case .handbook: "Справочник"
        }
    }
}

/// Маршруты сайта поверх вкладок: `/profile`, `/scenarios/:id`, `/runs/:id/debrief` и т. д.
enum Route: Hashable {
    case profile, notifications, settings, team
    case member(Int)
    case scenario(String)
    case map(String)
    case debrief(String)

    /// Как NavLink сайта: страница сценария и его развитие подсвечивают «Сценарии».
    var tab: Tab? {
        switch self {
        case .scenario, .map: .scenarios
        default: nil
        }
    }
}

/// Новелла на весь экран — `/runs/:id` сайта без шапки и навигации.
struct StoryRoute: Identifiable, Hashable {
    let scenarioId: String
    let id = UUID()
}

/// Куда перейти: вкладка, стек страниц над ней и открытая новелла.
@MainActor
@Observable
final class Navigator {
    var tab: Tab = .home
    var path: [Route] = []
    var story: StoryRoute?
    /// Номер ситуации для справочника — из ссылки «Ситуация N» со страницы сценария.
    var handbookFocus: Int?

    var highlighted: Tab? {
        guard let last = path.last else { return tab }
        return last.tab
    }

    func open(_ route: Route) {
        path.append(route)
    }

    func openTab(_ tab: Tab) {
        self.tab = tab
        path = []
    }

    func back() {
        if !path.isEmpty { path.removeLast() }
    }

    func play(_ scenarioId: String) {
        story = StoryRoute(scenarioId: scenarioId)
    }

    func situation(_ number: Int) {
        handbookFocus = number
        openTab(.handbook)
    }

    /// Из финала — путь или разбор; «назад» из них ведёт туда, откуда начинали, а не в законченную историю.
    func leaveStory(to route: Route?) {
        story = nil
        if let route { path.append(route) }
    }
}

/// Приложение после входа: шапка, страницы, нижняя навигация, новелла поверх.
struct AppShell: View {
    let container: AppContainer
    let fullName: String
    let role: Role
    @State private var navigator = Navigator()
    @State private var shell: ShellViewModel
    @State private var avatar: MyAvatarStore
    @Environment(\.vsm) private var colors

    init(container: AppContainer, fullName: String, role: Role) {
        self.container = container
        self.fullName = fullName
        self.role = role
        _shell = State(initialValue: ShellViewModel(repository: container.trainer))
        _avatar = State(initialValue: MyAvatarStore(repository: container.trainer))
    }

    var body: some View {
        VStack(spacing: 0) {
            AppHeader(
                fullName: fullName,
                role: role,
                unread: shell.unread,
                dark: container.preferences.theme == .dark,
                toggleTheme: { container.preferences.toggleTheme() },
                logout: { Task { await container.session.signOut() } }
            )
            NavigationStack(path: $navigator.path) {
                root(navigator.tab)
                    .id(navigator.tab)
                    .navigationDestination(for: Route.self) { route in
                        destination(route).hideSystemBar()
                    }
                    .hideSystemBar()
            }
            BottomNav(current: navigator.highlighted) { navigator.openTab($0) }
        }
        .background(SiteBackground())
        .environment(navigator)
        .environment(avatar)
        .environment(\.myAvatar, avatar.current)
        .task { await avatar.refresh() }
        .task { await shell.poll() }
        .fullScreenCoverCompat(item: $navigator.story) { story in
            StoryPlayerScreen(container: container, scenarioId: story.scenarioId)
                .modifier(VsmTheme(theme: container.preferences.theme, reduceMotion: container.preferences.reduceMotion))
                .environment(navigator)
        }
        .onChange(of: navigator.path) { _, _ in Task { await shell.refreshUnread() } }
    }

    @ViewBuilder
    private func root(_ tab: Tab) -> some View {
        switch tab {
        case .home: HomeScreen(container: container, fullName: fullName)
        case .scenarios: ScenarioListScreen(container: container)
        case .rating: RatingScreen(container: container)
        case .analytics: AnalyticsScreen(container: container, employeeId: nil)
        case .handbook: HandbookScreen(container: container)
        }
    }

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .profile: ProfileScreen(container: container)
        case .notifications: NotificationsScreen(container: container) { Task { await shell.refreshUnread() } }
        case .settings: SettingsScreen(container: container)
        case .team: TeamScreen(container: container)
        case let .member(id): AnalyticsScreen(container: container, employeeId: id)
        case let .scenario(id): ScenarioDetailScreen(container: container, scenarioId: id)
        case let .map(id): StoryMapScreen(container: container, scenarioId: id)
        case let .debrief(id): DebriefScreen(container: container, runId: id)
        }
    }
}

extension View {
    /// Системную навигационную панель не показываем: вместо неё шапка и «← назад» сайта.
    func hideSystemBar() -> some View {
        #if os(iOS)
        toolbar(.hidden, for: .navigationBar).background(SiteBackground())
        #else
        background(SiteBackground())
        #endif
    }

    /// Новелла — на весь экран (на macOS, где пакет только компилируется, — листом).
    func fullScreenCoverCompat<Item: Identifiable, Cover: View>(item: Binding<Item?>, @ViewBuilder content: @escaping (Item) -> Cover) -> some View {
        #if os(iOS)
        fullScreenCover(item: item, content: content)
        #else
        sheet(item: item, content: content)
        #endif
    }
}

// MARK: - Шапка

/// Шапка сайта: логотип «ВСМ», ползунок темы, аватар с индикатором «онлайн», бейджем и меню.
struct AppHeader: View {
    let fullName: String
    let role: Role
    let unread: Int
    let dark: Bool
    let toggleTheme: () -> Void
    let logout: () -> Void
    @State private var menuOpen = false
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(spacing: 0) {
            Button { navigator.openTab(.home) } label: {
                HStack(spacing: 12) {
                    LogoMark(width: 46)
                    Text("ВСМ").textStyle(VsmType.brandName).foregroundStyle(colors.brandName)
                }
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("ВСМ — на главную")
            .accessibilityAddTraits(.isButton)
            Spacer()
            ThemeToggle(dark: dark, toggle: toggleTheme)
            Spacer().frame(width: 14)
            Button { menuOpen.toggle() } label: {
                HStack(spacing: 6) {
                    UserAvatar(size: 52)
                        .overlay(alignment: .topTrailing) { OnlineDot().offset(x: -2, y: 2) }
                        .overlay(alignment: .bottomTrailing) {
                            if unread > 0 {
                                Text("\(unread)")
                                    .textStyle(TextStyle(11, .bold, line: 14))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 5)
                                    .frame(minWidth: 20, minHeight: 20)
                                    .background(colors.brand, in: Capsule())
                                    .overlay(Capsule().strokeBorder(colors.surface, lineWidth: 2))
                                    .offset(x: 4, y: 2)
                            }
                        }
                    Icon(kind: .chevronDown, size: 20, color: colors.navText)
                        .rotationEffect(.degrees(menuOpen ? 180 : 0))
                        .animation(.easeOut(duration: 0.2), value: menuOpen)
                }
                .padding(4)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Меню пользователя, непрочитанных уведомлений: \(unread)")
            .popover(isPresented: $menuOpen, attachmentAnchor: .point(.bottom), arrowEdge: .top) {
                UserMenu(fullName: fullName, role: role, unread: unread) { action in
                    menuOpen = false
                    switch action {
                    case let .route(route): navigator.open(route)
                    case .logout: logout()
                    }
                }
                .presentationCompactAdaptation(.popover)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

/// Выпадающее меню аватара сайта: имя и роль, Профиль, Уведомления со счётчиком, Настройки, Команда, Выйти.
private struct UserMenu: View {
    enum Action {
        case route(Route)
        case logout
    }

    let fullName: String
    let role: Role
    let unread: Int
    let select: (Action) -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text(fullName).textStyle(VsmType.smallStrong.weighted(.bold)).foregroundStyle(colors.text)
                Text(Labels.role(role)).textStyle(VsmType.small).foregroundStyle(colors.muted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            Rectangle().fill(colors.border).frame(height: 1)
            item("Профиль") { select(.route(.profile)) }
            item("Уведомления", badge: unread) { select(.route(.notifications)) }
            item("Настройки") { select(.route(.settings)) }
            if role == .instructor { item("Команда") { select(.route(.team)) } }
            item("Выйти", color: colors.negative) { select(.logout) }
        }
        .padding(.vertical, 6)
        .frame(width: 270)
        .background(colors.surface)
    }

    private func item(_ title: String, badge: Int = 0, color: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title).textStyle(VsmType.bodyStrong.weighted(.medium)).foregroundStyle(color ?? colors.text)
                Spacer()
                if badge > 0 { CountBadge(count: badge) }
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Зелёная точка «онлайн» с расходящимся кольцом (`ping`, 2.2 с).
private struct OnlineDot: View {
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        ZStack {
            if !reduce {
                TimelineView(.animation) { timeline in
                    let t = timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 2.2) / 2.2
                    Circle().strokeBorder(Palette.online, lineWidth: 2).scaleEffect(1 + 1.3 * t).opacity(1 - t)
                }
            }
            Circle().fill(Palette.online).overlay(Circle().strokeBorder(colors.surface, lineWidth: 2))
        }
        .frame(width: 12, height: 12)
        .accessibilityHidden(true)
    }
}

/// Ползунок темы: солнце — светлая, луна — тёмная; бегунок едет, как на сайте.
struct ThemeToggle: View {
    let dark: Bool
    let toggle: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        Button(action: toggle) {
            ZStack(alignment: .leading) {
                Capsule().fill(colors.surface)
                Circle()
                    .fill(LinearGradient(colors: dark ? [Palette.moonTop, Palette.moonBottom] : [Palette.sunTop, Palette.sunBottom], startPoint: .top, endPoint: .bottom))
                    .frame(width: 26, height: 26)
                    .padding(3)
                    .offset(x: dark ? 34 : 0)
                HStack {
                    Icon(kind: .sun, size: 16, color: dark ? colors.muted : .white)
                    Spacer()
                    Icon(kind: .moon, size: 16, color: dark ? .white : colors.muted)
                }
                .padding(.horizontal, 8)
            }
            .frame(width: 68, height: 34)
            .overlay(Capsule().strokeBorder(colors.border, lineWidth: 1))
            .vsmShadow(radius: 6, y: 3)
            .animation(.easeOut(duration: 0.3), value: dark)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Тёмная тема")
        .accessibilityValue(dark ? "включена" : "выключена")
    }
}

// MARK: - Нижняя навигация

/// Навигация сайта (`.nav`): белая пилюля с тенью, текстовые пункты, активный — цвет heading и синяя полоса снизу.
/// На телефоне она внизу экрана — под большой палец; Профиль открывается по аватару в шапке, как в меню сайта.
struct BottomNav: View {
    let current: Tab?
    let select: (Tab) -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        // Ширина пункта — по длине подписи: «Справочник» длиннее «Главной» (как веса пунктов в Android)
        let weights = Tab.allCases.map { CGFloat($0.title.count + 4) }
        let total = weights.reduce(0, +)
        GeometryReader { proxy in
            HStack(spacing: 0) {
                ForEach(Array(Tab.allCases.enumerated()), id: \.element) { index, tab in
                    let active = tab == current
                    Button { select(tab) } label: {
                        Text(tab.title)
                            .textStyle(active ? VsmType.navActive : VsmType.nav)
                            .foregroundStyle(active ? colors.heading : colors.navText)
                            .lineLimit(1)
                            .minimumScaleFactor(0.85)
                            .padding(.horizontal, 4)
                            // Полоска — сразу под подписью и чуть шире её, как `.nav a.active::after` сайта; у нижнего края
                            // пилюли её срезало бы скругление крайних пунктов («Главная», «Справочник»)
                            .overlay(alignment: .bottom) {
                                Capsule()
                                    .fill(colors.brand)
                                    .frame(height: 3)
                                    .offset(y: 8)
                                    .scaleEffect(x: active ? 1 : 0, anchor: .center)
                                    .opacity(active ? 1 : 0)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .frame(width: proxy.size.width * weights[index] / total)
                    .accessibilityAddTraits(active ? [.isSelected, .isButton] : .isButton)
                }
            }
        }
        .padding(.horizontal, 8)
        .animation(.easeOut(duration: 0.35), value: current)
        .frame(height: 58)
        .background(colors.surface, in: RoundedRectangle(cornerRadius: Radius.nav, style: .continuous))
        .vsmShadow(radius: 10, y: 4)
        .padding(.horizontal, 12)
        .padding(.top, 8)
        .padding(.bottom, 4)
    }
}

#if os(iOS)
import UIKit

/// Системная панель навигации скрыта (вместо неё шапка сайта), а вместе с ней iOS отключает жест «смахнуть назад».
/// Возвращаем жест: страницы без «← назад» закрываются так же, как в любом приложении iOS.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }
}
#endif
