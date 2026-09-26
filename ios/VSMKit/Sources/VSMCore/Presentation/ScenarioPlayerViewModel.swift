import Foundation
import Observation

/// Прохождение сценария на телефоне. Исход каждого шага, таймер и шкалы считает сервер;
/// здесь — порядок действий и защита от некорректных: двойное нажатие, ответ после таймера, повтор после обрыва.
@MainActor
@Observable
public final class ScenarioPlayerViewModel {
    public enum Phase: Equatable {
        case loading
        /// Сцена и диалог на экране, варианты скрыты, таймер ещё не идёт.
        case reading
        /// Варианты показаны; если у шага есть таймер — идёт обратный отсчёт.
        case choosing
        /// Запрос в пути — повторные нажатия игнорируются.
        case submitting
        /// Время вышло: ждём, пока сервер применит последствия промедления.
        case timedOut
        case finished
        case failed(String)
    }

    public private(set) var phase: Phase = .loading
    public private(set) var run: RunState?
    /// Реакция персонажей и изменение шкал на последнем шаге — последствия решения.
    public private(set) var lastSteps: [Step] = []
    /// Сообщение, которое не прерывает сценарий (например, обрыв связи при ответе).
    public private(set) var notice: String?

    private let scenarioId: String
    private let runs: RunRepository
    private let activeRuns: ActiveRunStore
    private let events: TrainerRepository
    private let now: () -> Date
    private var clock = ServerClock()

    public init(scenarioId: String, runs: RunRepository, activeRuns: ActiveRunStore, events: TrainerRepository, now: @escaping () -> Date = Date.init) {
        self.scenarioId = scenarioId
        self.runs = runs
        self.activeRuns = activeRuns
        self.events = events
        self.now = now
    }

    public var node: Node? { run?.node }

    /// Сколько секунд осталось на решение, во времени сервера. `nil` — у шага нет таймера или он не идёт.
    public func remainingSeconds(at local: Date? = nil) -> TimeInterval? {
        guard phase == .choosing || phase == .submitting, let deadline = node?.deadlineAt else { return nil }
        return clock.remaining(until: deadline, at: local ?? now())
    }

    /// Доля оставшегося времени для кольца таймера, 0…1.
    public func remainingFraction(at local: Date? = nil) -> Double? {
        guard let remaining = remainingSeconds(at: local), let total = node?.timerSeconds, total > 0 else { return nil }
        return remaining / Double(total)
    }

    /// Старт или продолжение: сервер вернёт незавершённое прохождение этого сценария, если оно есть.
    public func load() async {
        phase = .loading
        do {
            apply(try await runs.start(scenarioId: scenarioId))
        } catch {
            phase = .failed(error.userMessage)
        }
    }

    /// Сцена дочитана — показать варианты. Таймер стартует на сервере в этот момент.
    public func revealChoices() async {
        guard phase == .reading, let run, let node else { return }
        phase = .submitting
        do {
            apply(try await runs.reveal(runId: run.id, nodeId: node.id))
        } catch {
            await recover(from: error, fallback: .reading)
        }
    }

    public func choose(_ choice: Choice) async {
        guard phase == .choosing, let run, let node, node.choices.contains(choice) else { return }
        phase = .submitting
        notice = nil
        do {
            apply(try await runs.choose(runId: run.id, nodeId: node.id, choiceId: choice.id))
        } catch {
            await recover(from: error, fallback: .choosing)
        }
    }

    /// Вызывается экраном раз в долю секунды. Когда время вышло, просим сервер: он применит таймаут
    /// (штраф шкал и переход в ветку промедления) и вернёт следующую сцену.
    public func tick() async {
        guard phase == .choosing, let run, let node, let timeoutAt = node.timeoutAt else { return }
        guard clock.serverNow(at: now()) >= timeoutAt else { return }
        phase = .timedOut
        do {
            let state = try await runs.state(runId: run.id)
            if state.node?.id == node.id, state.status == .inProgress {
                // Сервер ещё не считает время истёкшим (задержка сети) — спросим на следующем тике
                self.run = state
                phase = .choosing
                clock.sync(serverTime: state.serverTime, receivedAt: now())
            } else {
                apply(state)
            }
        } catch {
            phase = .choosing
            notice = error.userMessage
        }
    }

    /// Пользователь закрыл сценарий до финала: прогресс на сервере сохранён, фиксируем выход для аналитики.
    public func exit() async {
        guard let run, run.status == .inProgress else { return }
        await events.recordEvent("run_exited", runId: run.id, notificationId: nil)
    }

    private func apply(_ state: RunState) {
        clock.sync(serverTime: state.serverTime, receivedAt: now())
        run = state
        if !state.lastSteps.isEmpty { lastSteps = state.lastSteps }
        if state.status == .inProgress {
            activeRuns.save(ActiveRun(runId: state.id, scenarioId: state.scenarioId, title: state.scenarioTitle))
            phase = state.node?.choicesShown == true ? .choosing : .reading
        } else {
            activeRuns.clear()
            phase = .finished
        }
    }

    /// Ответ не принят. Время вышло или шаг уже сменился (повтор после обрыва, второе устройство) —
    /// перечитываем состояние с сервера. Нет сети — остаёмся на шаге, ответ можно отправить ещё раз.
    private func recover(from error: Error, fallback: Phase) async {
        guard let run else { return }
        switch (error as? APIError)?.code {
        case "time_expired", "stale_node", "choices_not_shown", "run_finished":
            do {
                apply(try await runs.state(runId: run.id))
                if (error as? APIError)?.code == "time_expired" { notice = "Время на решение истекло — ответ не принят." }
            } catch {
                phase = fallback
                notice = error.userMessage
            }
        default:
            phase = fallback
            notice = error.userMessage
        }
    }
}
