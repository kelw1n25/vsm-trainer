import Charts
import SwiftUI
import VSMCore

/// Профиль: уровень, компетенции (сильные сверху), достижения, история прохождений.
struct ProfileView: View {
    let container: AppContainer
    @State private var model: ProfileViewModel
    @State private var confirmSignOut = false

    init(container: AppContainer) {
        self.container = container
        _model = State(initialValue: ProfileViewModel(repository: container.trainer))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { profile, staleSince in
            List {
                if let staleSince { StaleBanner(since: staleSince).listRowSeparator(.hidden) }
                Section { LevelCard(profile: profile) }
                Section {
                    NavigationLink {
                        AnalyticsView(repository: container.trainer)
                    } label: {
                        Label("Аналитика компетенций", systemImage: "chart.bar.xaxis")
                    }
                }
                Section("Компетенции") {
                    ForEach(model.competences, id: \.title) { competence in
                        VStack(alignment: .leading, spacing: 4) {
                            LabeledContent(competence.title, value: "\(competence.points)")
                            ProgressView(value: Double(min(competence.points, 100)), total: 100)
                        }
                    }
                }
                Section("Достижения") {
                    ForEach(profile.achievements, id: \.code) { achievement in
                        HStack {
                            Image(systemName: achievement.earnedAt == nil ? "lock" : "rosette")
                                .foregroundStyle(achievement.earnedAt == nil ? Color.secondary : Color.orange)
                            VStack(alignment: .leading) {
                                Text(achievement.title)
                                Text(achievement.description).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                        .opacity(achievement.earnedAt == nil ? 0.6 : 1)
                    }
                }
                Section("Пройдено: \(profile.runsCompleted)") {
                    ForEach(profile.history) { item in
                        NavigationLink {
                            DebriefView(container: container, runId: item.runId)
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.scenarioTitle)
                                Text("\(Labels.outcome(item.outcome)) · +\(item.xpEarned) XP · \(item.finishedAt.formatted(date: .abbreviated, time: .omitted))")
                                    .font(.caption).foregroundStyle(Palette.outcome(item.outcome))
                            }
                        }
                    }
                }
                Section {
                    Button("Выйти из аккаунта", role: .destructive) { confirmSignOut = true }
                }
            }
            .refreshable { await model.load() }
        }
        .largeTitle("Профиль")
        .task { await model.load() }
        .confirmationDialog("Выйти из аккаунта?", isPresented: $confirmSignOut, titleVisibility: .visible) {
            Button("Выйти", role: .destructive) { Task { await container.session.signOut() } }
        }
    }
}

struct LeaderboardView: View {
    @State private var model: LeaderboardViewModel

    init(repository: TrainerRepository) {
        _model = State(initialValue: LeaderboardViewModel(repository: repository))
    }

    var body: some View {
        List {
            Section {
                Picker("Уровень", selection: $model.scope) {
                    ForEach(LeaderboardScope.allCases, id: \.self) { Text($0.title) }
                }
                .pickerStyle(.segmented)
                Picker("Период", selection: $model.period) {
                    ForEach(LeaderboardPeriod.allCases, id: \.self) { Text($0.title) }
                }
                .pickerStyle(.segmented)
            }
            switch model.state {
            case .loading:
                ProgressView().frame(maxWidth: .infinity)
            case let .failed(message):
                Label(message, systemImage: "exclamationmark.triangle").foregroundStyle(.secondary)
            case let .loaded(board, staleSince):
                if let staleSince { StaleBanner(since: staleSince) }
                Section("\(board.title) · участников: \(board.participants)") {
                    ForEach(board.rows) { row in
                        HStack {
                            Text("\(row.rank)").font(.headline.monospacedDigit()).frame(width: 32)
                            VStack(alignment: .leading) {
                                Text(row.fullName).fontWeight(row.isMe ? .bold : .regular)
                                Text("\(row.levelTitle) · \(row.brigade)").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text("\(row.points) XP").font(.subheadline.monospacedDigit())
                        }
                        .listRowBackground(row.isMe ? Palette.brand.opacity(0.1) : nil)
                    }
                }
            }
        }
        .largeTitle("Рейтинг")
        .refreshable { await model.load() }
        .task(id: "\(model.scope.rawValue)-\(model.period.rawValue)") { await model.load() }
    }
}

struct NotificationsView: View {
    @State private var model: NotificationsViewModel

    init(repository: TrainerRepository) {
        _model = State(initialValue: NotificationsViewModel(repository: repository))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { list, staleSince in
            List {
                if let staleSince { StaleBanner(since: staleSince).listRowSeparator(.hidden) }
                if list.items.isEmpty {
                    ContentUnavailableView("Уведомлений нет", systemImage: "bell.slash")
                }
                ForEach(list.items) { item in
                    Button {
                        Task { await model.open(item) }
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                if !item.read { Circle().fill(Palette.brand).frame(width: 8, height: 8) }
                                Text(item.title).font(.headline)
                            }
                            Text(item.body).font(.subheadline).foregroundStyle(.secondary)
                            Text(item.createdAt.formatted(.relative(presentation: .named))).font(.caption).foregroundStyle(.tertiary)
                        }
                    }
                    .tint(.primary)
                }
            }
            .refreshable { await model.load() }
            .toolbar {
                if list.unread > 0 {
                    Button("Прочитать все") { Task { await model.markAllRead() } }
                }
            }
        }
        .inlineTitle("Уведомления")
        .task { await model.load() }
    }
}

/// Аналитика: выводы о сильных и проседающих компетенциях, типичные ошибки, прогресс по неделям.
struct AnalyticsView: View {
    @State private var model: AnalyticsViewModel

    init(repository: TrainerRepository) {
        _model = State(initialValue: AnalyticsViewModel(repository: repository))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { data, staleSince in
            List {
                if let staleSince { StaleBanner(since: staleSince).listRowSeparator(.hidden) }
                Section {
                    LabeledContent("Пройдено сценариев", value: "\(data.totalRuns)")
                    if let rate = data.completionRate {
                        LabeledContent("Доведено до финала", value: rate.formatted(.percent.precision(.fractionLength(0))))
                    }
                    if let seconds = data.avgDecisionSeconds {
                        LabeledContent("Среднее время решения", value: "\(seconds.formatted(.number.precision(.fractionLength(1)))) с")
                    }
                }
                if !data.strengths.isEmpty {
                    Section("Сильные стороны") { ForEach(data.strengths, id: \.self) { Label($0, systemImage: "hand.thumbsup") } }
                }
                if !data.weaknesses.isEmpty {
                    Section("Стоит подтянуть") { ForEach(data.weaknesses, id: \.self) { Label($0, systemImage: "target") } }
                }
                if !data.mistakes.isEmpty {
                    Section("Типичные ошибки") { ForEach(data.mistakes, id: \.self) { Text($0) } }
                }
                if let recommendation = data.recommendation {
                    Section("Рекомендация") {
                        Text(recommendation.title).font(.headline)
                        Text(recommendation.reason).font(.callout).foregroundStyle(.secondary)
                    }
                }
                Section("XP по неделям") {
                    Chart(data.progress, id: \.weekStart) { week in
                        BarMark(x: .value("Неделя", week.weekStart, unit: .weekOfYear), y: .value("XP", week.xp))
                            .foregroundStyle(Palette.brand)
                    }
                    .frame(height: 180)
                }
            }
            .refreshable { await model.load() }
        }
        .inlineTitle("Аналитика")
        .task { await model.load() }
    }
}
