import Foundation
import Observation

/// Загрузка экрана: без сети — сохранённые данные, при ошибке обновления уже показанное не пропадает.
@MainActor
func reload<T>(_ current: ScreenState<T>, _ fetch: () async throws -> Loaded<T>) async -> ScreenState<T> {
    do {
        let loaded = try await fetch()
        return .loaded(loaded.value, staleSince: loaded.staleSince)
    } catch {
        return current.value == nil ? .failed(error.userMessage) : current
    }
}

/// Слайд hero: приветствие, челлендж недели или рекомендация — как `HeroCarousel` сайта.
public struct HeroSlide: Hashable, Sendable {
    public let eyebrow: String
    public let titleTop: String
    public let titleBottom: String
    public let text: String
    public let note: String
}

/// Главная: hero-карусель и «Про ВСМ» — общая картина магистрали.
@MainActor
@Observable
public final class HomeViewModel {
    public struct Content {
        public let slides: [HeroSlide]
        /// Классы обслуживания из справочника кейсодержателя для «Про ВСМ».
        public let serviceClasses: [ServiceClass]
    }

    public private(set) var state: ScreenState<Content> = .loading
    private let repository: TrainerRepository
    private let fullName: String

    public init(repository: TrainerRepository, fullName: String) {
        self.repository = repository
        self.fullName = fullName
    }

    public func load() async {
        let repository = repository
        let name = fullName.isEmpty ? "коллега" : Labels.firstName(fullName)
        state = await reload(state) {
            async let scenarios = repository.scenarios()
            async let meta = try? repository.meta()
            async let analytics = try? repository.analytics()
            // Без справочника главная всё равно открывается — просто без таблицы классов
            async let handbook = try? repository.handbook()
            let loaded = try await scenarios
            var slides = [HeroSlide(
                eyebrow: "Привет, \(name)!", titleTop: "Развивай навыки —", titleBottom: "строй будущее ВСМ!",
                text: "Пройди сценарии, получай баллы, поднимайся в рейтинге и становись экспертом ВСМ.",
                note: "Твой прогресс влияет на общую безопасность!"
            )]
            if let challenge = await meta?.value.weeklyChallenge,
               let scenario = loaded.value.first(where: { $0.id == challenge.scenarioId }) {
                slides.append(HeroSlide(
                    eyebrow: "Челлендж недели", titleTop: "Пройди на успех:", titleBottom: scenario.title,
                    text: "Заверши сценарий успешно до конца недели и получи бонус +\(challenge.bonusXp) XP.",
                    note: "+\(challenge.bonusXp) XP за успешное прохождение!"
                ))
            }
            if let recommendation = await analytics?.value.recommendation {
                slides.append(HeroSlide(
                    eyebrow: "Рекомендация для тебя", titleTop: "Следующий шаг:", titleBottom: recommendation.title,
                    text: recommendation.reason, note: "Закрой пробел — и навык вырастет быстрее!"
                ))
            }
            return Loaded(value: Content(slides: slides, serviceClasses: await handbook?.value.serviceClasses ?? []), staleSince: loaded.staleSince)
        }
    }
}

@MainActor
@Observable
public final class ScenarioListViewModel {
    public private(set) var state: ScreenState<[ScenarioSummary]> = .loading
    public var category: String?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public var visible: [ScenarioSummary] {
        (state.value ?? []).filter { category == nil || $0.category == category }
    }

    /// Категории в порядке первого появления в каталоге — как фильтры сайта.
    public var categories: [String] {
        var seen: [String] = []
        for scenario in state.value ?? [] where !seen.contains(scenario.category) { seen.append(scenario.category) }
        return seen
    }

    public func load() async {
        let repository = repository
        state = await reload(state) { try await repository.scenarios() }
    }
}

/// Страница сценария: описание, что ждёт внутри, свои прошлые попытки — как `ScenarioPage`.
@MainActor
@Observable
public final class ScenarioDetailViewModel {
    public struct Content {
        public let scenario: ScenarioSummary?
        public let attempts: [HistoryItem]
    }

    public let scenarioId: String
    public private(set) var state: ScreenState<Content> = .loading
    private let repository: TrainerRepository

