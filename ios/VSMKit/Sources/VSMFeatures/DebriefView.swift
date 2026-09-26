import SwiftUI
import VSMCore

/// Обучающий разбор: по каждому шагу — решение, почему оно так сказалось на шкалах, как было лучше.
struct DebriefView: View {
    @State private var model: DebriefViewModel

    init(container: AppContainer, runId: String) {
        _model = State(initialValue: DebriefViewModel(runId: runId, runs: container.runs, repository: container.trainer))
    }

    var body: some View {
        StateView(state: model.state, retry: model.load) { debrief, _ in
            List {
                Section {
                    LabeledContent("Итог", value: Labels.outcome(debrief.outcome))
                    LabeledContent("Лояльность", value: "\(debrief.initialLoyalty) → \(debrief.loyalty)")
                    LabeledContent("Безопасность", value: "\(debrief.initialSafety) → \(debrief.safety)")
                    LabeledContent("Лучших решений", value: "\(debrief.bestDecisions) из \(debrief.decisions)")
                    if debrief.timeouts > 0 { LabeledContent("Истёк таймер", value: "\(debrief.timeouts)") }
                    if let reaction = debrief.averageReactionSeconds {
                        LabeledContent("Среднее время решения", value: "\(reaction.formatted(.number.precision(.fractionLength(1)))) с")
                    }
                    LabeledContent("Получено", value: "+\(debrief.xpEarned) XP")
                }
                if !debrief.competencePoints.isEmpty {
                    Section("Компетенции") {
                        ForEach(debrief.competencePoints.sorted { $0.value > $1.value }, id: \.key) { code, points in
                            LabeledContent(model.title(ofCompetence: code), value: Labels.signed(points))
                        }
                    }
                }
                ForEach(Array(debrief.steps.enumerated()), id: \.offset) { index, step in
                    Section("Шаг \(index + 1). \(step.situation)") { DebriefStepView(step: step) }
                }
                if !debrief.missedSteps.isEmpty {
                    Section("Что можно было сделать иначе") {
                        ForEach(Array(debrief.missedSteps.enumerated()), id: \.offset) { _, step in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(step.bestText ?? "").font(.subheadline.bold())
                                if let why = step.bestExplanation { Text(why).font(.caption).foregroundStyle(.secondary) }
                            }
                        }
                    }
                }
                ForEach(debrief.situations, id: \.number) { situation in
                    Section("Стандарт: ситуация №\(situation.number)") {
                        Text(situation.title).font(.subheadline.bold())
                        Text(situation.reaction).font(.callout)
                        // Фразы в справочнике уже в кавычках — показываем как есть
                        ForEach(situation.phrases, id: \.self) { Text($0).font(.callout).italic() }
                    }
                }
            }
        }
        .inlineTitle("Разбор")
        .task { await model.load() }
    }
}

struct DebriefStepView: View {
    let step: DebriefStep

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if step.kind == "timeout" {
                Label("Время истекло — решение принято за вас", systemImage: "timer").foregroundStyle(.orange)
            } else if let chosen = step.chosenText {
                Label(chosen, systemImage: step.wasBest ? "checkmark.circle.fill" : "exclamationmark.circle")
                    .foregroundStyle(step.wasBest ? .green : .orange)
            }
            if let explanation = step.explanation { Text(explanation).font(.callout) }
            HStack(spacing: 12) {
                Text("Лояльность \(Labels.signed(step.loyaltyDelta)) → \(step.loyaltyAfter)")
                Text("Безопасность \(Labels.signed(step.safetyDelta)) → \(step.safetyAfter)")
            }
            .font(.caption.monospacedDigit()).foregroundStyle(.secondary)
            if let elapsed = step.elapsedSeconds, let timer = step.timerSeconds {
                Text("Решение за \(elapsed.formatted(.number.precision(.fractionLength(1)))) с из \(timer)").font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}
