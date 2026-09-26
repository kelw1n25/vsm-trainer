import SwiftUI
import VSMCore

// MARK: - Поверхности

extension View {
    /// Тень сайта `--shadow: 0 8px 30px rgba(24,64,140,.06)` — мягкая, с оттенком бренда.
    func vsmShadow(radius: CGFloat = 15, y: CGFloat = 8) -> some View {
        modifier(ShadowModifier(radius: radius, y: y))
    }
}

private struct ShadowModifier: ViewModifier {
    let radius: CGFloat
    let y: CGFloat
    @Environment(\.vsm) private var colors

    func body(content: Content) -> some View {
        content.shadow(color: colors.shadow.opacity(colors.dark ? 0.35 : 0.08), radius: radius, y: y)
    }
}

/// Карточка `.card`: surface, рамка 1, радиус 20, тень, padding 24.
/// `accentLeading` — цветная полоса слева (разбор шага, рекомендация, новое уведомление), `accentTop` — сверху.
struct Card<Content: View>: View {
    var padding: CGFloat = Metrics.cardPadding
    var spacing: CGFloat = 12
    var background: AnyShapeStyle?
    var accentLeading: Color?
    var accentWidth: CGFloat = 6
    var accentTop: Color?
    @ViewBuilder var content: Content
    @Environment(\.vsm) private var colors

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
        VStack(alignment: .leading, spacing: spacing) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background ?? AnyShapeStyle(colors.surface))
            .overlay(alignment: .leading) {
                if let accentLeading { Rectangle().fill(accentLeading).frame(width: accentWidth) }
            }
            .overlay(alignment: .top) {
                if let accentTop { Rectangle().fill(accentTop).frame(height: 4) }
            }
            .clipShape(shape)
            .overlay(shape.strokeBorder(colors.border, lineWidth: 1))
            .vsmShadow()
    }
}

/// Вложенный блок `radius 16, background soft` — стандарт, шаг ролевой модели, итог финала.
struct SoftBlock<Content: View>: View {
    var color: Color?
    var spacing: CGFloat = 6
    @ViewBuilder var content: Content
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) { content }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color ?? colors.soft, in: RoundedRectangle(cornerRadius: Radius.block, style: .continuous))
    }
}

/// Блок награды `.reward` / ачивки `.achievement`: рамка, радиус 16, появление `reward-in` по очереди.
struct OutlinedBlock<Content: View>: View {
    let border: Color
    let background: Color
    var index = 0
    @ViewBuilder var content: Content

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.block, style: .continuous)
        VStack(alignment: .leading, spacing: 2) { content }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background, in: shape)
            .overlay(shape.strokeBorder(border, lineWidth: 1))
            .riseIn(index, step: 0.15, duration: 0.5)
    }
}

/// Фон страниц сайта: основной цвет и два мягких пятна света (`body::before` справа сверху, `body::after` слева снизу).
struct SiteBackground: View {
    @Environment(\.vsm) private var colors

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                colors.bg
                RadialGradient(colors: [Color(hex: 0x78AAFF, alpha: 0.2), .clear], center: .topTrailing, startRadius: 0, endRadius: proxy.size.width * 1.1)
                RadialGradient(colors: [Color(hex: 0x5AC8DC, alpha: 0.13), .clear], center: .bottomLeading, startRadius: 0, endRadius: proxy.size.width)
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: - Кнопки

/// Нажатие вместо наведения: кнопка чуть уменьшается, как `.button:active { transform: scale(.98) }`.
struct PressStyle: ButtonStyle {
    @Environment(\.vsmReduceMotion) private var reduce

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduce ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// Основная кнопка `.button`: градиент #2A78FF → #1561F0, радиус 12, 17/700, стрелка справа.
struct PrimaryButton: View {
    let title: String
    var large = false
    var arrow = false
    var enabled = true
    var busy = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if busy { ProgressView().tint(.white) }
                Text(title).textStyle(VsmType.button).foregroundStyle(.white)
                if arrow { Icon(kind: .arrowRight, size: 20, color: .white) }
            }
            .padding(.horizontal, large ? 34 : 30)
            .frame(minWidth: 120, minHeight: large ? Metrics.buttonLargeHeight : Metrics.buttonHeight)
            .background(
                LinearGradient(colors: [Palette.buttonTop, Palette.buttonBottom], startPoint: .top, endPoint: .bottom),
                in: RoundedRectangle(cornerRadius: Radius.button, style: .continuous)
            )
            .shadow(color: Palette.buttonTop.opacity(enabled ? 0.28 : 0), radius: 9, y: 6)
            .opacity(enabled ? 1 : 0.6)
        }
        .buttonStyle(PressStyle())
        .disabled(!enabled || busy)
    }
}

