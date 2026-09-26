import SwiftUI
import VSMCore

/// Полноэкранный режим визуальной новеллы — `StoryPlayer` сайта: сцена, персонажи, диалог, выбор, финал.
struct StoryPlayerScreen: View {
    private let container: AppContainer
    private let employeeId: Int
    @State private var engine: StoryEngine
    @State private var audio: StoryAudio
    @State private var muted: Bool
    @State private var historyOpen = false
    @State private var endings: (reached: Int, total: Int)?
    @State private var titles: [String: String] = [:]
    @State private var revealed = false
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce

    init(container: AppContainer, scenarioId: String) {
        self.container = container
        var employeeId = 0
        var name = "Проводник"
        if case let .signedIn(id, fullName, _) = container.session.state {
            employeeId = id
            name = Labels.firstName(fullName)
        }
        self.employeeId = employeeId
        let muted = container.storyMemory.muted(employeeId)
        let audio = StoryAudio(muted: muted)
        let preferences = container.preferences
        _audio = State(initialValue: audio)
        _muted = State(initialValue: muted)
        _engine = State(initialValue: StoryEngine(
            scenarioId: scenarioId, employeeId: employeeId, playerName: name, runs: container.runs, memory: container.storyMemory,
            reduceMotion: { preferences.reduceMotion }, sounds: audio
        ))
    }

    var body: some View {
        ZStack {
            Palette.storyBlack.ignoresSafeArea()
            if engine.phase == .error {
                VStack(spacing: 16) {
                    Text(engine.notice ?? "").textStyle(VsmType.body).foregroundStyle(.white).multilineTextAlignment(.center)
                    PrimaryButton(title: "К сценариям", action: toScenarios)
                }
                .padding(24)
            } else if let run = engine.run {
                stage(run)
            }
        }
        .task {
            audio.startMusic()
            if let meta = try? await container.trainer.meta() { titles = meta.value.competences }
            await engine.start()
        }
        .onChange(of: engine.run?.characters) { _, characters in audio.setVoices(for: characters ?? []) }
        .onChange(of: engine.phase) { _, phase in
            // Новелла проявляется из темноты (`story-reveal`, 0.9 с)
            if phase != .intro, phase != .loading, !revealed {
                if reduce { revealed = true } else { withAnimation(.easeOut(duration: 0.9)) { revealed = true } }
            }
            if phase == .ending, let run = engine.run { Task { await loadEndings(run.scenarioId) } }
        }
        .onDisappear {
            engine.dispose()
            audio.release()
        }
        .preferredColorScheme(.dark)
    }

