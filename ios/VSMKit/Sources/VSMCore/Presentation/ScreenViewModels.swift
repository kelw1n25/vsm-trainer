import Foundation
import Observation

/// Главная: кто я, уровень и XP, что пройти дальше, место в бригаде, непрочитанные уведомления.
@MainActor
@Observable
public final class HomeViewModel {
    public struct Content {
        public let profile: Profile
        public let recommendation: Recommendation?
        public let scenarios: [ScenarioSummary]
        public let myRank: LeaderboardRow?
        public let rankTitle: String?
        public let unread: Int
        public let activeRun: ActiveRun?
    }

    public private(set) var state: ScreenState<Content> = .loading
    private let repository: TrainerRepository
    private let activeRuns: ActiveRunStore
    private let runs: RunRepository

    public init(repository: TrainerRepository, runs: RunRepository, activeRuns: ActiveRunStore) {
        self.repository = repository
        self.runs = runs
        self.activeRuns = activeRuns
    }

    public func load() async {
        if state.value == nil { state = .loading }
        do {
            async let profile = repository.profile()
            async let scenarios = repository.scenarios()
            async let analytics = try? repository.analytics()
            async let board = try? repository.leaderboard(scope: .brigade, period: .week)
            async let notifications = try? repository.notifications()
            let (loadedProfile, loadedScenarios) = try await (profile, scenarios)
            let content = Content(
                profile: loadedProfile.value,
                recommendation: await analytics?.value.recommendation,
                scenarios: loadedScenarios.value,
                myRank: await board?.value.me,
                rankTitle: await board?.value.title,
                unread: await notifications?.value.unread ?? 0,
                activeRun: await validActiveRun()
            )
            state = .loaded(content, staleSince: loadedProfile.staleSince ?? loadedScenarios.staleSince)
        } catch {
            state = .failed(error.userMessage)
        }
    }

    /// Незавершённое прохождение могли закончить на другом устройстве или по таймеру — сверяемся с сервером.
    private func validActiveRun() async -> ActiveRun? {
        guard let active = activeRuns.current else { return nil }
        do {
            let state = try await runs.state(runId: active.runId)
            if state.status == .inProgress { return active }
            activeRuns.clear()
            return nil
        } catch APIError.offline {
            return active
        } catch {
            activeRuns.clear()
            return nil
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

    public var categories: [String] {
        Array(Set((state.value ?? []).map(\.category))).sorted()
    }

    public func load() async {
        do {
            let loaded = try await repository.scenarios()
            state = .loaded(loaded.value, staleSince: loaded.staleSince)
        } catch {
            if state.value == nil { state = .failed(error.userMessage) }
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

    /// Компетенции по убыванию очков — сильные сверху, проседающие снизу.
    public var competences: [(title: String, points: Int)] {
        guard let profile = state.value else { return [] }
        let codes = Set(profile.competencePoints.keys).union(competenceTitles.keys)
        return codes.map { (competenceTitles[$0] ?? $0, profile.competencePoints[$0] ?? 0) }.sorted { $0.points > $1.points }
    }

    public func load() async {
        do {
            async let meta = try? repository.meta()
            let loaded = try await repository.profile()
            competenceTitles = await meta?.value.competences ?? competenceTitles
            state = .loaded(loaded.value, staleSince: loaded.staleSince)
        } catch {
            if state.value == nil { state = .failed(error.userMessage) }
        }
    }
}

@MainActor
@Observable
public final class LeaderboardViewModel {
    public private(set) var state: ScreenState<Leaderboard> = .loading
    public var scope: LeaderboardScope = .brigade
    public var period: LeaderboardPeriod = .week
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func load() async {
        state = .loading
        do {
            let loaded = try await repository.leaderboard(scope: scope, period: period)
            state = .loaded(loaded.value, staleSince: loaded.staleSince)
        } catch {
            state = .failed(error.userMessage)
        }
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
        do {
            let loaded = try await repository.notifications()
            state = .loaded(loaded.value, staleSince: loaded.staleSince)
        } catch {
            if state.value == nil { state = .failed(error.userMessage) }
        }
    }

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

@MainActor
@Observable
public final class AnalyticsViewModel {
    public private(set) var state: ScreenState<Analytics> = .loading
    private let repository: TrainerRepository

    public init(repository: TrainerRepository) {
        self.repository = repository
    }

    public func load() async {
        do {
            let loaded = try await repository.analytics()
            state = .loaded(loaded.value, staleSince: loaded.staleSince)
        } catch {
            if state.value == nil { state = .failed(error.userMessage) }
        }
    }
}
