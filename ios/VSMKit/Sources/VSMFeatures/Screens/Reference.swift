import SwiftUI
import VSMCore

// MARK: - Справочник

/// Слайды кейсодержателя о подвижном составе и классах обслуживания ВСМ.
private let slides = [
    ("photo_service_classes", "Классы обслуживания в поездах ВСМ"),
    ("photo_service_classes_layout", "Компоновка вагонов по классам"),
    ("photo_rolling_stock", "Планировка вагонов комфорт и стандарт, предлагаемые сервисы"),
]

private struct Slide: Identifiable {
    let name: String
    let caption: String
    var id: String { name }
}

/// Справочник — `HandbookPage`: ролевая модель, классы обслуживания и слайды, стандарты, 51 ситуация с поиском.
struct HandbookScreen: View {
    @State private var model: HandbookViewModel
    @State private var slide: Slide?
    @FocusState private var searching: Bool
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer) {
        _model = State(initialValue: HandbookViewModel(repository: container.trainer))
    }

    var body: some View {
        ScrollViewReader { proxy in
            Page {
                PageTitle("Справочник проводника")
                Muted("Материалы кейсодержателя: «Примеры ситуаций взаимодействия поездного персонала с пассажирами», стандарты СТО РЖД 03.011, 03.013 и 03.014, слайды о подвижном составе ВСМ. На эти материалы опираются все сценарии.")
                if let content = model.state.value {
                    sections(content)
                } else {
                    ScreenContent(state: model.state, retry: model.load) { _ in EmptyView() }
                }
            }
            .task {
                await model.load()
                // Ссылка «Ситуация N» со страницы сценария — прокрутить к ней, когда справочник загрузится
                if let focus = navigator.handbookFocus {
                    try? await Task.sleep(for: .milliseconds(150))
                    withAnimation { proxy.scrollTo(focus, anchor: .top) }
                }
            }
        }
        .onDisappear { navigator.handbookFocus = nil }
        .fullScreenCoverCompat(item: $slide) { SlideViewer(slide: $0) { slide = nil } }
    }

    @ViewBuilder
    private func sections(_ content: HandbookViewModel.Content) -> some View {
        let handbook = content.handbook
        Card {
            CardTitle(handbook.roleModel.title)
            ForEach(Array(handbook.roleModel.steps.enumerated()), id: \.element.id) { index, step in
                SoftBlock {
                    (Text("\(index + 1). ").foregroundColor(colors.brandText) + Text(step.title))
                        .textStyle(VsmType.bodyBold)
                        .foregroundStyle(colors.text)
                    ForEach(step.phrases, id: \.self) { Bullet(text: $0, color: colors.muted) }
                }
            }
        }
        Card {
            CardTitle("Классы обслуживания")
            ForEach(handbook.serviceClasses) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title).textStyle(VsmType.h3.sized(17.7)).foregroundStyle(colors.heading).padding(.bottom, 4)
                    spec("Компоновка", item.layout)
                    spec("Шаг кресел", "\(item.pitchMm) мм")
                    spec("Ширина кресла", "\(item.seatMm) мм")
                    spec("Проход", "\(item.aisleMm) мм")
                    spec("Ожидание", "до \(item.maxWaitMinutes) мин")
                    Muted(item.summary).padding(.top, 4)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(RoundedRectangle(cornerRadius: Radius.block, style: .continuous).strokeBorder(colors.border, lineWidth: 1))
            }
            ForEach(slides, id: \.0) { name, caption in
                VStack(alignment: .leading, spacing: 6) {
                    Button { slide = Slide(name: name, caption: caption) } label: {
                        AssetImage(name: name)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.image, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: Radius.image, style: .continuous).strokeBorder(colors.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(caption), открыть на весь экран")
                    Text(caption).textStyle(VsmType.caption).foregroundStyle(colors.muted)
                }
            }
        }
        Card {
            CardTitle("Стандарты обслуживания")
            ForEach(handbook.standards) { standard in
                VStack(alignment: .leading, spacing: 4) {
                    Text(standard.title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                    Muted(standard.text)
                }
            }
        }
        VStack(alignment: .leading, spacing: 12) {
            CardTitle("Ситуации на борту и на посадке · \(handbook.situations.count)")
            TextField("", text: $model.query, prompt: Text("Поиск: билет, питомец, аллергия…").foregroundStyle(colors.muted))
                .focused($searching)
                .submitLabel(.search)
                .modifier(InputFrame(focused: searching))
                .accessibilityLabel("Поиск по ситуациям")
            Flow(spacing: 10, lineSpacing: 10) {
                Chip(title: "Все", selected: model.category == nil) { model.category = nil }
                ForEach(model.categories, id: \.self) { code in
                    Chip(title: Labels.category(code), selected: model.category == code) { model.category = code }
                }
            }
            if model.situations.isEmpty { Muted("Ничего не найдено.") }
        }
        ForEach(model.situations) { situation in
            SituationBlock(situation: situation, trainedIn: content.trainedIn[situation.number] ?? [], focused: situation.number == navigator.handbookFocus)
                .id(situation.number)
        }
    }

    private func spec(_ label: String, _ value: String) -> some View {
        HStack(spacing: 12) {
            Text(label).textStyle(VsmType.body).foregroundStyle(colors.muted)
            Text(value).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct SituationBlock: View {
    let situation: Situation
    let trainedIn: [ScenarioSummary]
    let focused: Bool
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    var body: some View {
        SoftBlock {
            Flow {
                Tag(text: Labels.category(situation.category), category: true)
                Tag(text: Labels.stage(situation.stage))
            }
            Text("\(situation.number). \(situation.title)").textStyle(VsmType.h3).foregroundStyle(colors.heading).accessibilityAddTraits(.isHeader)
            (Text("Реакция. ").bold() + Text(situation.reaction)).textStyle(VsmType.body).foregroundStyle(colors.text)
            ForEach(situation.phrases, id: \.self) { Bullet(text: $0, color: colors.brandText) }
            ForEach(situation.comment, id: \.self) { Bullet(text: $0, color: colors.muted) }
            if !trainedIn.isEmpty {
                Flow(spacing: 4, lineSpacing: 4) {
                    Text("Отрабатывается:").textStyle(VsmType.small).foregroundStyle(colors.text)
                    ForEach(Array(trainedIn.enumerated()), id: \.element.id) { index, scenario in
                        Button { navigator.open(.scenario(scenario.id)) } label: {
                            Text(scenario.title + (index < trainedIn.count - 1 ? "," : "")).textStyle(VsmType.small).underline().foregroundStyle(colors.brandText)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.top, 6)
            }
        }
        .overlay(RoundedRectangle(cornerRadius: Radius.block, style: .continuous).strokeBorder(focused ? colors.brand : .clear, lineWidth: 2))
    }
}

/// Слайд на весь экран с увеличением двумя пальцами (на сайте — ссылка на файл).
private struct SlideViewer: View {
    let slide: Slide
    let close: () -> Void
    @State private var scale: CGFloat = 1
    @GestureState private var pinch: CGFloat = 1

    var body: some View {
        ZStack(alignment: .bottom) {
            Palette.storyBlack.ignoresSafeArea()
            AssetImage(name: slide.name)
                .scaleEffect(min(max(scale * pinch, 1), 4))
                .gesture(MagnificationGesture().updating($pinch) { value, state, _ in state = value }.onEnded { scale = min(max(scale * $0, 1), 4) })
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            Text(slide.caption).textStyle(VsmType.small).foregroundStyle(.white).padding(24)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: close)
        .accessibilityAction(.escape, close)
        .accessibilityLabel(slide.caption)
        .accessibilityHint("Коснитесь, чтобы закрыть")
    }
}

// MARK: - Уведомления

/// Уведомления — `NotificationsPage`: карточки, новые — с синей полосой слева.
struct NotificationsScreen: View {
    @State private var model: NotificationsViewModel
    let changed: () -> Void
    @Environment(\.vsm) private var colors

    init(container: AppContainer, changed: @escaping () -> Void) {
        _model = State(initialValue: NotificationsViewModel(repository: container.trainer))
        self.changed = changed
    }

    var body: some View {
        Page {
            VStack(alignment: .leading, spacing: 12) {
                PageTitle("Уведомления")
                if let list = model.state.value, list.unread > 0 {
                    GhostButton(title: "Отметить все прочитанными") {
                        Task {
                            await model.markAllRead()
                            changed()
                        }
                    }
                }
            }
            if let list = model.state.value {
                if list.items.isEmpty { Muted("Уведомлений пока нет.") }
                ForEach(Array(list.items.enumerated()), id: \.element.id) { index, item in
                    Button {
                        Task {
                            await model.open(item)
                            changed()
                        }
                    } label: {
                        Card(padding: 20, spacing: 4, accentLeading: item.read ? nil : colors.brand, accentWidth: 5) {
                            Text(item.title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                            Text(item.body).textStyle(VsmType.body).foregroundStyle(colors.text)
                            Muted(SiteDate.dateTime(item.createdAt))
                        }
                    }
                    .buttonStyle(PressStyle())
                    .accessibilityElement(children: .combine)
                    .accessibilityHint(item.read ? "" : "Новое")
                    .riseIn(index, step: 0.04, duration: 0.4)
                }
            } else {
                ScreenContent(state: model.state, retry: model.load) { _ in EmptyView() }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}

// MARK: - Настройки

/// Настройки — `SettingsPage`: учётная запись, тёмная тема и «Уменьшить анимацию», выход.
struct SettingsScreen: View {
    let container: AppContainer
    @State private var model: SettingsViewModel

    init(container: AppContainer) {
        self.container = container
        _model = State(initialValue: SettingsViewModel(repository: container.trainer))
    }

    var body: some View {
        let preferences = container.preferences
        Page {
            PageTitle("Настройки")
            Card {
                CardTitle("Учётная запись")
                VStack(alignment: .leading, spacing: 10) {
                    DetailRow(label: "Сотрудник", value: model.profile?.fullName ?? "—")
                    DetailRow(label: "Табельный номер", value: model.profile?.personnelNumber ?? "—")
                    DetailRow(label: "Роль", value: model.profile.map { Labels.role($0.role) } ?? "—")
                    DetailRow(label: "Бригада и депо", value: model.profile.map { "\($0.brigade) · \($0.depot)" } ?? "—")
                }
                Muted("Данные учётной записи ведутся в HR-системе и обновляются через интеграцию.")
            }
            Card(spacing: 16) {
                CardTitle("Интерфейс")
                VsmSwitch(title: "Тёмная тема", hint: "то же, что ползунок в шапке", isOn: preferences.theme == .dark) {
                    preferences.setTheme($0 ? .dark : .light)
                }
                VsmSwitch(title: "Уменьшить анимацию", hint: "отключает переходы и эффекты появления", isOn: preferences.reduceMotion) {
                    preferences.setReduceMotion($0)
                }
            }
            GhostButton(title: "Выйти из системы", fill: true) { Task { await container.session.signOut() } }
        }
        .task { await model.load() }
    }
}

// MARK: - Команда

/// «Проводники депо» — `TeamPage`: таблица сайта на телефоне — карточки с теми же колонками.
struct TeamScreen: View {
    @State private var model: TeamViewModel
    @Environment(Navigator.self) private var navigator
    @Environment(\.vsm) private var colors

    init(container: AppContainer) {
        _model = State(initialValue: TeamViewModel(repository: container.trainer))
    }

    var body: some View {
        Page {
            PageTitle("Проводники депо")
            if let team = model.state.value {
                ForEach(Array(team.enumerated()), id: \.element.id) { index, member in
                    Button { navigator.open(.member(member.employeeId)) } label: {
                        Card(spacing: 4) {
                            Text(member.fullName).textStyle(VsmType.bodyBold).underline().foregroundStyle(colors.brandText)
                            cell("Бригада", member.brigade)
                            cell("Уровень", member.levelTitle)
                            cell("XP", "\(member.xp)")
                            cell("Последняя активность", member.lastActivityAt.map(SiteDate.day) ?? "—")
                            cell("Проседает", member.weakestCompetence ?? "—")
                        }
                    }
                    .buttonStyle(PressStyle())
                    .accessibilityElement(children: .combine)
                    .accessibilityHint("Открыть аналитику")
                    .riseIn(index, step: 0.04, duration: 0.4)
                }
            } else {
                ScreenContent(state: model.state, retry: model.load) { _ in EmptyView() }
            }
        }
        .task { await model.load() }
    }

    private func cell(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            // Значению — больше места: длинные названия компетенций не рвутся посреди слова
            Text(label).textStyle(VsmType.small).foregroundStyle(colors.muted).frame(width: 118, alignment: .leading)
            Text(value).textStyle(VsmType.smallStrong).foregroundStyle(colors.text).frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.top, 2)
    }
}