/// Вторичная кнопка `.button--ghost`: фон surface, текст brand-text, рамка control-border.
struct GhostButton: View {
    let title: String
    var large = false
    var enabled = true
    let action: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.button, style: .continuous)
        Button(action: action) {
            Text(title).textStyle(VsmType.button).foregroundStyle(colors.brandText)
                .padding(.horizontal, large ? 34 : 30)
                .frame(minHeight: large ? Metrics.buttonLargeHeight : Metrics.buttonHeight)
                .background(colors.surface, in: shape)
                .overlay(shape.strokeBorder(colors.controlBorder, lineWidth: 1))
                .opacity(enabled ? 1 : 0.6)
        }
        .buttonStyle(PressStyle())
        .disabled(!enabled)
    }
}

/// Ссылка-действие `.link-action` / `.back-link`: brand-text 17/600 с иконками.
struct LinkAction: View {
    let title: String
    var leading: IconKind?
    var trailing: IconKind?
    var small = false
    let action: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let leading { Icon(kind: leading, size: small ? 18 : 20, color: colors.brandText) }
                Text(title).textStyle(small ? VsmType.link.sized(15) : VsmType.link).foregroundStyle(colors.brandText)
                if let trailing { Icon(kind: trailing, size: 18, color: colors.brandText) }
            }
            .padding(.vertical, 8)
            .frame(minHeight: Metrics.touchMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
    }
}

/// «← Все сценарии»: `.back-link`.
struct BackLink: View {
    let title: String
    let action: () -> Void

    var body: some View {
        LinkAction(title: title, leading: .chevronLeft, small: true, action: action)
    }
}

// MARK: - Теги, чипы, вкладки

/// Тег `.tag` (высота 32, пилюля) и `.tag--category` (синий).
struct Tag: View {
    let text: String
    var category = false
    @Environment(\.vsm) private var colors

    var body: some View {
        Text(text)
            .textStyle(category ? VsmType.tagStrong : VsmType.tag)
            .foregroundStyle(category ? colors.brandText : colors.tagText)
            .padding(.horizontal, 14)
            .frame(height: 32)
            .background(category ? colors.softStrong : colors.tagBg, in: Capsule())
    }
}

