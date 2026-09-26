import SwiftUI
import VSMCore

struct ScenarioPlayerView: View {
    let container: AppContainer
    let scenario: ScenarioSummary
    @State private var model: ScenarioPlayerViewModel
    @State private var confirmExit = false
    @Environment(\.dismiss) private var dismiss

    init(container: AppContainer, scenario: ScenarioSummary) {
        self.container = container
        self.scenario = scenario
        _model = State(initialValue: ScenarioPlayerViewModel(
            scenarioId: scenario.id, runs: container.runs, activeRuns: container.activeRuns, events: container.trainer
        ))
    }

    var body: some View {
        NavigationStack {
            content
                .inlineTitle(scenario.title)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            if model.phase == .finished { dismiss() } else { confirmExit = true }
                        } label: {
                            Image(systemName: "xmark").accessibilityLabel("Закрыть сценарий")
                        }
                    }
                }
                .confirmationDialog("Выйти из сценария?", isPresented: $confirmExit, titleVisibility: .visible) {
                    Button("Выйти", role: .destructive) {
                        Task {
                            await model.exit()
                            dismiss()
                        }
                    }
                } message: {
                    Text("Прогресс сохранится на сервере. Если у шага идёт таймер, он не остановится.")
                }
        }
        .task { await model.load() }
        .task {
            // Раз в четверть секунды: не пора ли спросить сервер о последствиях истёкшего таймера
            while !Task.isCancelled {
                await model.tick()
                try? await Task.sleep(nanoseconds: 250_000_000)
            }
        }
        .sensoryFeedback(.warning, trigger: model.phase == .timedOut)
        .sensoryFeedback(.success, trigger: model.phase == .finished)
    }

    @ViewBuilder
    private var content: some View {
        switch model.phase {
        case .loading:
            ProgressView("Загружаем сценарий…").frame(maxWidth: .infinity, maxHeight: .infinity)
        case let .failed(message):
            ContentUnavailableView {
                Label("Сценарий не открылся", systemImage: "exclamationmark.triangle")
            } description: {
                Text(message)
            } actions: {
                Button("Повторить") { Task { await model.load() } }.buttonStyle(.borderedProminent)
            }
        case .finished:
            if let run = model.run, let final = run.final {
                FinalView(container: container, run: run, final: final, consequences: model.lastSteps) { dismiss() }
            }
        default:
            if let run = model.run, let node = run.node {
                playing(run: run, node: node)
            }
        }
    }

    private func playing(run: RunState, node: Node) -> some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                ScaleBar(title: "Лояльность пассажира", value: run.loyalty, delta: model.lastSteps.map(\.loyaltyDelta).reduce(0, +), tint: Palette.loyalty)
                ScaleBar(title: "Рейтинг безопасности", value: run.safety, delta: model.lastSteps.map(\.safetyDelta).reduce(0, +), tint: Palette.safety)
            }
            .padding()
            .animation(.snappy, value: run.loyalty)
            .animation(.snappy, value: run.safety)
            Divider()
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        ForEach(Array(model.lastSteps.enumerated()), id: \.offset) { _, step in
                            ConsequenceCard(step: step)
                        }
                        ForEach(Array(node.dialogue.enumerated()), id: \.offset) { _, line in
                            LineView(line: line)
                        }
                        Color.clear.frame(height: 1).id("end")
                    }
                    .padding()
                }
                .onChange(of: node.id) { _, _ in withAnimation { proxy.scrollTo("end") } }
            }
            if let notice = model.notice {
                Label(notice, systemImage: "exclamationmark.circle")
                    .font(.footnote).foregroundStyle(.orange)
                    .padding(.horizontal).padding(.top, 8)
                    .accessibilityIdentifier("player.notice")
            }
            decision(node: node).padding().background(.bar)
        }
    }

    @ViewBuilder
    private func decision(node: Node) -> some View {
        switch model.phase {
        case .reading:
            Button {
                Task { await model.revealChoices() }
            } label: {
                Text("К решению").bold().frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .accessibilityIdentifier("player.reveal")
        case .timedOut:
            ProgressView("Время вышло — ситуация развивается без вас…")
        default:
            VStack(spacing: 10) {
                TimelineView(.periodic(from: .now, by: 0.2)) { context in
                    if let remaining = model.remainingSeconds(at: context.date), let fraction = model.remainingFraction(at: context.date) {
                        HStack {
                            CountdownRing(remaining: remaining, fraction: fraction)
                            Text("Решите, пока не истекло время").font(.subheadline).foregroundStyle(.secondary)
                            Spacer()
                        }
                    }
                }
                ForEach(node.choices) { choice in
                    Button {
                        Task { await model.choose(choice) }
                    } label: {
                        Text(choice.text).frame(maxWidth: .infinity, alignment: .leading).multilineTextAlignment(.leading)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    .disabled(model.phase != .choosing)
                }
                if model.phase == .submitting { ProgressView() }
            }
        }
    }
}

/// Последствия предыдущего шага: что выбрано, как отреагировали пассажиры, как изменились шкалы.
struct ConsequenceCard: View {
    let step: Step

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(step.isTimeout ? "Время вышло" : step.text, systemImage: step.isTimeout ? "timer" : "checkmark.bubble")
                .font(.subheadline.bold())
            ForEach(Array(step.reaction.enumerated()), id: \.offset) { _, line in LineView(line: line) }
            HStack(spacing: 16) {
                Text("Лояльность \(Labels.signed(step.loyaltyDelta))").foregroundStyle(step.loyaltyDelta < 0 ? .red : .green)
                Text("Безопасность \(Labels.signed(step.safetyDelta))").foregroundStyle(step.safetyDelta < 0 ? .red : .green)
            }
            .font(.caption.bold())
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
    }
}