    @ViewBuilder
    private func stage(_ run: RunState) -> some View {
        let cast = Dictionary(run.characters.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        ZStack(alignment: .top) {
            if let scene = engine.scene {
                ZStack {
                    StoryBackground(name: scene.background)
                    StoryCast(
                        characters: scene.characters, expressions: engine.expressions, cast: cast,
                        speaker: engine.line?.speaker, speaking: engine.typing && engine.line?.kind == "speech"
                    )
                }
                .opacity(revealed ? 1 : 0)
                // Касание сцены — то же, что «Далее»: мгновенно допечатывает реплику или листает дальше
                if engine.phase == .dialogue {
                    Color.clear.contentShape(Rectangle()).padding(.top, 150).onTapGesture { engine.advance() }.accessibilityHidden(true)
                }
                StoryBar(run: run, engine: engine, muted: muted, exit: exit, history: { historyOpen = true }, sound: toggleSound)
                VStack {
                    Spacer()
                    Group {
                        switch engine.phase {
                        case .dialogue:
                            if let line = engine.line { DialogueBox(line: line, engine: engine, cast: cast) }
                        case .choices:
                            if let node = run.node { Choices(node: node, engine: engine) }
                        default:
                            EmptyView()
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.bottom, 12)
                }
                if let notice = engine.notice, engine.phase != .ending {
                    Text(notice)
                        .textStyle(VsmType.small)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Palette.storyBlack.opacity(0.8), in: RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
                        .padding(.top, 170)
                        .padding(.horizontal, 16)
                        .accessibilityAddTraits(.updatesFrequently)
                }
            }
            if engine.phase == .intro { Intro(run: run) { engine.advance() } }
            if engine.phase == .ending, run.final != nil {
                StoryEnd(
                    run: run, endings: endings, titles: titles,
                    map: { navigator.leaveStory(to: .map(run.scenarioId)) },
                    debrief: { navigator.leaveStory(to: .debrief(run.id)) },
                    restart: { Task { await engine.restart() } },
                    scenarios: toScenarios
                )
            }
            if historyOpen {
                HistoryPanel(entries: engine.history) { historyOpen = false }
                    .transition(reduce ? .opacity : .opacity.combined(with: .move(edge: .trailing)))
            }
        }
        .animation(reduce ? nil : .easeOut(duration: 0.3), value: historyOpen)
    }

    /// «← Выйти»: прогресс сохранён на сервере; выход до финала фиксируем для аналитики.
    private func exit() {
        if let run = engine.run, run.status == .inProgress {
            let trainer = container.trainer
            Task { await trainer.recordEvent("run_exited", runId: run.id, notificationId: nil) }
        }
        navigator.leaveStory(to: nil)
    }

    private func toScenarios() {
        navigator.leaveStory(to: nil)
        navigator.openTab(.scenarios)
    }

    private func toggleSound() {
        muted.toggle()
        container.storyMemory.saveMuted(employeeId, muted)
        audio.setMuted(muted)
    }

    /// «Открыто финалов: N из M» на финальном экране — из архива веток сервера.
    private func loadEndings(_ scenarioId: String) async {
        guard let map = try? await container.trainer.storyMap(scenarioId: scenarioId) else { return }
        endings = (map.value.endings.filter(\.reached).count, map.value.endings.count)
    }
}

// MARK: - Панель

/// Верхняя стеклянная панель: «← Выйти», точки шагов, две шкалы, История / Звук / Авто / Пропустить.
private struct StoryBar: View {
    let run: RunState
    let engine: StoryEngine
    let muted: Bool
    let exit: () -> Void
    let history: () -> Void
    let sound: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        let total = run.stepsTaken + max(run.stepsLeft, run.node != nil ? 1 : 0)
        let step = run.lastSteps.last
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Button(action: exit) {
                    Text("← Выйти").textStyle(VsmType.storyBar.weighted(.semibold)).foregroundStyle(.white.opacity(0.85)).padding(.vertical, 6).frame(minHeight: 32)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Выйти, прогресс сохранится")
                HStack(spacing: 6) {
                    ForEach(0..<total, id: \.self) { index in
                        let done = index < run.stepsTaken
                        let current = index == run.stepsTaken && run.node != nil
                        Circle()
                            .fill(done ? Color.white : (current ? colors.brand : .clear))
                            .overlay(Circle().strokeBorder(done || current ? Color.white : Color.white.opacity(0.7), lineWidth: 1.5))
                            .frame(width: 8, height: 8)
                            .padding(current ? 3 : 0)
                            .background(current ? colors.brand.opacity(0.35) : .clear, in: Circle())
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Шаг \(run.stepsTaken + 1) из \(total)")
            }
            HStack(spacing: 8) {
                ScaleMeter(label: "Лояльность", value: run.loyalty, delta: step?.loyaltyDelta ?? 0, color: Palette.storyLoyalty, key: run.stepsTaken)
                ScaleMeter(label: "Безопасность", value: run.safety, delta: step?.safetyDelta ?? 0, color: Palette.storySafety, key: run.stepsTaken)
            }
            HStack(spacing: 6) {
                ToolButton(title: "История", on: false, enabled: true, action: history)
                ToolButton(title: muted ? "Звук выкл." : "Звук", on: !muted, enabled: true, action: sound)
                ToolButton(title: "Авто", on: engine.auto, enabled: true) { engine.toggleAuto() }
                ToolButton(title: "Пропустить", on: false, enabled: engine.canSkip) { engine.skip() }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Palette.storyBlack.opacity(0.42), in: RoundedRectangle(cornerRadius: Radius.storyBar, style: .continuous))
        .padding(8)
    }
}

/// Дельта над шкалой после решения (`delta-float`, 2.6 с): появляется, висит, уплывает вверх.
private struct DeltaFloat {
    var opacity: Double = 0
    var y: CGFloat = 0
}

/// Шкала панели: подпись, дорожка, значение и всплывающая дельта после решения.
private struct ScaleMeter: View {
    let label: String
    let value: Int
    let delta: Int
    let color: Color
    let key: Int
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        HStack(spacing: 6) {
            Text(label).textStyle(VsmType.storyBar).foregroundStyle(.white.opacity(0.8)).lineLimit(1).fixedSize()
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.25))
                    Capsule().fill(color).frame(width: proxy.size.width * CGFloat(min(max(value, 0), 100)) / 100)
                }
            }
            .frame(height: 6)
            .animation(reduce ? nil : .site(0.8), value: value)
            Text("\(value)").textStyle(VsmType.storyBar.weighted(.bold)).foregroundStyle(.white)
        }
        .overlay(alignment: .topTrailing) {
            if delta != 0, !reduce {
                Text(Labels.signed(delta))
                    .textStyle(TextStyle(12, .heavy, line: 16))
                    .foregroundStyle(delta < 0 ? Palette.deltaDown : Palette.deltaUp)
                    .keyframeAnimator(initialValue: DeltaFloat(), trigger: key) { view, value in
                        view.opacity(value.opacity).offset(y: value.y - 14)
                    } keyframes: { _ in
                        KeyframeTrack(\.opacity) {
                            LinearKeyframe(1, duration: 0.39)
                            LinearKeyframe(1, duration: 1.43)
                            LinearKeyframe(0, duration: 0.78)
                        }
                        KeyframeTrack(\.y) {
                            LinearKeyframe(6, duration: 0.01)
                            LinearKeyframe(0, duration: 0.38)
                            LinearKeyframe(0, duration: 1.43)
                            LinearKeyframe(-6, duration: 0.78)
                        }
                    }
                    .accessibilityHidden(true)
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value) из 100")
    }
}