/// Чип фильтра `.chip`: 38, рамка control-border, выбранный — фон brand и белый текст.
struct Chip: View {
    let title: String
    let selected: Bool
    let action: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        Button(action: action) {
            Text(title)
                .textStyle(VsmType.bodyStrong)
                .foregroundStyle(selected ? .white : colors.text)
                .padding(.horizontal, 18)
                .frame(height: 38)
                .background(selected ? colors.brand : colors.surface, in: Capsule())
                .overlay(Capsule().strokeBorder(selected ? colors.brand : colors.controlBorder, lineWidth: 1))
                .frame(minHeight: Metrics.touchMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .animation(.easeOut(duration: 0.2), value: selected)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// Счётчик `.count-badge` рядом с заголовком секции.
struct CountBadge: View {
    let count: Int
    @Environment(\.vsm) private var colors

    var body: some View {
        Text("\(count)")
            .textStyle(VsmType.tagStrong.weighted(.bold))
            .foregroundStyle(colors.brandText)
            .padding(.horizontal, 8)
            .frame(minWidth: 28, minHeight: 28)
            .background(colors.softStrong, in: Capsule())
    }
}

/// Бейдж исхода `.outcome--success|partial|failure`.
struct OutcomeBadge: View {
    let status: RunStatus
    @Environment(\.vsm) private var colors

    var body: some View {
        let (background, text): (Color, Color) = switch status {
        case .success: (colors.successBg, colors.positive)
        case .partial: (colors.partialBg, colors.warning)
        case .failure: (colors.failureBg, colors.negative)
        case .inProgress: (colors.tagBg, colors.text)
        }
        Text(Labels.outcome(status))
            .textStyle(VsmType.smallStrong)
            .foregroundStyle(text)
            .padding(.horizontal, 12)
            .padding(.vertical, 3)
            .background(background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Вкладки `.tabs`: белая плашка с рамкой и тенью, активная — фон brand.
struct Tabs<Key: Hashable>: View {
    let options: [(Key, String)]
    let selected: Key
    let select: (Key) -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.tabs, style: .continuous)
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(options, id: \.0) { key, title in
                    let active = key == selected
                    Button { select(key) } label: {
                        Text(title)
                            .textStyle(VsmType.bodyStrong)
                            .foregroundStyle(active ? .white : colors.text)
                            .padding(.horizontal, 18)
                            .frame(height: 40)
                            .background(active ? colors.brand : .clear, in: RoundedRectangle(cornerRadius: Radius.tab, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(active ? .isSelected : [])
                }
            }
            .padding(4)
            .background(colors.surface, in: shape)
            .overlay(shape.strokeBorder(colors.border, lineWidth: 1))
            .vsmShadow(radius: 8, y: 4)
            .padding(.vertical, 10)
            .padding(.horizontal, 2)
            .animation(.easeOut(duration: 0.2), value: selected)
        }
        // Тень плашки не обрезается краем прокрутки
        .scrollClipDisabled()
        .padding(.vertical, -10)
    }
}

// MARK: - Прогресс

/// Полоса `.progress`: 10, дорожка track, заливка — градиент #4F8DFF → brand, растёт при появлении (0.9 с).
struct GradientProgress: View {
    let fraction: Double
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce
    @State private var shown: Double = 0

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(colors.track)
                Capsule()
                    .fill(LinearGradient(colors: [Palette.progressStart, colors.brand], startPoint: .leading, endPoint: .trailing))
                    .frame(width: proxy.size.width * min(1, max(0, shown)))
            }
        }
        .frame(height: 10)
        .onAppear { animate(to: fraction) }
        .onChange(of: fraction) { _, value in animate(to: value) }
    }

    private func animate(to value: Double) {
        if reduce { shown = value } else { withAnimation(.site(0.9)) { shown = value } }
    }
}

/// `LevelProgress` сайта: «Уровень N · Название», полоса, «XP · до следующего уровня …».
struct LevelProgress: View {
    let level: Level
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Уровень \(level.level) · \(level.title)").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
            GradientProgress(fraction: level.progress)
                .accessibilityLabel("Прогресс уровня \(Int(level.progress * 100))%")
            let tail = level.nextLevelXp.map { " · до следующего уровня \($0 - level.xp) XP" } ?? " · максимальный уровень"
            Text("\(level.xp) XP\(tail)").textStyle(VsmType.body).foregroundStyle(colors.muted)
        }
    }
}

/// «Сложность: ● ● ○» — `.difficulty`.
struct DifficultyDots: View {
    let level: Int
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(spacing: 8) {
            Text("Сложность:").textStyle(VsmType.route).foregroundStyle(colors.muted).padding(.trailing, 4)
            ForEach(0..<3, id: \.self) { index in
                Circle().fill(index < level ? Palette.difficultyOn : colors.dotOff).frame(width: 12, height: 12)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Сложность \(level) из 3")
    }
}

// MARK: - Тексты

/// `.page-title`.
struct PageTitle: View {
    let text: String
    @Environment(\.vsm) private var colors

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text).textStyle(VsmType.pageTitle).foregroundStyle(colors.heading).accessibilityAddTraits(.isHeader)
    }
}

