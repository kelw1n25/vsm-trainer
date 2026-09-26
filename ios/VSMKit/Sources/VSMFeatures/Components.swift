import SwiftUI
import VSMCore

/// Фирменные цвета тренажёра: синий ВСМ и цвета шкал. Остальное — системная палитра, чтобы
/// приложение выглядело как iOS и само подстраивалось под тёмную тему.
enum Palette {
    static let brand = Color(red: 0.12, green: 0.37, blue: 0.84)
    static let loyalty = Color(red: 0.25, green: 0.47, blue: 0.95)
    static let safety = Color(red: 0.13, green: 0.68, blue: 0.47)

    static func outcome(_ status: RunStatus) -> Color {
        switch status {
        case .success: .green
        case .partial: .orange
        case .failure: .red
        case .inProgress: .secondary
        }
    }

    static func category(_ code: String) -> String {
        ["conflict": "person.2.wave.2", "medical": "cross.case", "service": "cup.and.saucer", "safety": "flame"][code] ?? "tram"
    }
}

/// Шкала лояльности или безопасности 0–100 с изменением за последний шаг.
struct ScaleBar: View {
    let title: String
    let value: Int
    let delta: Int?
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title).font(.caption).foregroundStyle(.secondary)
                Spacer()
                if let delta, delta != 0 {
                    Text(Labels.signed(delta))
                        .font(.caption.bold())
                        .foregroundStyle(delta > 0 ? .green : .red)
                        .transition(.scale.combined(with: .opacity))
                }
                Text("\(value)").font(.caption.monospacedDigit().bold())
            }
            ProgressView(value: Double(value), total: 100).tint(value < 30 ? .red : tint)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title): \(value) из 100")
    }
}

/// Кольцо обратного отсчёта. Когда остаётся четверть времени, краснеет.
struct CountdownRing: View {
    let remaining: TimeInterval
    let fraction: Double

    var body: some View {
        ZStack {
            Circle().stroke(.quaternary, lineWidth: 5)
            Circle()
                .trim(from: 0, to: fraction)
                .stroke(fraction < 0.25 ? Color.red : Palette.brand, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(Int(remaining.rounded(.up)))").font(.headline.monospacedDigit())
        }
        .frame(width: 48, height: 48)
        .accessibilityLabel("Осталось \(Int(remaining.rounded(.up))) секунд")
    }
}

/// Показ сохранённых данных без сети — пользователь должен понимать, что цифры могут быть неактуальны.
struct StaleBanner: View {
    let since: Date

    var body: some View {
        Label("Нет связи. Данные от \(since.formatted(date: .abbreviated, time: .shortened))", systemImage: "wifi.slash")
            .font(.footnote)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(.yellow.opacity(0.15), in: RoundedRectangle(cornerRadius: 10))
    }
}

/// Загрузка / ошибка / данные для любого экрана с `ScreenState`.
struct StateView<Value, Content: View>: View {
    let state: ScreenState<Value>
    let retry: () async -> Void
    @ViewBuilder let content: (Value, Date?) -> Content

    var body: some View {
        switch state {
        case .loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let .failed(message):
            ContentUnavailableView {
                Label("Не удалось загрузить", systemImage: "exclamationmark.triangle")
            } description: {
                Text(message)
            } actions: {
                Button("Повторить") { Task { await retry() } }.buttonStyle(.borderedProminent)
            }
        case let .loaded(value, staleSince):
            content(value, staleSince)
        }
    }
}

extension View {
    /// Заголовок навигации в стиле iOS: крупный на корневых экранах.
    func largeTitle(_ title: String) -> some View {
        #if os(iOS)
        navigationTitle(title).navigationBarTitleDisplayMode(.large)
        #else
        navigationTitle(title)
        #endif
    }

    func inlineTitle(_ title: String) -> some View {
        #if os(iOS)
        navigationTitle(title).navigationBarTitleDisplayMode(.inline)
        #else
        navigationTitle(title)
        #endif
    }
}
