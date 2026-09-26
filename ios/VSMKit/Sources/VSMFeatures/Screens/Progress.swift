import SwiftUI
import VSMCore

// MARK: - Рейтинг

/// Рейтинг — `RatingPage`: «Ваше место», вкладки уровня и периода, строки с медалями за 1–3 места.
struct RatingScreen: View {
    @State private var model: LeaderboardViewModel
    @Environment(\.vsm) private var colors

    init(container: AppContainer) {
        _model = State(initialValue: LeaderboardViewModel(repository: container.trainer))
    }

    var body: some View {
        Page {
            PageTitle("Рейтинг")
            if let board = model.state.value, let me = board.me {
                Card(spacing: 16, background: AnyShapeStyle(LinearGradient(colors: [colors.surface, colors.soft], startPoint: .topLeading, endPoint: .bottomTrailing))) {
                    HStack(spacing: 20) {
                        UserAvatar(size: 88, ring: 4)
                        VStack(alignment: .leading, spacing: 0) {
                            Muted("Ваше место · \(board.title)")
                            (Text("\(me.rank)").foregroundColor(colors.brand)
                                + Text(" из \(board.participants)").font(.custom(Manrope.name(.semibold), size: 17.6)).foregroundColor(colors.muted))
                                .textStyle(VsmType.pageTitle.sized(41.6))
                            Text(me.fullName).textStyle(VsmType.body).foregroundStyle(colors.text)
                        }
                    }
                    .accessibilityElement(children: .combine)
                    VStack(alignment: .leading, spacing: 0) {
                        Muted("Баллы за период")
                        AnimatedNumber(value: me.points, style: VsmType.kpiValue, color: colors.heading, suffix: " XP")
                    }
                    if let level = model.level { LevelProgress(level: level) }
                }
            }
            VStack(alignment: .leading, spacing: 16) {
                Tabs(options: LeaderboardScope.allCases.map { ($0, $0.title) }, selected: model.scope) { scope in
                    Task { await model.select(scope: scope) }
                }
                Tabs(options: LeaderboardPeriod.allCases.map { ($0, $0.title) }, selected: model.period) { period in
                    Task { await model.select(period: period) }
                }
            }
            if let board = model.state.value {
                (Text(board.title) + Text(" · участников: \(board.participants)").foregroundColor(colors.muted))
                    .textStyle(VsmType.h2)
                    .foregroundStyle(colors.heading)
                ForEach(Array(board.rows.enumerated()), id: \.element.id) { index, row in
                    RatingRow(row: row, index: index)
                }
                .id("\(board.scope)-\(board.period)")
            } else {
                ScreenContent(state: model.state, retry: model.load) { _ in EmptyView() }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}

private struct RatingRow: View {
    let row: LeaderboardRow
    let index: Int
    @Environment(\.vsm) private var colors

    var body: some View {
        let medal = (1...3).contains(row.rank) ? Palette.rankTop[row.rank - 1] : nil
        let shape = RoundedRectangle(cornerRadius: Radius.image, style: .continuous)
        HStack(spacing: 16) {
            Text("\(row.rank)")
                .textStyle(VsmType.bodyBold.weighted(.heavy))
                .foregroundStyle(medal?.1 ?? colors.text)
                .frame(width: 36, height: 36)
                .background(medal?.0 ?? colors.tagBg, in: Circle())
            if row.isMe { UserAvatar(size: 44, ring: 0) } else { InitialsAvatar(fullName: row.fullName, size: 44) }
            VStack(alignment: .leading, spacing: 0) {
                Text(row.fullName + (row.isMe ? " (вы)" : "")).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                Text("\(row.brigade) · \(row.depot) · \(row.levelTitle)").textStyle(VsmType.small).foregroundStyle(colors.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text("\(row.points) XP").textStyle(VsmType.bodyBold.sized(17.6)).foregroundStyle(colors.heading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(row.isMe ? colors.soft : .clear, in: shape)
        .overlay(shape.strokeBorder(row.isMe ? colors.brandBorder : .clear, lineWidth: 1.5))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(row.rank) место, \(row.fullName)\(row.isMe ? ", вы" : ""), \(row.points) XP")
        .riseIn(index, step: 0.055, duration: 0.4)
    }
}

// MARK: - KPI

struct KpiItem {
    let label: String
    var number: Int?
    var suffix = ""
    var text: String?
    var caption: String?
}

/// KPI-плитки `.kpis`: на телефоне две колонки, число досчитывает при появлении.
struct KpiGrid: View {
    let items: [KpiItem]
    @Environment(\.vsm) private var colors

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)], alignment: .leading, spacing: 16) {
            ForEach(Array(items.enumerated()), id: \.offset) { index, kpi in
                Card(padding: 16, spacing: 4) {
                    Text(kpi.label).textStyle(VsmType.kpiLabel).foregroundStyle(colors.muted)
                    if let number = kpi.number {
                        AnimatedNumber(value: number, style: VsmType.kpiValue, color: colors.heading, suffix: kpi.suffix)
                    } else {
                        Text(kpi.text ?? "").textStyle(VsmType.kpiValue).foregroundStyle(colors.heading)
                    }
                    if let caption = kpi.caption { Muted(caption) }
                    Spacer(minLength: 0)
                }
                .frame(maxHeight: .infinity, alignment: .top)
                .accessibilityElement(children: .combine)
                .riseIn(index, step: 0.06, duration: 0.45)
            }
        }
    }
}

// MARK: - Аналитика

private func percent(_ value: Double?) -> String {
    value.map { "\(Int(($0 * 100).rounded()))%" } ?? "—"
}

/// Взвешенная по числу прохождений / решений доля по всем категориям — как `overall` сайта.
private func overall(_ data: Analytics, _ pick: (CategoryStats) -> (Double?, Int)) -> Double? {
    var sum = 0.0
    var weight = 0
    for category in data.categories {
        let (rate, count) = pick(category)
        if let rate {
            sum += rate * Double(count)
            weight += count
        }
    }
    return weight > 0 ? sum / Double(weight) : nil
}

private let weekFormat: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "dd.MM"
    return formatter
}()

/// Аналитика — `AnalyticsPage`: KPI, рекомендация, выводы, XP по неделям, навыки, типы ситуаций, история.
struct AnalyticsScreen: View {
    @State private var model: AnalyticsViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer, employeeId: Int?) {
        _model = State(initialValue: AnalyticsViewModel(repository: container.trainer, employeeId: employeeId))
    }

    var body: some View {
        Page {
            ScreenContent(state: model.state, retry: model.load) { data in
                PageTitle(model.own ? "Аналитика" : "Аналитика: \(data.fullName)")
                KpiGrid(items: [
                    KpiItem(label: "Пройдено сценариев", number: data.totalRuns),
                    KpiItem(label: "Средний результат", text: percent(overall(data) { ($0.successRate, $0.runs) }), caption: "успешных прохождений"),
                    KpiItem(label: "Набрано баллов", number: data.progress.map(\.xp).reduce(0, +), suffix: " XP", caption: "за 8 недель"),
                    KpiItem(label: "Лучшие решения", text: percent(overall(data) { ($0.bestChoiceRate, $0.decisions) }), caption: "от всех решений"),
                ])
                if let recommendation = data.recommendation {
                    Card(background: AnyShapeStyle(LinearGradient(colors: [colors.soft, colors.surface], startPoint: .topLeading, endPoint: .bottomTrailing)), accentLeading: colors.brand) {
                        Text("РЕКОМЕНДАЦИЯ").textStyle(VsmType.heroEyebrow).foregroundStyle(colors.heroEyebrow)
                        CardTitle(recommendation.title)
                        Muted(recommendation.reason)
                        if model.own {
                            PrimaryButton(title: "Пройти", arrow: true) { navigator.play(recommendation.scenarioId) }
                        }
                    }
                }
                if data.totalRuns == 0 {
                    Muted("Пройдите первый сценарий — здесь появятся выводы о сильных и слабых сторонах.")
                } else {
                    Card {
                        CardTitle("Выводы")
                        if !data.strengths.isEmpty {
                            Text("💪 Сильные стороны: \(data.strengths.joined(separator: ", "))").textStyle(VsmType.body).foregroundStyle(colors.text)
                        }
                        if !data.weaknesses.isEmpty {
                            Text("🎯 Стоит подтянуть: \(data.weaknesses.joined(separator: ", "))").textStyle(VsmType.body).foregroundStyle(colors.text)
                        }
                        if data.mistakes.isEmpty { Muted("Типичных ошибок не найдено.") }
                        ForEach(data.mistakes, id: \.self) { Bullet(text: $0) }
                    }
                    Card {
                        CardTitle("Прогресс: XP по неделям")
                        XpBarChart(labels: data.progress.map { weekFormat.string(from: $0.weekStart) }, values: data.progress.map(\.xp))
                    }
                    Card(spacing: 16) {
                        CardTitle("Навыки")
                        let maximum = max(data.competences.map(\.points).max() ?? 0, 1)
                        ForEach(data.competences) { competence in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(competence.title).textStyle(VsmType.body.sized(15)).foregroundStyle(colors.text)
                                    Spacer()
                                    Text("\(competence.points)").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                                }
                                GradientProgress(fraction: Double(competence.points) / Double(maximum))
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }
                    // Таблица сайта на 6 колонок на телефоне не читается — те же данные строками «подпись — значение»
                    Card {
                        CardTitle("По типам ситуаций")
                        ForEach(Array(data.categories.enumerated()), id: \.element.id) { index, category in
                            if index > 0 { Rectangle().fill(colors.border).frame(height: 1) }
                            VStack(alignment: .leading, spacing: 4) {
                                Text(category.title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                                cell("Прохождений", "\(category.runs)")
                                cell("Успех", percent(category.successRate))
                                cell("Лучшие решения", percent(category.bestChoiceRate))
                                cell("Истёк таймер", percent(category.timeoutRate))
                                cell("Время решения", category.averageReactionShare.map { "\(percent($0)) таймера" } ?? "—")
                            }
                            .padding(.vertical, 8)
                            .accessibilityElement(children: .combine)
                        }
                    }
                    if model.own, !model.history.isEmpty {
                        Card {
                            CardTitle("История прохождения")
                            HistoryList(items: model.history)
                        }
                    }
                }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }

    private func cell(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).textStyle(VsmType.small).foregroundStyle(colors.muted)
            Spacer()
            Text(value).textStyle(VsmType.smallStrong).foregroundStyle(colors.text)
        }
    }
}

// MARK: - Профиль

/// Профиль — `ProfilePage`: шапка с аватаром и уровнем, 4 KPI, радар компетенций, ачивки, история.
struct ProfileScreen: View {
    @State private var model: ProfileViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer) {
        _model = State(initialValue: ProfileViewModel(repository: container.trainer))
    }

    var body: some View {
        Page {
            ScreenContent(state: model.state, retry: model.load) { profile in
                Card(spacing: 16, background: AnyShapeStyle(LinearGradient(colors: [colors.surface, colors.soft], startPoint: .topLeading, endPoint: .bottomTrailing))) {
                    UserAvatar(size: 112, ring: 4)
                    LinkAction(title: "Изменить аватар") { navigator.open(.settings) }
                    VStack(alignment: .leading, spacing: 4) {
                        PageTitle(profile.fullName)
                        Muted("\(Labels.role(profile.role)) · \(profile.brigade) · \(profile.depot)")
                        Muted("Табельный № \(profile.personnelNumber)")
                    }
                    LevelProgress(level: profile.level)
                }
                let earned = profile.achievements.filter { $0.earnedAt != nil }.count
                KpiGrid(items: [
                    KpiItem(label: "Общий балл", number: profile.level.xp, suffix: " XP"),
                    KpiItem(label: "Пройдено сценариев", number: profile.runsCompleted),
                    KpiItem(label: "Достижения", text: "\(earned) из \(profile.achievements.count)"),
                    KpiItem(label: "Уровень", text: "\(profile.level.level) · \(profile.level.title)"),
                ])
                Card {
                    CardTitle("Компетенции")
                    CompetenceRadar(items: profile.competencePoints.sorted { $0.key < $1.key }.map { (model.title(ofCompetence: $0.key), $0.value) })
                }
                Card {
                    CardTitle("Достижения")
                    ForEach(Array(profile.achievements.enumerated()), id: \.element.code) { index, achievement in
                        let earnedAt = achievement.earnedAt
                        OutlinedBlock(border: earnedAt != nil ? colors.successBorder : colors.border, background: earnedAt != nil ? colors.surface : colors.surfaceMuted, index: index) {
                            Text("\(earnedAt != nil ? "🏅" : "🔒") \(achievement.title)")
                                .textStyle(VsmType.bodyBold)
                                .foregroundStyle(earnedAt != nil ? colors.text : colors.muted)
                            Muted(achievement.description)
                            if let earnedAt { Muted(SiteDate.day(earnedAt)) }
                        }
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("\(achievement.title), \(earnedAt != nil ? "получено" : "не получено"). \(achievement.description)")
                    }
                }
                Card {
                    CardTitle("История прохождений")
                    if profile.history.isEmpty {
                        Muted("Пока нет завершённых сценариев.")
                        Button { navigator.openTab(.scenarios) } label: {
                            Text("Выбрать сценарий").textStyle(VsmType.body).underline().foregroundStyle(colors.brandText)
                        }
                        .buttonStyle(.plain)
                    } else {
                        HistoryList(items: profile.history, showCategory: true)
                    }
                }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}