/// `.section__title` со счётчиком `.count-badge`.
struct SectionTitle: View {
    let text: String
    var count: Int?
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(spacing: 14) {
            Text(text).textStyle(VsmType.sectionTitle).foregroundStyle(colors.heading).accessibilityAddTraits(.isHeader)
            if let count { CountBadge(count: count) }
        }
    }
}

/// `h2` внутри карточки.
struct CardTitle: View {
    let text: String
    @Environment(\.vsm) private var colors

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text).textStyle(VsmType.h2).foregroundStyle(colors.heading).padding(.bottom, 2).accessibilityAddTraits(.isHeader)
    }
}

struct Muted: View {
    let text: String
    var small = false
    @Environment(\.vsm) private var colors

    init(_ text: String, small: Bool = false) {
        self.text = text
        self.small = small
    }

    var body: some View {
        Text(text).textStyle(small ? VsmType.small : VsmType.body).foregroundStyle(colors.muted)
    }
}

/// Маршрут с меткой `.route`: «📍 Москва — Санкт-Петербург · Бизнес».
struct RouteRow: View {
    let route: String
    let serviceClass: String
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(spacing: 8) {
            Icon(kind: .pin, size: 16, color: colors.muted)
            Text("\(route) · \(serviceClass)").textStyle(VsmType.route).foregroundStyle(colors.muted)
        }
        .accessibilityElement(children: .combine)
    }
}

/// Пункт списка `<li>` с висячим отступом: перенос строки встаёт под текст, а не под маркер.
struct Bullet: View {
    let text: String
    var color: Color?
    var style = VsmType.body
    @Environment(\.vsm) private var colors

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 0) {
            Text("•").textStyle(style).padding(.leading, 4).padding(.trailing, 10).accessibilityHidden(true)
            Text(text).textStyle(style).frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(color ?? colors.text)
    }
}

/// `CompetenceList`: изменения очков по компетенциям, нулевые не показываются.
struct CompetenceList: View {
    let points: [String: Int]
    let titles: [String: String]
    @Environment(\.vsm) private var colors

    var body: some View {
        let entries = points.filter { $0.value != 0 }.sorted { $0.key < $1.key }
        if !entries.isEmpty {
            VStack(alignment: .leading, spacing: 2) {
                ForEach(entries, id: \.key) { code, value in
                    Bullet(text: "\(titles[code] ?? code): \(Labels.signed(value))", color: value < 0 ? colors.negative : colors.positive)
                }
            }
        }
    }
}

/// `Rewards`: новый уровень и ачивки, полученные только что.
struct Rewards: View {
    let achievements: [Achievement]
    let levelUp: Level?
    @Environment(\.vsm) private var colors

    var body: some View {
        if !achievements.isEmpty || levelUp != nil {
            VStack(spacing: 10) {
                if let levelUp {
                    OutlinedBlock(border: colors.brandBorder, background: colors.soft) {
                        Text("Новый уровень \(levelUp.level): \(levelUp.title)").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                    }
                }
                ForEach(Array(achievements.enumerated()), id: \.element.code) { index, achievement in
                    OutlinedBlock(border: colors.successBorder, background: colors.surface, index: index + 1) {
                        Text("🏅 \(achievement.title)").textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                        Text(achievement.description).textStyle(VsmType.body).foregroundStyle(colors.muted)
                    }
                }
            }
        }
    }
}

/// Подпись и значение, как `<dt>/<dd>` сайта.
struct DetailRow: View {
    let label: String
    let value: String
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).textStyle(VsmType.body).foregroundStyle(colors.muted)
            Text(value).textStyle(VsmType.bodyStrong).foregroundStyle(colors.text)
        }
        .accessibilityElement(children: .combine)
    }
}

