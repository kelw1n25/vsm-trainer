import Foundation
import Observation

public enum StoryPhase: Equatable, Sendable {
    case loading, intro, dialogue, choices, ending, error
}

/// Кто проигрывает звуки реплик — движок не знает, как именно.
@MainActor
public protocol StorySoundPlayer: AnyObject {
    func play(_ name: String)
    /// «Голос» персонажа во время печати реплики; soft — тише, для мыслей.
    func talk(speaker: String, soft: Bool)
}

/// Движок визуальной новеллы — перенос `frontend/src/story/StoryEngine.ts` один к одному.
///
/// Сервер решает, что происходит в истории: какие варианты доступны, куда ведёт выбор, когда истекает таймер.
/// Движок отвечает за подачу: очередь реплик, печать текста, смену сцен, журнал, автопрокрутку,
/// пропуск уже виденного и сохранение места, где игрок остановился.
@MainActor
@Observable
public final class StoryEngine {
    public static let typeInterval: Duration = .milliseconds(28)
    public static let sentencePauseTicks = 9
    public static let introDuration: Duration = .milliseconds(2400)
    static let autoBaseMs = 1100
    static let autoPerCharMs = 32
    /// Щелчок голоса — на каждый третий напечатанный символ, кроме пробелов и знаков препинания.
    static let talkEveryChars = 3

    public private(set) var phase: StoryPhase = .loading
    public private(set) var run: RunState?
    public private(set) var scene: Scene?
    /// Текущее выражение лица каждого персонажа сцены.
    public private(set) var expressions: [String: String] = [:]
    public private(set) var line: Line?
    /// Сколько символов реплики уже напечатано.
    public private(set) var shownChars = 0
    public private(set) var typing = false
    public private(set) var history: [HistoryEntry] = []
    public private(set) var auto: Bool
    /// Сцену уже видели в прошлых прохождениях — её можно пропустить до выбора.
    public private(set) var canSkip = false
    public private(set) var busy = false
    public private(set) var notice: String?

    private struct Segment {
        let scene: Scene
        let nodeId: String?
        let lines: [Line]
    }

    private let scenarioId: String
    private let employeeId: Int
    public let playerName: String
    private let runs: RunRepository
    private let memory: StoryMemory
    private let now: () -> Date
    private let sleep: @Sendable (Duration) async throws -> Void
    private let reduceMotion: () -> Bool
    private weak var sounds: StorySoundPlayer?
    /// Разница серверных и клиентских часов — таймер считается по серверному времени.
    private var clock = ServerClock()

    private var queue: [Segment] = []
    private var segmentIndex = 0
    private var lineIndex = 0
    private var seenBefore: Set<String> = []
    private var choices: [String] = []
    private var typingTask: Task<Void, Never>?
    private var autoTask: Task<Void, Never>?
    private var introTask: Task<Void, Never>?
    private var timeoutTask: Task<Void, Never>?

    public init(
        scenarioId: String,
        employeeId: Int,
        playerName: String,
        runs: RunRepository,
        memory: StoryMemory,
        now: @escaping () -> Date = Date.init,
        sleep: @escaping @Sendable (Duration) async throws -> Void = { try await Task.sleep(for: $0) },
        reduceMotion: @escaping () -> Bool = { false },
        sounds: StorySoundPlayer? = nil
    ) {
        self.scenarioId = scenarioId
        self.employeeId = employeeId
        self.playerName = playerName
        self.runs = runs
        self.memory = memory
        self.now = now
        self.sleep = sleep
        self.reduceMotion = reduceMotion
        self.sounds = sounds
        auto = memory.auto(employeeId)
    }

    private func setRun(_ state: RunState) {
        run = state
        clock.sync(serverTime: state.serverTime, receivedAt: now())
    }

