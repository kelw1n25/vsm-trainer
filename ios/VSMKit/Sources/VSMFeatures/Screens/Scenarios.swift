import SwiftUI
import VSMCore

/// Иллюстрация сценария `ScenarioImage` в рамке с радиусом 14 и тенью, кадр заполняется как `slice` на сайте.
struct ScenarioIllustration: View {
    let scenario: ScenarioSummary

    var body: some View {
        Color.clear
            .overlay { AssetImage(name: Assets.scenarioImage(scenario.id, category: scenario.category), contentMode: .fill) }
            .clipShape(RoundedRectangle(cornerRadius: Radius.image, style: .continuous))
            .vsmShadow(radius: 8, y: 4)
            .accessibilityHidden(true)
    }
}

/// Карточка сценария `ScenarioCard` в ширине телефона: иллюстрация сверху, теги, название, маршрут, сложность,
/// «Начать →». Касание карточки открывает страницу сценария, кнопка — сразу прохождение. Появляется по очереди.
struct ScenarioCard: View {
    let scenario: ScenarioSummary
    let index: Int
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    var body: some View {
        Card(padding: 18, spacing: 0) {
            ScenarioIllustration(scenario: scenario).frame(height: 150)
            Flow {
                Tag(text: Labels.category(scenario.category), category: true)
                Tag(text: "Финалов: \(scenario.endingsTotal)")
            }
            .padding(.top, 16)
            Text(scenario.title).textStyle(VsmType.cardTitle).foregroundStyle(colors.text).padding(.top, 12).padding(.bottom, 14)
            RouteRow(route: scenario.route, serviceClass: scenario.serviceClass)
            DifficultyDots(level: scenario.difficulty).padding(.top, 16)
            PrimaryButton(title: "Начать", arrow: true) { navigator.play(scenario.id) }
                .frame(minWidth: 160)
                .padding(.top, 16)
                .accessibilityLabel("Начать сценарий «\(scenario.title)»")
        }
        .contentShape(Rectangle())
        .onTapGesture { navigator.open(.scenario(scenario.id)) }
        .accessibilityElement(children: .contain)
        .accessibilityAction(named: "Открыть сценарий") { navigator.open(.scenario(scenario.id)) }
        .riseIn(index)
    }
}

/// Раздел «Сценарии»: все сценарии с фильтром по типу ситуации.
struct ScenarioListScreen: View {
    @State private var model: ScenarioListViewModel

    init(container: AppContainer) {
        _model = State(initialValue: ScenarioListViewModel(repository: container.trainer))
    }

