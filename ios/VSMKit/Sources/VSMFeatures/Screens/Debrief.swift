import SwiftUI
import VSMCore

/// Разбор — `DebriefPage`: итог, статистика, график шкал, разбор каждого шага, стандарты по ситуациям.
struct DebriefScreen: View {
    @State private var model: DebriefViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer, runId: String) {
        _model = State(initialValue: DebriefViewModel(runId: runId, runs: container.runs, repository: container.trainer))
    }

    var body: some View {
        Page {
            ScreenContent(state: model.state, retry: model.load) { debrief in
                PageTitle("Разбор: \(debrief.scenarioTitle)")
                let accent: Color = switch debrief.outcome {
                case .success: colors.positive
                case .partial: colors.warning
                case .failure: colors.negative
                case .inProgress: colors.brand
                }
                let background: AnyShapeStyle? = debrief.outcome == .success
                    ? AnyShapeStyle(LinearGradient(stops: [.init(color: colors.successBg, location: 0), .init(color: colors.surface, location: 0.6)], startPoint: .top, endPoint: .bottom))
                    : nil
                Card(background: background, accentTop: accent) {
                    Text(Labels.outcome(debrief.outcome).uppercased()).textStyle(VsmType.eyebrow).foregroundStyle(colors.muted)
                    CardTitle(debrief.ending)
                    Text(debrief.finalText).textStyle(VsmType.body).foregroundStyle(colors.text)
                    Stats(items: [
                        ("\(debrief.bestDecisions) из \(debrief.decisions)", "лучших решений"),
                        ("\(debrief.timeouts)", "истёкших таймеров"),
                        (debrief.averageReactionSeconds.map { "\(formatSeconds($0)) с" } ?? "—", "среднее время решения"),
                        ("+\(debrief.xpEarned)", "XP"),
                    ])
                    CompetenceList(points: debrief.competencePoints, titles: model.competenceTitles)
                }
                Card {
                    CardTitle("Как менялись шкалы")
                    ScaleLineChart(
                        labels: ["Старт"] + debrief.steps.indices.map { "Шаг \($0 + 1)" },
                        loyalty: [debrief.initialLoyalty] + debrief.steps.map(\.loyaltyAfter),
                        safety: [debrief.initialSafety] + debrief.steps.map(\.safetyAfter)
                    )
                }
                ForEach(Array(debrief.steps.enumerated()), id: \.offset) { index, step in
                    StepReview(step: step, index: index, titles: model.competenceTitles)
                }
                Card {
                    CardTitle("Как действовать по стандарту")
                    Muted("Рекомендации из «Ситуаций на борту» для ситуаций этого сценария.")
                    ForEach(debrief.situations) { StandardBlock(situation: $0) }
                }
                VStack(spacing: 12) {
                    PrimaryButton(title: "Развитие истории", fill: true) { navigator.open(.map(debrief.scenarioId)) }
                    GhostButton(title: "Пройти заново", fill: true) { navigator.open(.scenario(debrief.scenarioId)) }
                    GhostButton(title: "Вернуться к сценариям", fill: true) { navigator.openTab(.scenarios) }
                }
            }
        }
        .task { await model.load() }
    }
}

/// Секунды как на сайте: без лишних нулей, до десятых.
func formatSeconds(_ value: Double) -> String {
    let rounded = (value * 10).rounded() / 10
    return rounded == rounded.rounded() ? "\(Int(rounded))" : String(format: "%.1f", rounded)
}

/// Сетка `.stats`: крупное число и подпись.
struct Stats: View {
    let items: [(String, String)]
    var valueColor: Color?
    var labelColor: Color?
    @Environment(\.vsm) private var colors

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 16, alignment: .topLeading), GridItem(.flexible(), spacing: 16, alignment: .topLeading)], alignment: .leading, spacing: 16) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                VStack(alignment: .leading, spacing: 0) {
                    Text(item.0).textStyle(VsmType.stat).foregroundStyle(valueColor ?? colors.text)
                    Text(item.1).textStyle(VsmType.body).foregroundStyle(labelColor ?? colors.muted)
                }
                .accessibilityElement(children: .combine)
            }
        }
    }
}

/// Разбор шага `.review`: полоса слева — зелёная (лучший), оранжевая (другой), красная (таймаут).
private struct StepReview: View {
    let step: DebriefStep
    let index: Int
    let titles: [String: String]
    @Environment(\.vsm) private var colors

    var body: some View {
        let accent = step.kind == "timeout" ? colors.negative : (step.wasBest ? colors.positive : colors.warning)
        Card(spacing: 6, accentLeading: accent) {
            let timing = step.elapsedSeconds.flatMap { elapsed in step.timerSeconds.map { " · решение за \(String(format: "%.1f", elapsed)) из \($0) с" } } ?? ""
            Muted("Шаг \(index + 1)\(timing)")
            Text(step.situation).textStyle(VsmType.body).foregroundStyle(colors.text)
            Text(step.kind == "timeout" ? "⏱ Время истекло, решение не принято" : "Ваш выбор: \(step.chosenText ?? "")")
                .textStyle(VsmType.bodyBold)
                .foregroundStyle(colors.text)
            (Text("Лояльность \(Labels.signed(step.loyaltyDelta))").foregroundColor(step.loyaltyDelta < 0 ? colors.negative : colors.positive)
                + Text(" · ").foregroundColor(colors.text)
                + Text("Безопасность \(Labels.signed(step.safetyDelta))").foregroundColor(step.safetyDelta < 0 ? colors.negative : colors.positive))
                .textStyle(VsmType.body)
            CompetenceList(points: step.competences, titles: titles)
            if let explanation = step.explanation { Muted(explanation) }
            if !step.wasBest, let best = step.bestText {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Лучший вариант: \(best)").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                    if let why = step.bestExplanation { Text(why).textStyle(VsmType.body).foregroundStyle(colors.text) }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(colors.successBg, in: RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: Radius.button, style: .continuous).strokeBorder(colors.successBorder, lineWidth: 1))
                .padding(.top, 8)
            }
        }
        .riseIn(index, step: 0.06)
    }
}

/// Стандарт ситуации `.standard`: номер и название, реакция, фразы синим.
struct StandardBlock: View {
    let situation: Situation
    @Environment(\.vsm) private var colors

    var body: some View {
        SoftBlock {
            Text("\(situation.number). \(situation.title)").textStyle(VsmType.h3).foregroundStyle(colors.heading)
            Text(situation.reaction).textStyle(VsmType.body).foregroundStyle(colors.text)
            ForEach(situation.phrases, id: \.self) { Bullet(text: $0, color: colors.brandText) }
        }
    }
}