    /// Задача, которая после паузы что-то делает; отмена паузы отменяет и действие.
    private func after(_ delay: Duration, _ action: @escaping @MainActor () async -> Void) -> Task<Void, Never> {
        Task { [sleep] in
            do { try await sleep(delay) } catch { return }
            guard !Task.isCancelled else { return }
            await action()
        }
    }

    /// Старт или продолжение: сервер вернёт незавершённое прохождение сценария, если оно есть.
    public func start() async {
        do {
            open(try await runs.start(scenarioId: scenarioId))
        } catch {
            phase = .error
            notice = error.userMessage
        }
    }

    private func open(_ state: RunState) {
        setRun(state)
        seenBefore = Set(memory.scenario(employeeId, scenarioId: state.scenarioId).seenNodes)
        let progress = memory.progress(employeeId, runId: state.id)
        choices = progress?.choices ?? []
        history = progress?.history ?? []

        if state.status != .inProgress {
            finish()
            return
        }
        guard let node = state.node else { return }
        if node.choicesShown {
            // Варианты уже показаны и таймер идёт — сразу к выбору
            scene = node.scene
            expressions = Self.expressions(after: node.dialogue, in: node.scene)
            showChoices()
            return
        }
        queue = [Segment(scene: node.scene, nodeId: node.id, lines: node.dialogue)]
        if let progress {
            let resumeAt = progress.nodeId == node.id ? max(0, min(progress.lineIndex, node.dialogue.count - 1)) : 0
            // Реплика, на которой остановились, уже есть в журнале — покажем её снова, но не задвоим запись
            if resumeAt < node.dialogue.count, progress.history.last?.text == node.dialogue[resumeAt].text {
                history = Array(progress.history.dropLast())
            }
            playSegment(0, from: resumeAt)
            return
        }
        // Новое прохождение: затемнение, название сценария, затем первая сцена
        phase = .intro
        introTask = after(reduceMotion() ? .milliseconds(300) : Self.introDuration) { [weak self] in
            self?.playSegment(0, from: 0)
        }
    }

    /// Очередь показа из ответа сервера: реакция на решение, затем новая сцена или финал.
    private func loadScene(_ state: RunState, previous: Scene?) {
        setRun(state)
        var segments: [Segment] = []
        for step in state.lastSteps {
            if step.isTimeout { log(HistoryEntry(kind: "timeout", speaker: nil, text: "Время на решение истекло")) }
            if !step.reaction.isEmpty, let previous { segments.append(Segment(scene: previous, nodeId: nil, lines: step.reaction)) }
        }
        if let node = state.node {
            segments.append(Segment(scene: node.scene, nodeId: node.id, lines: node.dialogue))
        } else if let final = state.final {
            segments.append(Segment(scene: final.scene, nodeId: nil, lines: final.dialogue))
        }
        queue = segments
        playSegment(0, from: 0)
    }

    private func playSegment(_ index: Int, from startLine: Int) {
        introTask?.cancel()
        segmentIndex = index
        guard index < queue.count else {
            endOfQueue()
            return
        }
        let segment = queue[index]
        if let nodeId = segment.nodeId, let run { memory.rememberScene(employeeId, scenarioId: run.scenarioId, nodeId: nodeId) }
        phase = .dialogue
        scene = segment.scene
        expressions = Self.expressions(after: Array(segment.lines.prefix(startLine)), in: segment.scene)
        canSkip = segment.nodeId.map { seenBefore.contains($0) } ?? false
        notice = nil
        if segment.lines.isEmpty {
            playSegment(index + 1, from: 0)
            return
        }
        showLine(startLine)
    }

