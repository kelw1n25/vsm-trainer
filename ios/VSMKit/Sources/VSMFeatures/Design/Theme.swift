import CoreText
import SwiftUI
import VSMCore

extension Color {
    /// Цвет из записи сайта: `Color(hex: 0x146BFF)`, прозрачность — отдельно.
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

/// Цветовые токены сайта (`frontend/src/styles.css`, `:root` и `html[data-theme="dark"]`).
/// Имена совпадают с CSS-переменными: `--soft-strong` → softStrong. Новых цветов нет.
struct VsmColors {
    let bg, surface, soft, softStrong, text, heading, muted, border: Color
    let brand, brandText, positive, negative, warning, loyalty, safety: Color
    let track, trackStrong, navText, brandName, divider, caption: Color
    let heroBg: [Color]
    let heroBorder, heroGlow, heroEyebrow, heroText: Color
    let glass, glassSoft, glassText, surfaceMuted, tagBg, tagText, dotOff, controlBorder, inputBg, brandBorder: Color
    let successBg, successBorder, partialBg, noticeBorder, failureBg: Color
    /// Тень карточек `--shadow`: синеватая в светлой теме, чёрная в тёмной.
    let shadow: Color
    /// Стекло новеллы `--story-glass`.
    let storyGlass, storyGlassBorder, storyInk: Color
    let dark: Bool

    static let light = VsmColors(
        bg: Color(hex: 0xF7FAFE), surface: Color(hex: 0xFFFFFF), soft: Color(hex: 0xEEF6FF), softStrong: Color(hex: 0xE4EEFF),
        text: Color(hex: 0x121C38), heading: Color(hex: 0x0F1A38), muted: Color(hex: 0x6D7891), border: Color(hex: 0xE8EEF6),
        brand: Color(hex: 0x146BFF), brandText: Color(hex: 0x1E5FD6), positive: Color(hex: 0x16915A), negative: Color(hex: 0xD6363E),
        warning: Color(hex: 0xC26A00), loyalty: Color(hex: 0x2F6FDD), safety: Color(hex: 0x169C7A),
        track: Color(hex: 0xE6ECF5), trackStrong: Color(hex: 0xD5DFED), navText: Color(hex: 0x1B2645), brandName: Color(hex: 0x16223F),
        divider: Color(hex: 0xD7DFEA), caption: Color(hex: 0x6B7890),
        heroBg: [Color(hex: 0xEEF5FF), Color(hex: 0xE7F1FE), Color(hex: 0xDCE9FA)],
        heroBorder: Color(hex: 0xE3EDF9), heroGlow: Color(hex: 0xFFFFFF, alpha: 0.9), heroEyebrow: Color(hex: 0x4F86E8), heroText: Color(hex: 0x5B6680),
        glass: Color(hex: 0xFFFFFF, alpha: 0.88), glassSoft: Color(hex: 0xFFFFFF, alpha: 0.72), glassText: Color(hex: 0x243152),
        surfaceMuted: Color(hex: 0xF7FAFF), tagBg: Color(hex: 0xF0F3F7), tagText: Color(hex: 0x6C7890), dotOff: Color(hex: 0xC9D3E1),
        controlBorder: Color(hex: 0xD8E2F1), inputBg: Color(hex: 0xFBFDFF), brandBorder: Color(hex: 0xC4D8FF),
        successBg: Color(hex: 0xEAF8F0), successBorder: Color(hex: 0xA6DCBF), partialBg: Color(hex: 0xFFF4E4),
        noticeBorder: Color(hex: 0xFFE0B5), failureBg: Color(hex: 0xFDE8E9),
        shadow: Color(hex: 0x18408C),
        storyGlass: Color(hex: 0xFFFFFF, alpha: 0.9), storyGlassBorder: Color(hex: 0xFFFFFF, alpha: 0.7), storyInk: Color(hex: 0x0F1A38),
        dark: false
    )

    static let dark = VsmColors(
        bg: Color(hex: 0x0B1222), surface: Color(hex: 0x131C30), soft: Color(hex: 0x18233B), softStrong: Color(hex: 0x1D2A47),
        text: Color(hex: 0xD6DFEE), heading: Color(hex: 0xF2F6FC), muted: Color(hex: 0x8E9AB3), border: Color(hex: 0x243049),
        brand: Color(hex: 0x3D86FF), brandText: Color(hex: 0x82B2FF), positive: Color(hex: 0x3FCF8E), negative: Color(hex: 0xFF6B72),
        warning: Color(hex: 0xF0A040), loyalty: Color(hex: 0x6FA0FF), safety: Color(hex: 0x3FCFA8),
        track: Color(hex: 0x243049), trackStrong: Color(hex: 0x2C3854), navText: Color(hex: 0xC9D4E8), brandName: Color(hex: 0xF2F6FC),
        divider: Color(hex: 0x2C3854), caption: Color(hex: 0x8E9AB3),
        heroBg: [Color(hex: 0x131F3A), Color(hex: 0x162543), Color(hex: 0x1A2C52)],
        heroBorder: Color(hex: 0x22314F), heroGlow: Color(hex: 0x5A8CFF, alpha: 0.14), heroEyebrow: Color(hex: 0x82B2FF), heroText: Color(hex: 0xA9B6CD),
        glass: Color(hex: 0x131C30, alpha: 0.9), glassSoft: Color(hex: 0x131C30, alpha: 0.75), glassText: Color(hex: 0xD6DFEE),
        surfaceMuted: Color(hex: 0x162036), tagBg: Color(hex: 0x1D2740), tagText: Color(hex: 0x9AA7C0), dotOff: Color(hex: 0x33405E),
        controlBorder: Color(hex: 0x2C3854), inputBg: Color(hex: 0x0F1729), brandBorder: Color(hex: 0x2F4F8F),
        successBg: Color(hex: 0x10291F), successBorder: Color(hex: 0x22603F), partialBg: Color(hex: 0x2B2112),
        noticeBorder: Color(hex: 0x5A4218), failureBg: Color(hex: 0x321419),
        shadow: Color(hex: 0x000000),
        storyGlass: Color(hex: 0x0F1729, alpha: 0.9), storyGlassBorder: Color(hex: 0x7896D2, alpha: 0.25), storyInk: Color(hex: 0xE6ECF7),
        dark: true
    )