    public init(scenarioId: String, repository: TrainerRepository) {
        self.scenarioId = scenarioId
        self.repository = repository
    }

    public func load() async {
        let repository = repository
        let id = scenarioId
        state = await reload(state) {
            let scenarios = try await repository.scenarios()
            let attempts = (try? await repository.profile().value.history.filter { $0.scenarioId == id }) ?? []
            return Loaded(value: Content(scenario: scenarios.value.first { $0.id == id }, attempts: attempts), staleSince: scenarios.staleSince)
        }
    }
}

/// Развитие истории: открытые развилки и финалы.
@MainActor
@Observable
public final class StoryMapViewModel {
    public struct Content {
        public let title: String
        public let map: StoryMap
    }

    public let scenarioId: String
    public private(set) var state: ScreenState<Content> = .loading
    private let repository: TrainerRepository

    public init(scenarioId: String, repository: TrainerRepository) {
        self.scenarioId = scenarioId
        self.repository = repository
    }

    public func load() async {
        let repository = repository
        let id = scenarioId
        state = await reload(state) {
            let map = try await repository.storyMap(scenarioId: id)
            let title = (try? await repository.scenarios().value.first { $0.id == id }?.title) ?? "Сценарий"
            return Loaded(value: Content(title: title, map: map.value), staleSince: map.staleSince)
        }
    }
}

/// Разбор после сценария: последствия каждого решения, лучший вариант, стандарт по ситуации.
@MainActor
@Observable
public final class DebriefViewModel {
    public private(set) var state: ScreenState<Debrief> = .loading
    public private(set) var competenceTitles: [String: String] = [:]
    private let runId: String
    private let runs: RunRepository
    private let repository: TrainerRepository
    private var reported = false

    public init(runId: String, runs: RunRepository, repository: TrainerRepository) {
        self.runId = runId
        self.runs = runs
        self.repository = repository
    }

    public func title(ofCompetence code: String) -> String {
        competenceTitles[code] ?? code
    }

    public func load() async {
        state = .loading
        do {
            async let meta = try? repository.meta()
            let debrief = try await runs.debrief(runId: runId)
            competenceTitles = await meta?.value.competences ?? [:]
            state = .loaded(debrief, staleSince: nil)
            if !reported {
                reported = true
                await repository.recordEvent("debrief_opened", runId: runId, notificationId: nil)
            }
        } catch {
            state = .failed(error.userMessage)
        }
    }
}

@MainActor
@Observable
public final class ProfileViewModel {
    public private(set) var state: ScreenState<Profile> = .loading
    public private(set) var competenceTitles: [String: String] = [:]
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func title(ofCompetence code: String) -> String {
        competenceTitles[code] ?? code
    }

    public func load() async {
        let repository = repository
        if let meta = try? await repository.meta() { competenceTitles = meta.value.competences }
        state = await reload(state) { try await repository.profile() }
    }
}

@MainActor
@Observable
public final class LeaderboardViewModel {
    public private(set) var state: ScreenState<Leaderboard> = .loading
    public private(set) var scope: LeaderboardScope = .brigade
    public private(set) var period: LeaderboardPeriod = .week
    /// Свой уровень — для карточки «Ваш уровень» над таблицей.
    public private(set) var level: Level?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func select(scope: LeaderboardScope? = nil, period: LeaderboardPeriod? = nil) async {
        if let scope { self.scope = scope }
        if let period { self.period = period }
        await load()
    }

    public func load() async {
        let repository = repository
        let scope = scope
        let period = period
        if level == nil { level = try? await repository.profile().value.level }
        state = await reload(state) { try await repository.leaderboard(scope: scope, period: period) }
    }
}

@MainActor
@Observable
public final class NotificationsViewModel {
    public private(set) var state: ScreenState<NotificationList> = .loading
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func load() async {
        let repository = repository
        state = await reload(state) { try await repository.notifications() }
    }

    /// Касание уведомления: отмечаем прочитанным и фиксируем открытие для аналитики.
    public func open(_ notification: AppNotification) async {
        await repository.recordEvent("notification_opened", runId: nil, notificationId: notification.id)
        guard !notification.read else { return }
        try? await repository.markRead(notification.id)
        await load()
    }