/// Подвал сайта: «ВСМ · Геймификация обучения проводников / Демо-версия · все данные синтетические».
struct SiteFooter: View {
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Rectangle().fill(colors.border).frame(height: 1).padding(.bottom, 14)
            Text("ВСМ · Геймификация обучения проводников").textStyle(VsmType.small).foregroundStyle(colors.text)
            Text("Демо-версия · все данные синтетические").textStyle(VsmType.small).foregroundStyle(colors.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 8)
    }
}

// MARK: - Поля ввода

/// Рамка поля `input` сайта: 48, радиус 12, рамка 1.5 control-border, фокус — рамка brand и кольцо.
struct InputFrame: ViewModifier {
    let focused: Bool
    @Environment(\.vsm) private var colors

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: Radius.button, style: .continuous)
        content
            .textFieldStyle(.plain)
            .textStyle(VsmType.body)
            .foregroundStyle(colors.text)
            .padding(.horizontal, 14)
            .frame(height: Metrics.inputHeight)
            .background(colors.inputBg, in: shape)
            .overlay(shape.strokeBorder(focused ? colors.brand : colors.controlBorder, lineWidth: 1.5))
            .background(shape.stroke(colors.brand.opacity(focused ? 0.3 : 0), lineWidth: 6))
            .animation(.easeOut(duration: 0.15), value: focused)
    }
}

/// Подпись поля `label` сайта над полем.
struct FieldLabel: View {
    let text: String
    @Environment(\.vsm) private var colors

    var body: some View {
        Text(text).textStyle(VsmType.tagStrong).foregroundStyle(colors.text).accessibilityHidden(true)
    }
}

/// Переключатель `.switch`: дорожка 46×26, бегунок 20, включён — brand; подпись справа.
struct VsmSwitch: View {
    let title: String
    let hint: String
    let isOn: Bool
    let change: (Bool) -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        Button { change(!isOn) } label: {
            HStack(spacing: 14) {
                ZStack(alignment: isOn ? .trailing : .leading) {
                    Capsule().fill(isOn ? colors.brand : colors.trackStrong).frame(width: 46, height: 26)
                    Circle().fill(.white).frame(width: 20, height: 20).shadow(color: .black.opacity(0.2), radius: 1.5, y: 1).padding(3)
                }
                .frame(width: 46, height: 26)
                .animation(.easeOut(duration: 0.2), value: isOn)
                VStack(alignment: .leading, spacing: 0) {
                    Text(title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text)
                    Text("— \(hint)").textStyle(VsmType.body).foregroundStyle(colors.muted)
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(title)
        .accessibilityValue(isOn ? "включено" : "выключено")
        .accessibilityHint(hint)
        .accessibilityAddTraits(.isButton)
    }
}

// MARK: - Аватары

/// Аватар проводника — та же картинка, что на сайте (`Avatar`), с белым кольцом и тенью.
struct UserAvatar: View {
    let size: CGFloat
    var ring: CGFloat = 3
    @Environment(\.vsm) private var colors

    var body: some View {
        AssetImage(name: "avatar", contentMode: .fill)
            .frame(width: size, height: size)
            .background(colors.surface)
            .clipShape(Circle())
            .overlay(Circle().strokeBorder(colors.surface, lineWidth: ring))
            .vsmShadow(radius: 6, y: 3)
            .accessibilityHidden(true)
    }
}

/// `InitialsAvatar`: инициалы «ИФ» на градиенте soft-strong → brand-border.
struct InitialsAvatar: View {
    let fullName: String
    let size: CGFloat
    @Environment(\.vsm) private var colors

    var body: some View {
        let parts = fullName.split(separator: " ")
        let initials = (parts.count > 1 ? String(parts[1].prefix(1)) : "") + (parts.first.map { String($0.prefix(1)) } ?? "")
        Text(initials)
            .textStyle(TextStyle(size * 0.38, .heavy))
            .foregroundStyle(colors.brandText)
            .frame(width: size, height: size)
            .background(LinearGradient(colors: [colors.softStrong, colors.brandBorder], startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle())
            .accessibilityHidden(true)
    }
}

// MARK: - Движение

private struct RiseIn: ViewModifier {
    let index: Int
    let step: Double
    let duration: Double
    @Environment(\.vsmReduceMotion) private var reduce
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown || reduce ? 1 : 0)
            .offset(y: shown || reduce ? 0 : 18)
            .onAppear {
                guard !reduce else { return }
                withAnimation(.site(duration).delay(Double(index) * step)) { shown = true }
            }
    }
}

extension View {
    /// Появление блока `rise-in` сайта: снизу на 18 и из прозрачности, по очереди с шагом `step`.
    /// «Уменьшить анимацию» показывает блок сразу.
    func riseIn(_ index: Int = 0, step: Double = 0.07, duration: Double = 0.55) -> some View {
        modifier(RiseIn(index: index, step: step, duration: duration))
    }
}

/// `AnimatedNumber`: число досчитывает до значения за 0.9 с (ease-out cubic).
struct AnimatedNumber: View {
    let value: Int
    let style: TextStyle
    let color: Color
    var prefix = ""
    var suffix = ""
    @Environment(\.vsmReduceMotion) private var reduce
    @State private var start = Date()