    var heroGradient: LinearGradient {
        LinearGradient(colors: heroBg, startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

/// Фиксированные цвета сайта, не зависящие от темы.
enum Palette {
    static let buttonTop = Color(hex: 0x2A78FF)
    static let buttonBottom = Color(hex: 0x1561F0)
    static let difficultyOn = Color(hex: 0x1F5FE0)
    static let online = Color(hex: 0x2BD17E)
    static let sunTop = Color(hex: 0xFFD35C)
    static let sunBottom = Color(hex: 0xFFB020)
    static let moonTop = Color(hex: 0x5B8CFF)
    static let moonBottom = Color(hex: 0x2F5FD8)
    static let progressStart = Color(hex: 0x4F8DFF)
    static let rankTop = [(Color(hex: 0xFFE8A3), Color(hex: 0x8A5A00)), (Color(hex: 0xE5E9F0), Color(hex: 0x4A5568)), (Color(hex: 0xF6D7BF), Color(hex: 0x7A4420))]
    static let storyBlack = Color(hex: 0x05080F)
    static let storyLoyalty = Color(hex: 0x6FA0FF)
    static let storySafety = Color(hex: 0x3FCFA8)
    static let storyNarration = Color(hex: 0x9FBDF2)
    static let deltaUp = Color(hex: 0x8FE8C6)
    static let deltaDown = Color(hex: 0xFF9AA0)
    static let timerHurryStart = Color(hex: 0xD6363E)
    static let timerHurryEnd = Color(hex: 0xFF6B72)
    static let logoTop = Color(hex: 0x3B8BFF)
    static let logoBottom = Color(hex: 0x1452D9)
}

private struct ColorsKey: EnvironmentKey {
    static let defaultValue = VsmColors.light
}

private struct ReduceMotionKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var vsm: VsmColors {
        get { self[ColorsKey.self] }
        set { self[ColorsKey.self] = newValue }
    }

    /// «Уменьшить анимацию» из настроек: как `html[data-reduce-motion]` на сайте отключает переходы.
    /// Системная настройка «Уменьшение движения» действует так же.
    var vsmReduceMotion: Bool {
        get { self[ReduceMotionKey.self] }
        set { self[ReduceMotionKey.self] = newValue }
    }
}

/// Тема приложения — токены сайта для выбранной темы и режим анимаций.
struct VsmTheme: ViewModifier {
    let theme: ThemeMode
    let reduceMotion: Bool
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion

    func body(content: Content) -> some View {
        let colors = theme == .dark ? VsmColors.dark : VsmColors.light
        content
            .environment(\.vsm, colors)
            .environment(\.vsmReduceMotion, reduceMotion || systemReduceMotion)
            .preferredColorScheme(theme == .dark ? .dark : .light)
            .tint(colors.brand)
            .foregroundStyle(colors.text)
    }
}

// MARK: - Шрифт

/// Шрифт сайта — Manrope (SIL OFL 1.1, лицензия в `Resources/Fonts/Manrope-OFL.txt`),
/// начертания 400–800 встроены в пакет и регистрируются при запуске.
enum Manrope {
    private static let faces = ["Manrope-Regular", "Manrope-Medium", "Manrope-SemiBold", "Manrope-Bold", "Manrope-ExtraBold"]
    @MainActor private static var registered = false

    @MainActor
    static func register() {
        guard !registered else { return }
        registered = true
        for face in faces {
            guard let url = Bundle.module.url(forResource: face, withExtension: "ttf", subdirectory: "Resources/Fonts") else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }

    static func name(_ weight: Font.Weight) -> String {
        switch weight {
        case .medium: "Manrope-Medium"
        case .semibold: "Manrope-SemiBold"
        case .bold: "Manrope-Bold"
        case .heavy, .black: "Manrope-ExtraBold"
        default: "Manrope-Regular"
        }
    }
}

/// Стиль текста сайта: размер, насыщенность, высота строки и трекинг (CSS px = pt).
struct TextStyle {
    let size: CGFloat
    let weight: Font.Weight
    let line: CGFloat
    let tracking: CGFloat

    init(_ size: CGFloat, _ weight: Font.Weight, line: CGFloat? = nil, tracking: CGFloat = 0) {
        self.size = size
        self.weight = weight
        self.line = line ?? size * 1.5
        self.tracking = tracking
    }

    /// Шрифт масштабируется вместе с системным размером текста (Dynamic Type).
    var font: Font { .custom(Manrope.name(weight), size: size, relativeTo: .body) }

    func sized(_ newSize: CGFloat) -> TextStyle { TextStyle(newSize, weight, line: line * newSize / size, tracking: tracking) }
    func weighted(_ newWeight: Font.Weight) -> TextStyle { TextStyle(size, newWeight, line: line, tracking: tracking) }
}

/// Шкала размеров сайта для телефона — та же, что в Android-клиенте.
enum VsmType {
    static let pageTitle = TextStyle(25.6, .heavy, line: 32, tracking: -0.01)
    static let sectionTitle = TextStyle(28, .heavy, line: 34, tracking: -0.01)
    static let heroTitle = TextStyle(24, .heavy, line: 31, tracking: -0.01)
    static let heroTitleLong = TextStyle(21, .heavy, line: 27, tracking: -0.01)
    static let heroEyebrow = TextStyle(15, .medium, line: 20, tracking: 0.02)
    static let heroText = TextStyle(15, .regular, line: 24)
    static let h2 = TextStyle(20.8, .heavy, line: 27)
    static let h3 = TextStyle(16, .heavy, line: 22)
    static let cardTitle = TextStyle(20.5, .bold, line: 30)
    static let body = TextStyle(16, .regular)
    static let bodyStrong = TextStyle(16, .semibold)
    static let bodyBold = TextStyle(16, .bold)
    static let bodyLarge = TextStyle(17, .regular, line: 27)
    static let button = TextStyle(17, .bold, line: 22)
    static let link = TextStyle(17, .semibold, line: 22)
    static let tag = TextStyle(15, .medium, line: 20)
    static let tagStrong = TextStyle(15, .semibold, line: 20)
    static let route = TextStyle(15.5, .regular, line: 22)
    static let kpiLabel = TextStyle(14, .semibold, line: 20)
    static let kpiValue = TextStyle(20.8, .heavy, line: 25)
    static let stat = TextStyle(22.4, .heavy, line: 28)
    static let small = TextStyle(14, .regular, line: 20)
    static let smallStrong = TextStyle(14, .semibold, line: 20)
    static let caption = TextStyle(13, .regular, line: 18)
    static let eyebrow = TextStyle(13, .heavy, line: 18, tracking: 0.14)
    static let nav = TextStyle(13, .medium, line: 18)
    static let navActive = TextStyle(13, .semibold, line: 18)
    static let brandName = TextStyle(26, .heavy, line: 30, tracking: -0.02)
    static let storyName = TextStyle(14, .heavy, line: 18, tracking: 0.08)
    static let storyText = TextStyle(17, .regular, line: 26)
    static let storyNarration = TextStyle(16, .regular, line: 25, tracking: 0.01)
    static let storyChoice = TextStyle(15, .regular, line: 21)
    static let storyBar = TextStyle(13, .regular, line: 18)
    static let storyIntroTitle = TextStyle(28, .heavy, line: 32)
    static let storyEndTitle = TextStyle(26, .heavy, line: 31)
}

extension View {
    /// Текст в стиле сайта: шрифт, трекинг в em и высота строки.
    func textStyle(_ style: TextStyle) -> some View {
        // Собственная высота строки Manrope ≈ 1.37 кегля — добираем до CSS line-height межстрочным интервалом
        font(style.font)
            .tracking(style.tracking * style.size)
            .lineSpacing(max(0, style.line - style.size * 1.366))
    }
}

/// Отступы сайта для телефона: поле страницы 16, шаг блоков 24 (`.stack`), padding карточки 24.
enum Metrics {
    static let gutter: CGFloat = 16
    static let stack: CGFloat = 24
    static let cardPadding: CGFloat = 24
    static let buttonHeight: CGFloat = 51
    static let buttonLargeHeight: CGFloat = 56
    static let inputHeight: CGFloat = 48
    static let touchMin: CGFloat = 44
}

/// Радиусы сайта: `--radius-lg` 20, `--radius` 16, кнопки и поля 12, картинки 14.
enum Radius {
    static let card: CGFloat = 20
    static let block: CGFloat = 16
    static let button: CGFloat = 12
    static let image: CGFloat = 14
    static let tab: CGFloat = 10
    static let tabs: CGFloat = 14
    static let storyBar: CGFloat = 18
    static let storyBox: CGFloat = 24
    static let storyChoice: CGFloat = 18
    static let storyEnd: CGFloat = 28
    static let nav: CGFloat = 30
}

/// Кривая появления сайта `cubic-bezier(.2,.7,.2,1)`.
extension Animation {
    static func site(_ duration: Double) -> Animation { .timingCurve(0.2, 0.7, 0.2, 1, duration: duration) }
}
