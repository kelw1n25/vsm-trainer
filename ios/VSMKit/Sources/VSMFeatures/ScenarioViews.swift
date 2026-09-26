import SwiftUI
import VSMCore

struct ScenarioRow: View {
    let scenario: ScenarioSummary

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: Palette.category(scenario.category))
                .frame(width: 36, height: 36)
                .background(Palette.brand.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(Palette.brand)
            VStack(alignment: .leading, spacing: 2) {
                Text(scenario.title).font(.body)
                Text("\(Labels.category(scenario.category)) · \(scenario.serviceClass) · \(String(repeating: "●", count: scenario.difficulty))")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

struct ScenarioListView: View {
    let container: AppContainer
    @State private var model: ScenarioListViewModel

    init(container: AppContainer) {
        self.container = container
        _model = State(initialValue: ScenarioListViewModel(repository: container.trainer))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { _, staleSince in
            List {
                if let staleSince { StaleBanner(since: staleSince).listRowSeparator(.hidden) }
                Picker("Тип ситуации", selection: $model.category) {
                    Text("Все").tag(String?.none)
                    ForEach(model.categories, id: \.self) { Text(Labels.category($0)).tag(String?.some($0)) }
                }
                ForEach(model.visible) { scenario in
                    NavigationLink(value: scenario) { ScenarioRow(scenario: scenario) }
                }
            }
            .refreshable { await model.load() }
        }
        .largeTitle("Сценарии")
        .navigationDestination(for: ScenarioSummary.self) { ScenarioDetailView(container: container, scenario: $0) }
        .task { await model.load() }
    }
}

struct ScenarioDetailView: View {
    let container: AppContainer
    let scenario: ScenarioSummary
    @State private var playing: ScenarioSummary?

    var body: some View {
        List {
            Section {
                Text(scenario.description)
            }
            Section("Условия") {
                LabeledContent("Тип", value: Labels.category(scenario.category))
                LabeledContent("Класс обслуживания", value: scenario.serviceClass)
                LabeledContent("Маршрут", value: scenario.route)
                LabeledContent("Сложность", value: String(repeating: "●", count: scenario.difficulty))
                LabeledContent("Финалов", value: "\(scenario.endingsTotal)")
            }
            Section {
                Text("Решения меняют лояльность пассажира и рейтинг безопасности. На критических шагах идёт таймер: промедление тоже имеет последствия.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .inlineTitle(scenario.title)
        .safeAreaInset(edge: .bottom) {
            Button {
                playing = scenario
            } label: {
                Text("Начать сценарий").bold().frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding()
            .background(.bar)
            .accessibilityIdentifier("scenario.start")
        }
        .scenarioPlayer(item: $playing, container: container)
    }
}

extension View {
    /// Сценарий открывается поверх вкладок на весь экран: во время решения ничто не отвлекает.
    func scenarioPlayer(item: Binding<ScenarioSummary?>, container: AppContainer, onClose: @escaping () -> Void = {}) -> some View {
        #if os(iOS)
        fullScreenCover(item: item, onDismiss: onClose) { ScenarioPlayerView(container: container, scenario: $0) }
        #else
        sheet(item: item, onDismiss: onClose) { ScenarioPlayerView(container: container, scenario: $0) }
        #endif
    }
}