    private func showLine(_ index: Int) {
        let segment = queue[segmentIndex]
        lineIndex = index
        let current = segment.lines[index]
        log(HistoryEntry(kind: current.kind, speaker: speakerName(current), text: current.text))
        if let expression = current.expression { expressions[current.speaker] = expression }
        line = current
        shownChars = 0
        typing = true
        saveProgress()
        if let sound = current.sound { sounds?.play(sound) }

        typingTask?.cancel()
        autoTask?.cancel()
        if reduceMotion() {
            finishTyping()
            return
        }
        typingTask = Task { [weak self, sleep] in
            let text = Array(current.text)
            var shown = 0
            while true {
                do { try await sleep(Self.typeInterval) } catch { return }
                guard let self, !Task.isCancelled else { return }
                shown += 1
                if shown >= text.count { break }
                let char = text[shown - 1]
                if current.kind != "narration", shown % Self.talkEveryChars == 0, char.isLetter || char.isNumber {
                    self.sounds?.talk(speaker: current.speaker, soft: current.kind == "thought")
                }
                self.shownChars = shown
                // Пауза после конца предложения — реплика «дышит», как в живой речи
                if ".!?…".contains(char), text[shown] == " " {
                    do { try await sleep(Self.typeInterval * Self.sentencePauseTicks) } catch { return }
                }
            }
            guard !Task.isCancelled else { return }
            self?.finishTyping()
        }
    }

    private func finishTyping() {
        typingTask?.cancel()
        shownChars = line.map { $0.text.count } ?? 0
        typing = false
        if auto, let line {
            let delay = min(4000, Self.autoBaseMs + line.text.count * Self.autoPerCharMs)
            autoTask = after(.milliseconds(delay)) { [weak self] in self?.advance() }
        }
    }

    /// «Далее» или касание сцены: допечатать реплику сразу, а если она уже напечатана — следующая.
    public func advance() {
        if phase == .intro {
            playSegment(0, from: lineIndex)
            return
        }
        guard phase == .dialogue else { return }
        if typing {
            finishTyping()
            return
        }
        autoTask?.cancel()
        if lineIndex + 1 < queue[segmentIndex].lines.count {
            showLine(lineIndex + 1)
        } else {
            playSegment(segmentIndex + 1, from: 0)
        }
    }

    /// Пропустить уже знакомую сцену до момента выбора.
    public func skip() {
        guard canSkip, phase == .dialogue else { return }
        typingTask?.cancel()
        autoTask?.cancel()
        let segment = queue[segmentIndex]
        for rest in segment.lines.dropFirst(lineIndex + 1) {
            log(HistoryEntry(kind: rest.kind, speaker: speakerName(rest), text: rest.text))
        }
        expressions = Self.expressions(after: segment.lines, in: segment.scene)
        typing = false
        lineIndex = segment.lines.count - 1
        playSegment(segmentIndex + 1, from: 0)
    }

    public func toggleAuto() {
        auto.toggle()
        memory.saveAuto(employeeId, auto)
        if auto, phase == .dialogue, !typing { advance() }
        if !auto { autoTask?.cancel() }
    }

    private func endOfQueue() {
        if run?.node != nil {
            showChoices()
        } else if run?.final != nil {
            finish()
        }
    }

    /// Сцена дочитана: сервер показывает варианты и запускает таймер.
    private func showChoices() {
        guard let run, let node = run.node else { return }
        autoTask?.cancel()
        phase = .choices
        line = nil
        typing = false
        canSkip = false
        if node.choicesShown {
            saveProgress()
            watchTimeout()
            return
        }
        busy = true
        Task { [weak self] in
            guard let self else { return }
            do {
                self.setRun(try await self.runs.reveal(runId: run.id, nodeId: node.id))
                self.busy = false
                self.saveProgress()
                self.watchTimeout()
            } catch {
                self.busy = false
                await self.recover(error)
            }
        }
    }

    /// Истечение таймера применяет сервер — в момент дедлайна спрашиваем, что произошло.
    private func watchTimeout() {
        timeoutTask?.cancel()
        guard let timeoutAt = run?.node?.timeoutAt else { return }
        let wait = clock.remaining(until: timeoutAt, at: now()) + 0.3
        timeoutTask = after(.milliseconds(Int(wait * 1000))) { [weak self] in await self?.refresh() }
    }