private struct ToolButton: View {
    let title: String
    let on: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.tab, style: .continuous)
        Button(action: action) {
            Text(title)
                .textStyle(VsmType.storyBar)
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 32)
                .background(on ? Color.white.opacity(0.18) : .clear, in: shape)
                .overlay(shape.strokeBorder(on ? Color.white : Color.white.opacity(0.35), lineWidth: 1))
                .opacity(enabled ? 1 : 0.4)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

// MARK: - Диалог

/// Окно диалога `.story-box`: имя говорящего заглавными, текст печатается по символу; речь — в «ёлочках»,
/// мысли — курсивом с синей полосой, рассказчик — тёмное стекло. Ненапечатанный остаток держит высоту окна.
private struct DialogueBox: View {
    let line: Line
    let engine: StoryEngine
    let cast: [String: Character]
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce
    @State private var nudge = false

    var body: some View {
        let narration = line.kind == "narration"
        let thought = line.kind == "thought"
        let name = narration ? "Рассказчик" : (line.speaker == "player" ? engine.playerName : (line.name ?? cast[line.speaker]?.name ?? ""))
        let ink = narration ? Color(hex: 0xEEF3FB) : colors.storyInk
        let accent = narration ? Palette.storyNarration : colors.brand
        let shape = RoundedRectangle(cornerRadius: Radius.storyBox, style: .continuous)
        let quote = line.kind == "speech"
        let text = Text((quote ? "«" : "") + String(line.text.prefix(engine.shownChars)) + (quote && !engine.typing ? "»" : ""))
            + Text(String(line.text.dropFirst(engine.shownChars))).foregroundColor(.clear)
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .lastTextBaseline, spacing: 10) {
                Text(name.uppercased()).textStyle(VsmType.storyName).foregroundStyle(accent)
                if !narration, let role = line.role { Text(role).textStyle(VsmType.caption).foregroundStyle(colors.muted).lineLimit(1) }
                if thought { Text("мысли").textStyle(VsmType.caption).foregroundStyle(colors.muted) }
            }
            .padding(.bottom, 8)
            HStack(alignment: .top, spacing: 13) {
                if thought { Rectangle().fill(colors.brand.opacity(0.45)).frame(width: 3) }
                text
                    .textStyle(narration ? VsmType.storyNarration : VsmType.storyText)
                    .italic(thought)
                    .foregroundStyle(ink.opacity(thought ? 0.78 : 1))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .fixedSize(horizontal: false, vertical: true)
            Button { engine.advance() } label: {
                Text((engine.typing ? "Показать" : "Далее") + " →")
                    .textStyle(VsmType.bodyBold)
                    .foregroundStyle(accent)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 4)
                    .offset(x: nudge ? 4 : 0)
                    .frame(minHeight: Metrics.touchMin)
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity, alignment: .trailing)
            .padding(.top, 4)
        }
        .padding(.horizontal, 18)
        .padding(.top, 16)
        .padding(.bottom, 4)
        .background(narration ? AnyShapeStyle(Palette.storyBlack.opacity(0.72)) : AnyShapeStyle(colors.storyGlass), in: shape)
        .overlay(shape.strokeBorder(narration ? Color.white.opacity(0.12) : colors.storyGlassBorder, lineWidth: 1))
        .shadow(color: Palette.storyBlack.opacity(0.4), radius: 20, y: 10)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(name): \(line.text)")
        .accessibilityHint("Коснитесь дважды — далее")
        .accessibilityAction { engine.advance() }
        .onAppear {
            guard !reduce else { return }
            withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) { nudge = true }
        }
    }
}