    var body: some View {
        Page {
            PageTitle("Сценарии")
            if model.state.value != nil {
                Flow(spacing: 10, lineSpacing: 10) {
                    Chip(title: "Все", selected: model.category == nil) { model.category = nil }
                    ForEach(model.categories, id: \.self) { code in
                        Chip(title: Labels.category(code), selected: model.category == code) { model.category = code }
                    }
                }
                SectionTitle(text: model.category.map(Labels.category) ?? "Все сценарии", count: model.visible.count)
                if model.visible.isEmpty { Muted("В этой категории пока нет сценариев.") }
                ForEach(Array(model.visible.enumerated()), id: \.element.id) { index, scenario in
                    ScenarioCard(scenario: scenario, index: index)
                }
            } else {
                ScreenContent(state: model.state, retry: model.load) { _ in EmptyView() }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}

private let outcomeRank: [RunStatus: Int] = [.success: 3, .partial: 2, .failure: 1, .inProgress: 0]

/// Страница сценария `ScenarioPage`: описание, три особенности, ситуации из справочника, свои попытки.
struct ScenarioDetailScreen: View {
    @State private var model: ScenarioDetailViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer, scenarioId: String) {
        _model = State(initialValue: ScenarioDetailViewModel(scenarioId: scenarioId, repository: container.trainer))
    }

    var body: some View {
        Page {
            BackLink(title: "Все сценарии") { navigator.openTab(.scenarios) }
            ScreenContent(state: model.state, retry: model.load) { detail in
                if let scenario = detail.scenario {
                    content(scenario, attempts: detail.attempts)
                } else {
                    Card {
                        PageTitle("Сценарий не найден")
                        PrimaryButton(title: "К списку сценариев") { navigator.openTab(.scenarios) }
                    }
                }
            }
        }
        .task { await model.load() }
    }

    @ViewBuilder
    private func content(_ scenario: ScenarioSummary, attempts: [HistoryItem]) -> some View {
        let best = attempts.max { (outcomeRank[$0.outcome] ?? 0) < (outcomeRank[$1.outcome] ?? 0) }
        Card(spacing: 14) {
            Flow {
                Tag(text: Labels.category(scenario.category), category: true)
                Tag(text: "Финалов: \(scenario.endingsTotal)")
            }
            PageTitle(scenario.title)
            RouteRow(route: scenario.route, serviceClass: scenario.serviceClass)
            DifficultyDots(level: scenario.difficulty)
            Text(scenario.description).textStyle(VsmType.bodyLarge).foregroundStyle(colors.text)
            HStack(alignment: .top, spacing: 28) {
                Kpi(label: "Попыток", value: "\(attempts.count)")
                Kpi(label: "Лучший результат", value: best.map { Labels.outcome($0.outcome) } ?? "—")
            }
            .padding(.vertical, 6)
            PrimaryButton(title: "Начать сценарий", large: true, arrow: true) { navigator.play(scenario.id) }
            if !attempts.isEmpty {
                GhostButton(title: "Развитие истории", large: true) { navigator.open(.map(scenario.id)) }
            }
            ScenarioIllustration(scenario: scenario).aspectRatio(160 / 150, contentMode: .fit).padding(.top, 12)
        }
        let endings = Labels.plural(scenario.endingsTotal, "финал", "финала", "финалов")
        Feature(title: "🎬 Интерактивная история", text: "Сцены, диалоги и реакции персонажей. Выбор меняет сюжет — у сценария \(scenario.endingsTotal) \(endings).", index: 0)
        Feature(title: "⏱ Решения под таймером", text: "Таймер стартует, когда появились варианты. Не успели — ситуация развивается без вас.", index: 1)
        Feature(title: "⚖️ Две шкалы", text: "Лояльность пассажира и рейтинг безопасности. Падение любой до нуля — провал.", index: 2)
        Card {
            CardTitle("Какие ситуации отрабатываются")
            Muted("По материалам «Ситуации на борту» — после финала разбор покажет, как действовать по стандарту.")
            Flow(spacing: 10, lineSpacing: 10) {
                ForEach(scenario.situations, id: \.self) { number in
                    Button { navigator.situation(number) } label: {
                        Text("Ситуация \(number)")
                            .textStyle(VsmType.bodyStrong)
                            .underline()
                            .foregroundStyle(colors.text)
                            .padding(.horizontal, 18)
                            .frame(height: 38)
                            .background(colors.surface, in: Capsule())
                            .overlay(Capsule().strokeBorder(colors.controlBorder, lineWidth: 1))
                            .frame(minHeight: Metrics.touchMin)
                    }
                    .buttonStyle(PressStyle())
                }
            }
            .padding(.top, 8)
        }
        if !attempts.isEmpty {
            Card {
                CardTitle("Ваши попытки")
                HistoryList(items: attempts, showTitle: false)
            }
        }
    }
}

struct Kpi: View {
    let label: String
    let value: String
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).textStyle(VsmType.kpiLabel).foregroundStyle(colors.muted)
            Text(value).textStyle(VsmType.kpiValue).foregroundStyle(colors.heading)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct Feature: View {
    let title: String
    let text: String
    let index: Int
    @Environment(\.vsm) private var colors

    var body: some View {
        Card(spacing: 6) {
            Text(title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
            Muted(text)
        }
        .riseIn(index, step: 0.06, duration: 0.45)
    }
}

/// Список `.history`: дата · сценарий · бейдж исхода · XP · «Разбор».
struct HistoryList: View {
    let items: [HistoryItem]
    var showTitle = true
    var showCategory = false
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                Flow(spacing: 12, lineSpacing: 8) {
                    Text(SiteDate.dateTime(item.finishedAt)).textStyle(VsmType.body).foregroundStyle(colors.muted)
                    if showTitle {
                        Text(item.scenarioTitle + (showCategory ? " · \(Labels.category(item.category))" : ""))
                            .textStyle(VsmType.body)
                            .foregroundStyle(colors.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    OutcomeBadge(status: item.outcome)
                    Text("+\(item.xpEarned) XP").textStyle(VsmType.body).foregroundStyle(colors.text)
                    Button { navigator.open(.debrief(item.runId)) } label: {
                        Text("Разбор").textStyle(VsmType.body).underline().foregroundStyle(colors.brandText).frame(minHeight: 32)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Разбор: \(item.scenarioTitle)")
                }
                .padding(.vertical, 12)
                .riseIn(index, step: 0.06, duration: 0.45)
                if index < items.count - 1 { Rectangle().fill(colors.border).frame(height: 1) }
            }
        }
    }
}

// MARK: - Развитие истории

private let rootBranch = "root"

/// Развитие истории — `StoryMapPage`: какие развилки сотрудник уже исследовал и какие финалы открыл.
/// Закрытое сервер отдаёт без текста — подсмотреть его нельзя.
struct StoryMapScreen: View {
    @State private var model: StoryMapViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer, scenarioId: String) {
        _model = State(initialValue: StoryMapViewModel(scenarioId: scenarioId, repository: container.trainer))
    }

    var body: some View {
        Page {
            BackLink(title: "К сценарию") { navigator.back() }
            PageTitle("Развитие истории")
            ScreenContent(state: model.state, retry: model.load) { content in
                let map = content.map
                let reached = map.endings.filter(\.reached).count
                let tree = MapTree(map: map)
                Muted("«\(content.title)» — пройдено раз: \(map.playthroughs), исследовано решений: \(map.choicesExplored) из \(map.choicesTotal), открыто финалов: \(reached) из \(map.endings.count). Неисследованные ветки остаются закрытыми.")
                Card(spacing: 8) {
                    Text("НАЧАЛО").textStyle(VsmType.eyebrow).foregroundStyle(colors.brandText)
                    Branch(nodeId: map.startNode, via: rootBranch, tree: tree)
                }
                Card {
                    CardTitle("Финалы")
                    ForEach(map.endings) { EndingTile(ending: $0) }
                    PrimaryButton(title: "Пройти заново") { navigator.play(model.scenarioId) }.padding(.top, 6)
                }
            }
        }
        .task { await model.load() }
    }
}

/// Узел разворачивается там, где встретился впервые при обходе в глубину; в других местах — «сходится с веткой».
private struct MapTree {
    let nodes: [String: MapNode]
    let endings: [String: MapEnding]
    let owners: [String: String]

    init(map: StoryMap) {
        let nodes = Dictionary(map.nodes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        var owners: [String: String] = [:]
        func visit(_ nodeId: String, via: String) {
            guard owners[nodeId] == nil, let node = nodes[nodeId] else { return }
            owners[nodeId] = via
            for choice in node.choices {
                if let next = choice.next { visit(next, via: "\(nodeId):\(choice.id)") }
            }
            if let next = node.timeoutNext { visit(next, via: "\(nodeId):timeout") }
        }
        visit(map.startNode, via: rootBranch)
        self.nodes = nodes
        endings = Dictionary(map.endings.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        self.owners = owners
    }
}

private struct Branch: View {
    let nodeId: String
    let via: String
    let tree: MapTree
    @Environment(\.vsm) private var colors

    var body: some View {
        if let ending = tree.endings[nodeId] {
            (Text("Финал: ") + Text(ending.ending ?? "").bold())
                .textStyle(VsmType.body)
                .foregroundStyle(colors.text)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(colors.tagBg, in: RoundedRectangle(cornerRadius: Radius.image, style: .continuous))
                .padding(.vertical, 4)
        } else if let node = tree.nodes[nodeId] {
            if tree.owners[nodeId] != via {
                Text("→ сходится с веткой «\(node.situation)»").textStyle(VsmType.small).foregroundStyle(colors.muted).padding(.vertical, 4)
            } else {
                VStack(alignment: .leading, spacing: 0) {
                    Text(node.situation).textStyle(VsmType.bodyBold).foregroundStyle(colors.heading).padding(.bottom, 8)
                    // Линия ветвей слева, как `border-left` на сайте; вложенные ветки — через AnyView (рекурсивный тип)
                    VStack(alignment: .leading, spacing: 0) {
                        ForEach(node.choices) { choice in
                            BranchItem(explored: choice.explored) {
                                if choice.explored {
                                    Text("✓ \(choice.text ?? "")").textStyle(VsmType.body).foregroundStyle(colors.text)
                                    if let next = choice.next { AnyView(Branch(nodeId: next, via: "\(node.id):\(choice.id)", tree: tree)) }
                                } else {
                                    Text("🔒 Ветка не исследована").textStyle(VsmType.body).foregroundStyle(colors.muted)
                                }
                            }
                        }
                        if node.timer {
                            BranchItem(explored: node.timeoutExplored) {
                                if node.timeoutExplored {
                                    Text("⏱ Время на решение истекло").textStyle(VsmType.body).foregroundStyle(colors.text)
                                    if let next = node.timeoutNext { AnyView(Branch(nodeId: next, via: "\(node.id):timeout", tree: tree)) }
                                } else {
                                    Text("🔒 Ветка не исследована").textStyle(VsmType.body).foregroundStyle(colors.muted)
                                }
                            }
                        }
                    }
                    .overlay(alignment: .leading) { Rectangle().fill(colors.border).frame(width: 2) }
                }
                .padding(.top, 6)
            }
        }
    }
}

private struct BranchItem<Content: View>: View {
    let explored: Bool
    @ViewBuilder var content: Content
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            Rectangle().fill(explored ? colors.brand : colors.border).frame(width: 14, height: 2).padding(.top, 11)
            VStack(alignment: .leading, spacing: 6) { content }.padding(.leading, 12)
        }
        .padding(.vertical, 6)
    }
}

private struct EndingTile: View {
    let ending: MapEnding
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if ending.reached {
                Text(ending.ending ?? "").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                Text(Labels.outcome(ending.outcome)).textStyle(VsmType.caption).foregroundStyle(colors.muted)
            } else {
                Text("🔒 Финал не открыт").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                Text("Попробуйте другую линию поведения").textStyle(VsmType.caption).foregroundStyle(colors.muted)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(ending.reached ? colors.soft : colors.tagBg, in: RoundedRectangle(cornerRadius: Radius.block, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}