/// Реплика: рассказчик — курсивом, мысль — в скобках, речь персонажа — с именем.
struct LineView: View {
    let line: Line

    var body: some View {
        switch line.kind {
        case "narration":
            Text(line.text).italic().foregroundStyle(.secondary)
        case "thought":
            Text("(\(line.text))").italic().foregroundStyle(.secondary)
        default:
            VStack(alignment: .leading, spacing: 2) {
                Text(line.speaker == "player" ? "Вы" : (line.name ?? "")).font(.caption.bold()).foregroundStyle(Palette.brand)
                Text(line.text)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(line.speaker == "player" ? Palette.brand.opacity(0.1) : Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
        }
    }
}

/// Финал: чем закончилось, сколько XP, какие ачивки и уровень, переход к разбору.
struct FinalView: View {
    let container: AppContainer
    let run: RunState
    let final: Final
    let consequences: [Step]
    let close: () -> Void

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 8) {
                    Text(Labels.outcome(final.outcome)).font(.caption.bold()).foregroundStyle(Palette.outcome(final.outcome))
                    Text(final.ending).font(.title2.bold())
                    Text(final.text)
                }
                .padding(.vertical, 4)
            }
            ForEach(Array(consequences.enumerated()), id: \.offset) { _, step in
                Section { ConsequenceCard(step: step) }
            }
            Section("Итог") {
                LabeledContent("Лояльность пассажира", value: "\(run.loyalty)")
                LabeledContent("Рейтинг безопасности", value: "\(run.safety)")
                LabeledContent("Получено", value: "+\(final.xpEarned) XP")
            }
            if let level = run.levelUp {
                Section { Label("Новый уровень: \(level.title)", systemImage: "arrow.up.circle.fill").foregroundStyle(.green) }
            }
            if !run.newAchievements.isEmpty {
                Section("Новые достижения") {
                    ForEach(run.newAchievements, id: \.code) { achievement in
                        VStack(alignment: .leading) {
                            Label(achievement.title, systemImage: "rosette").font(.headline)
                            Text(achievement.description).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
            }
            Section {
                NavigationLink("Разбор решений") { DebriefView(container: container, runId: run.id) }
                    .accessibilityIdentifier("final.debrief")
                Button("Закрыть", action: close)
            }
        }
    }
}