// MARK: - Выбор

/// Варианты `.story-choices`: полоса таймера «N с на решение» (последние 5 с — красная), карточки с номерами по очереди.
private struct Choices: View {
    let node: Node
    let engine: StoryEngine
    @State private var picked: String?
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(spacing: 10) {
            if let timer = node.timerSeconds, node.deadlineAt != nil {
                TimelineView(.periodic(from: .now, by: 0.1)) { _ in
                    let remaining = engine.remaining() ?? 0
                    let hurry = remaining < 5
                    ZStack {
                        GeometryReader { proxy in
                            Rectangle()
                                .fill(LinearGradient(
                                    colors: hurry ? [Palette.timerHurryStart, Palette.timerHurryEnd] : [Color(hex: 0x146BFF), Color(hex: 0x4F8DFF)],
                                    startPoint: .leading, endPoint: .trailing
                                ))
                                .frame(width: proxy.size.width * min(max(remaining / Double(timer), 0), 1))
                        }
                        Text("\(Int(remaining.rounded(.up))) с на решение").textStyle(VsmType.caption.weighted(.bold)).foregroundStyle(.white)
                    }
                    .frame(height: 30)
                    .background(Palette.storyBlack.opacity(0.55))
                    .clipShape(RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("Осталось \(Int(remaining.rounded(.up))) секунд")
                }
            }
            if node.choices.isEmpty { Text("…").textStyle(VsmType.body).foregroundStyle(.white) }
            ForEach(Array(node.choices.enumerated()), id: \.element.id) { index, choice in
                ChoiceCard(index: index, choice: choice, picked: picked == choice.id, enabled: !engine.busy && picked == nil && (engine.remaining() ?? 1) > 0) {
                    picked = choice.id
                    Task { await engine.choose(choice.id) }
                }
            }
        }
        .id(node.id)
        // Если ответ не принят (обрыв связи), разрешаем выбрать снова
        .onChange(of: engine.busy) { _, busy in if !busy, engine.notice != nil { picked = nil } }
    }
}

