import SwiftUI
import VSMCore

/// Главная: уровень и XP, незавершённый сценарий, рекомендация, место в бригаде, последние ачивки.
struct HomeView: View {
    let container: AppContainer
    @State private var model: HomeViewModel
    @State private var playing: ScenarioSummary?

    init(container: AppContainer) {
        self.container = container
        _model = State(initialValue: HomeViewModel(repository: container.trainer, runs: container.runs, activeRuns: container.activeRuns))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { content, staleSince in
            List {
                if let staleSince { StaleBanner(since: staleSince).listRowSeparator(.hidden) }
                Section {
                    LevelCard(profile: content.profile)
                }
                if let active = content.activeRun, let scenario = content.scenarios.first(where: { $0.id == active.scenarioId }) {
                    Section("Незавершённый сценарий") {
                        Button {
                            playing = scenario
                        } label: {
                            Label("Продолжить «\(active.title)»", systemImage: "play.circle.fill")
                        }
                    }
                }
                if let recommendation = content.recommendation,
                   let scenario = content.scenarios.first(where: { $0.id == recommendation.scenarioId }) {
                    Section("Рекомендация") {
                        NavigationLink(value: scenario) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(recommendation.title).font(.headline)
                                Text(recommendation.reason).font(.subheadline).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                Section("Рейтинг за неделю") {
                    if let rank = content.myRank {
                        LabeledContent(content.rankTitle ?? "Бригада", value: "\(rank.rank) место · \(rank.points) XP")
                    } else {
                        Text("Пройдите сценарий, чтобы попасть в рейтинг").foregroundStyle(.secondary)
                    }
                }
                let recent = content.profile.earnedAchievements.prefix(3)
                if !recent.isEmpty {
                    Section("Последние достижения") {
                        ForEach(Array(recent), id: \.code) { achievement in
                            Label(achievement.title, systemImage: "rosette")
                        }
                    }
                }
                Section("Сценарии") {
                    ForEach(content.scenarios.prefix(3)) { scenario in
                        NavigationLink(value: scenario) { ScenarioRow(scenario: scenario) }
                    }
                }
            }
            .refreshable { await model.load() }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    NavigationLink {
                        NotificationsView(repository: container.trainer)
                    } label: {
                        Image(systemName: content.unread > 0 ? "bell.badge" : "bell")
                            .accessibilityLabel("Уведомления: \(content.unread) непрочитанных")
                    }
                }
            }
        }
        .largeTitle("Главная")
        .navigationDestination(for: ScenarioSummary.self) { ScenarioDetailView(container: container, scenario: $0) }
        .task { await model.load() }
        .scenarioPlayer(item: $playing, container: container) { Task { await model.load() } }
    }
}

struct LevelCard: View {
    let profile: Profile

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(profile.fullName).font(.headline)
            Text("\(profile.brigade) · \(profile.depot)").font(.subheadline).foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline) {
                Text("Уровень \(profile.level.level)").font(.title3.bold())
                Text(profile.level.title).foregroundStyle(.secondary)
                Spacer()
                Text("\(profile.level.xp) XP").font(.subheadline.monospacedDigit())
            }
            ProgressView(value: profile.level.progress)
            if let next = profile.level.nextLevelXp {
                Text("До следующего уровня \(max(0, next - profile.level.xp)) XP").font(.caption).foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