    public func markAllRead() async {
        try? await repository.markAllRead()
        await load()
    }
}

/// Аналитика своя или проводника для инструктора (`/team/:id` сайта).
@MainActor
@Observable
public final class AnalyticsViewModel {
    public private(set) var state: ScreenState<Analytics> = .loading
    /// История прохождений — только по себе: профиль другого сотрудника инструктору не нужен.
    public private(set) var history: [HistoryItem] = []
    public let employeeId: Int?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository, employeeId: Int? = nil) {
        self.repository = repository
        self.employeeId = employeeId
    }

    public var own: Bool { employeeId == nil }

    public func load() async {
        let repository = repository
        let employeeId = employeeId
        state = await reload(state) {
            if let employeeId { return try await repository.employeeAnalytics(employeeId) }
            return try await repository.analytics()
        }
        if own, let profile = try? await repository.profile() { history = profile.value.history }
    }
}

/// Справочник и в каких сценариях отрабатывается каждая ситуация.
@MainActor
@Observable
public final class HandbookViewModel {
    public struct Content {
        public let handbook: Handbook
        public let trainedIn: [Int: [ScenarioSummary]]
    }

    public private(set) var state: ScreenState<Content> = .loading
    public var query = ""
    public var category: String?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    /// Ситуации по строке поиска и категории — как фильтр `HandbookPage`.
    public var situations: [Situation] {
        guard let handbook = state.value?.handbook else { return [] }
        let needle = query.trimmingCharacters(in: .whitespaces).lowercased()
        return handbook.situations.filter { situation in
            (category == nil || situation.category == category)
                && (needle.isEmpty || ([situation.title, situation.reaction] + situation.phrases).joined(separator: " ").lowercased().contains(needle))
        }
    }

    /// Фильтры справочника сайта — все четыре типа ситуаций.
    public let categories = ["conflict", "medical", "service", "safety"]

    public func load() async {
        let repository = repository
        state = await reload(state) {
            let handbook = try await repository.handbook()
            let scenarios = (try? await repository.scenarios().value) ?? []
            var trainedIn: [Int: [ScenarioSummary]] = [:]
            for scenario in scenarios {
                for number in scenario.situations { trainedIn[number, default: []].append(scenario) }
            }
            return Loaded(value: Content(handbook: handbook.value, trainedIn: trainedIn), staleSince: handbook.staleSince)
        }
    }
}

/// Для инструктора: проводники его депо.
@MainActor
@Observable
public final class TeamViewModel {
    public private(set) var state: ScreenState<[TeamMember]> = .loading
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func load() async {
        let repository = repository
        state = await reload(state) { try await repository.team() }
    }
}

/// Настройки: данные профиля только для чтения.
@MainActor
@Observable
public final class SettingsViewModel {
    public private(set) var profile: Profile?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func load() async {
        profile = try? await repository.profile().value
    }
}

/// Аватар вошедшего сотрудника — один на все экраны: загружается с профилем, выбор в настройках виден сразу.
@MainActor
@Observable
public final class MyAvatarStore {
    public private(set) var current = Avatar()
    /// Итог последнего сохранения: «Сохранено» или текст ошибки сервера.
    public private(set) var status: String?
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    /// Без профиля остаётся аватар по умолчанию.
    public func refresh() async {
        if let profile = try? await repository.profile() { current = profile.value.avatar }
    }

    /// Выбор виден сразу; сервер отказал или нет сети — возвращаем прежний.
    public func choose(_ next: Avatar) async {
        let previous = current
        current = next
        status = nil
        do {
            current = try await repository.updateAvatar(next)
            status = "Сохранено"
        } catch {
            current = previous
            status = error.userMessage
        }
    }
}

/// Шапка: счётчик непрочитанных уведомлений — опрос раз в 30 с, как на сайте.
@MainActor
@Observable
public final class ShellViewModel {
    public private(set) var unread = 0
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    /// Ошибку опроса не показываем: счётчик обновится при следующей попытке.
    public func refreshUnread() async {
        if let list = try? await repository.notifications() { unread = list.value.unread }
    }

    public func poll() async {
        while !Task.isCancelled {
            await refreshUnread()
            try? await Task.sleep(for: .seconds(30))
        }
    }
}