private struct ChoiceCard: View {
    let index: Int
    let choice: Choice
    let picked: Bool
    let enabled: Bool
    let action: () -> Void
    @State private var shown = false
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.storyChoice, style: .continuous)
        Button(action: action) {
            HStack(spacing: 14) {
                Text("\(index + 1)")
                    .textStyle(VsmType.bodyBold.weighted(.heavy))
                    .foregroundStyle(colors.brandText)
                    .frame(width: 30, height: 30)
                    .background(colors.softStrong, in: RoundedRectangle(cornerRadius: Radius.tab, style: .continuous))
                Text(choice.text).textStyle(VsmType.storyChoice).foregroundStyle(colors.storyInk).frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(colors.storyGlass, in: shape)
            .overlay(shape.strokeBorder(picked ? colors.brand : colors.storyGlassBorder, lineWidth: picked ? 3 : 1))
            .shadow(color: Palette.storyBlack.opacity(0.35), radius: 10, y: 5)
            .opacity(enabled || picked ? 1 : 0.55)
        }
        .buttonStyle(PressStyle())
        .disabled(!enabled)
        .opacity(shown || reduce ? 1 : 0)
        .offset(y: shown || reduce ? 0 : 14)
        .accessibilityLabel("Вариант \(index + 1): \(choice.text)")
        .accessibilityIdentifier("story.choice.\(index)")
        .onAppear {
            guard !reduce else { return }
            withAnimation(.site(0.4).delay(Double(index + 1) * 0.1)) { shown = true }
        }
    }
}

// MARK: - Заставка, финал, журнал

/// Заставка: на тёмном экране категория, класс и маршрут, затем название; касание — сразу к сцене.
private struct Intro: View {
    let run: RunState
    let skip: () -> Void
    @State private var text = false
    @State private var fade = false
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        ZStack {
            Palette.storyBlack.ignoresSafeArea()
            VStack(spacing: 14) {
                Text("\(Labels.category(run.category)) · \(run.serviceClass) · \(run.route)".uppercased())
                    .textStyle(TextStyle(14, .regular, line: 20, tracking: 0.12))
                    .foregroundStyle(Palette.storyNarration)
                Text(run.scenarioTitle).textStyle(VsmType.storyIntroTitle).foregroundStyle(.white).accessibilityAddTraits(.isHeader)
            }
            .multilineTextAlignment(.center)
            .padding(24)
            .opacity(text || reduce ? 1 : 0)
        }
        .opacity(fade ? 0 : 1)
        .contentShape(Rectangle())
        .onTapGesture(perform: skip)
        .accessibilityAction(named: "Пропустить заставку", skip)
        .task {
            guard !reduce else { return }
            withAnimation(.easeOut(duration: 0.6)) { text = true }
            try? await Task.sleep(for: .milliseconds(1800))
            withAnimation(.easeIn(duration: 0.6)) { fade = true }
        }
    }
}