    private func refresh() async {
        guard let before = run else { return }
        do {
            let state = try await runs.state(runId: before.id)
            if state.node?.id == before.node?.id, state.status == .inProgress {
                // Сервер ещё не считает время истёкшим (задержка сети) — подождём ещё
                setRun(state)
                watchTimeout()
            } else {
                loadScene(state, previous: scene)
            }
        } catch {
            notice = error.userMessage
        }
    }

    /// Решение игрока: сервер применяет последствия, история продолжается реакцией персонажей.
    public func choose(_ choiceId: String) async {
        guard !busy, phase == .choices, let run, let node = run.node,
              let choice = node.choices.first(where: { $0.id == choiceId }) else { return }
        timeoutTask?.cancel()
        // Флаг «занят» ставится до запроса: второе касание ничего не отправит
        busy = true
        do {
            let next = try await runs.choose(runId: run.id, nodeId: node.id, choiceId: choiceId)
            choices.append(choiceId)
            log(HistoryEntry(kind: "choice", speaker: playerName, text: choice.text))
            busy = false
            loadScene(next, previous: scene)
        } catch {
            busy = false
            await recover(error)
        }
    }

    /// Время вышло или шаг сменился (повтор после обрыва, второе устройство) — перечитываем состояние с сервера.
    private func recover(_ error: Error) async {
        notice = error.userMessage
        if let code = (error as? APIError)?.code, ["time_expired", "stale_node", "run_finished", "choices_not_shown"].contains(code) {
            await refresh()
        }
    }

    private func finish() {
        timeoutTask?.cancel()
        autoTask?.cancel()
        if let run, let final = run.final {
            memory.rememberEnding(employeeId, scenarioId: run.scenarioId, runId: run.id, ending: final.ending)
        }
        phase = .ending
        line = nil
        typing = false
        canSkip = false
    }

    /// «Пройти заново»: сервер создаёт новое прохождение того же сценария, и история начинается сначала.
    public func restart() async {
        dispose()
        queue = []
        phase = .loading
        run = nil
        scene = nil
        expressions = [:]
        line = nil
        history = []
        notice = nil
        busy = false
        await start()
    }

    private func saveProgress() {
        guard let run else { return }
        let inNode = segmentIndex < queue.count && queue[segmentIndex].nodeId != nil
        memory.saveProgress(employeeId, runId: run.id, RunProgress(
            scenarioId: run.scenarioId,
            nodeId: run.node?.id ?? "",
            lineIndex: inNode ? lineIndex : 0,
            history: history,
            choices: choices,
            ending: memory.progress(employeeId, runId: run.id)?.ending
        ))
    }

    public func dispose() {
        typingTask?.cancel()
        autoTask?.cancel()
        introTask?.cancel()
        timeoutTask?.cancel()
    }

    /// Остаток таймера в секундах по серверным часам; nil — таймера нет.
    public func remaining(at local: Date? = nil) -> TimeInterval? {
        guard let deadline = run?.node?.deadlineAt else { return nil }
        return clock.remaining(until: deadline, at: local ?? now())
    }

    /// Напечатанная часть реплики.
    public var shownText: String {
        guard let line else { return "" }
        return String(line.text.prefix(shownChars))
    }

    private func log(_ entry: HistoryEntry) {
        history.append(entry)
    }

    private func speakerName(_ line: Line) -> String? {
        if line.kind == "narration" { return nil }
        return line.speaker == "player" ? playerName : line.name
    }

    private static func expressions(after lines: [Line], in scene: Scene) -> [String: String] {
        var result = Dictionary(scene.characters.map { ($0.id, $0.expression) }, uniquingKeysWith: { first, _ in first })
        for line in lines {
            if let expression = line.expression { result[line.speaker] = expression }
        }
        return result
    }
}