    var body: some View {
        TimelineView(.animation(paused: reduce)) { timeline in
            let t = reduce ? 1 : min(1, timeline.date.timeIntervalSince(start) / 0.9)
            let eased = 1 - pow(1 - t, 3)
            Text("\(prefix)\(Int(Double(value) * eased))\(suffix)").textStyle(style).foregroundStyle(color)
        }
        .onChange(of: value) { _, _ in start = Date() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(prefix)\(value)\(suffix)")
    }
}

// MARK: - Состояния экрана и страница

enum SiteDate {
    private static let dateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        return formatter
    }()

    private static let date: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter
    }()

    /// Формат дат сайта (`toLocaleString("ru-RU")`).
    static func dateTime(_ value: Date) -> String { dateTime.string(from: value) }
    static func day(_ value: Date) -> String { date.string(from: value) }
}

/// Плашка `.notice`: фон partial-bg, рамка notice-border, радиус 16.
struct Notice: View {
    let text: String
    @Environment(\.vsm) private var colors

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.block, style: .continuous)
        Text(text)
            .textStyle(VsmType.body)
            .foregroundStyle(colors.text)
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.partialBg, in: shape)
            .overlay(shape.strokeBorder(colors.noticeBorder, lineWidth: 1))
    }
}

/// Состояния экрана как на сайте: «Загрузка…» приглушённым текстом, ошибка — красным с кнопкой повтора,
/// данные без сети — плашка `.notice` с датой.
struct ScreenContent<Value, Content: View>: View {
    let state: ScreenState<Value>
    let retry: () async -> Void
    @ViewBuilder let content: (Value) -> Content
    @Environment(\.vsm) private var colors

    var body: some View {
        switch state {
        case .loading:
            Text("Загрузка…").textStyle(VsmType.body).foregroundStyle(colors.muted)
        case let .failed(message):
            VStack(alignment: .leading, spacing: 12) {
                Text(message).textStyle(VsmType.body).foregroundStyle(colors.negative)
                GhostButton(title: "Повторить") { Task { await retry() } }
            }
        case let .loaded(value, staleSince):
            VStack(alignment: .leading, spacing: Metrics.stack) {
                if let staleSince { Notice(text: "Нет связи с сервером. Показаны данные от \(SiteDate.dateTime(staleSince))") }
                content(value)
            }
        }
    }
}

/// Страница сайта (`.stack`): поле 16, блоки через 24, подвал в конце. Список ленивый —
/// длинные разделы (справочник, рейтинг) не рисуются целиком заранее.
struct Page<Content: View>: View {
    var footer = true
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Metrics.stack) {
                content
                if footer { SiteFooter() }
            }
            .padding(.horizontal, Metrics.gutter)
            .padding(.top, 4)
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
    }
}