/// Финал — `StoryEnd`: название финала, к чему привели решения, XP, шкалы, финалы, компетенции, награды, действия.
private struct StoryEnd: View {
    let run: RunState
    let endings: (reached: Int, total: Int)?
    let titles: [String: String]
    let map: () -> Void
    let debrief: () -> Void
    let restart: () -> Void
    let scenarios: () -> Void
    @State private var shown = false
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        if let final = run.final {
            let eyebrow = final.outcome == .failure ? colors.negative : (final.outcome == .partial ? colors.warning : colors.brandText)
            ZStack {
                Palette.storyBlack.opacity(shown || reduce ? 0.72 : 0).ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("СЦЕНАРИЙ ЗАВЕРШЁН").textStyle(VsmType.eyebrow).foregroundStyle(eyebrow)
                        Text(final.ending).textStyle(VsmType.storyEndTitle).foregroundStyle(colors.heading).accessibilityAddTraits(.isHeader)
                        Text(final.text).textStyle(TextStyle(16, .regular, line: 26)).foregroundStyle(colors.muted)
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                            EndStat(label: "Получено XP") { AnimatedNumber(value: final.xpEarned, style: VsmType.storyEndTitle.sized(24), color: colors.heading, prefix: "+") }
                            EndStat(label: "Лояльность пассажира") { Text("\(run.loyalty)%").textStyle(VsmType.storyEndTitle.sized(24)).foregroundStyle(colors.heading) }
                            EndStat(label: "Рейтинг безопасности") { Text("\(run.safety)%").textStyle(VsmType.storyEndTitle.sized(24)).foregroundStyle(colors.heading) }
                            if let endings {
                                EndStat(label: "Открыто финалов") { Text("\(endings.reached) из \(endings.total)").textStyle(VsmType.storyEndTitle.sized(24)).foregroundStyle(colors.heading) }
                            }
                        }
                        CompetenceList(points: final.competencePoints, titles: titles)
                        Rewards(achievements: run.newAchievements, levelUp: run.levelUp)
                        VStack(spacing: 10) {
                            PrimaryButton(title: "Посмотреть путь", fill: true, action: map)
                            GhostButton(title: "Разбор решений", fill: true, action: debrief).accessibilityIdentifier("final.debrief")
                            GhostButton(title: "Пройти заново", fill: true, action: restart)
                            GhostButton(title: "Вернуться к сценариям", fill: true, action: scenarios)
                        }
                        .padding(.top, 12)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 24)
                    .background(colors.surface, in: RoundedRectangle(cornerRadius: Radius.storyEnd, style: .continuous))
                    .shadow(color: .black.opacity(0.35), radius: 30, y: 12)
                    .opacity(shown || reduce ? 1 : 0)
                    .offset(y: shown || reduce ? 0 : 18)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 24)
                }
            }
            .onAppear {
                guard !reduce else { return }
                withAnimation(.site(0.6).delay(0.4)) { shown = true }
            }
        }
    }
}

private struct EndStat<Value: View>: View {
    let label: String
    @ViewBuilder var value: Value
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).textStyle(VsmType.caption).foregroundStyle(colors.muted)
            value
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(colors.soft, in: RoundedRectangle(cornerRadius: Radius.block, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// Журнал реплик `.story-history`: все прочитанные реплики, решения и истёкшие таймеры.
private struct HistoryPanel: View {
    let entries: [HistoryEntry]
    let close: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("История").textStyle(VsmType.bodyBold).foregroundStyle(colors.storyInk).accessibilityAddTraits(.isHeader)
                Spacer()
                Button(action: close) {
                    Text("Закрыть").textStyle(VsmType.bodyBold).foregroundStyle(colors.brand).frame(minHeight: Metrics.touchMin)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 8)
            Rectangle().fill(colors.border).frame(height: 1)
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 14) {
                        ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
                            entryView(entry).id(index)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    .padding(.bottom, 32)
                }
                .onAppear { proxy.scrollTo(entries.count - 1, anchor: .bottom) }
            }
        }
        .background(colors.storyGlass.ignoresSafeArea())
        .accessibilityAction(.escape, close)
    }

    @ViewBuilder
    private func entryView(_ entry: HistoryEntry) -> some View {
        let who = entry.kind == "choice" ? "Ваше решение" : entry.speaker
        let color: Color = switch entry.kind {
        case "narration": colors.muted
        case "timeout": colors.negative
        default: colors.storyInk.opacity(entry.kind == "thought" ? 0.75 : 1)
        }
        VStack(alignment: .leading, spacing: 2) {
            if let who { Text(who.uppercased()).textStyle(TextStyle(12, .heavy, line: 16, tracking: 0.06)).foregroundStyle(colors.brandText) }
            Text(entry.text)
                .textStyle(VsmType.body.weighted(entry.kind == "choice" || entry.kind == "timeout" ? .semibold : .regular))
                .italic(entry.kind == "thought")
                .foregroundStyle(color)
        }
        .padding(.horizontal, entry.kind == "choice" ? 14 : 0)
        .padding(.vertical, entry.kind == "choice" ? 10 : 0)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(entry.kind == "choice" ? colors.soft : .clear, in: RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}
