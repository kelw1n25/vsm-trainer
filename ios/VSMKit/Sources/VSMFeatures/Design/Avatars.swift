import SwiftUI
import VSMCore

/// Варианты конструктора — те же, что у сайта (`frontend/src/avatar.ts`) и сервера (`profiles/avatar.py`).
enum AvatarOptions {
    static func label(_ background: Avatar.Background) -> String {
        switch background {
        case .blue: "Голубой"
        case .mint: "Мятный"
        case .sand: "Песочный"
        case .lilac: "Сиреневый"
        case .coral: "Коралловый"
        case .night: "Ночной"
        }
    }

    static func colors(_ background: Avatar.Background) -> (top: Color, bottom: Color) {
        switch background {
        case .blue: (Color(hex: 0xEAF2FF), Color(hex: 0xC9DDFB))
        case .mint: (Color(hex: 0xE6F7EF), Color(hex: 0xBFE8D3))
        case .sand: (Color(hex: 0xFFF4E4), Color(hex: 0xF5D9AE))
        case .lilac: (Color(hex: 0xF1ECFF), Color(hex: 0xD8CCF7))
        case .coral: (Color(hex: 0xFDECEC), Color(hex: 0xF6C6C6))
        case .night: (Color(hex: 0x2A3A5E), Color(hex: 0x16223F))
        }
    }

    static func label(_ headwear: Avatar.Headwear) -> String {
        switch headwear {
        case .cap: "Фуражка"
        case .none: "Без головного убора"
        }
    }

    static func label(_ tie: Avatar.Tie) -> String {
        switch tie {
        case .red: "Красный"
        case .blue: "Синий"
        case .green: "Зелёный"
        case .graphite: "Графитовый"
        }
    }

    static func color(_ tie: Avatar.Tie) -> Color {
        switch tie {
        case .red: Color(hex: 0xD23A3A)
        case .blue: Color(hex: 0x2F6FDD)
        case .green: Color(hex: 0x169C7A)
        case .graphite: Color(hex: 0x3A4460)
        }
    }
}

private struct MyAvatarKey: EnvironmentKey {
    static let defaultValue = Avatar()
}

extension EnvironmentValues {
    /// Аватар вошедшего сотрудника — задаёт оболочка приложения, чтобы новый выбор был виден сразу везде.
    var myAvatar: Avatar {
        get { self[MyAvatarKey.self] }
        set { self[MyAvatarKey.self] = newValue }
    }
}

/// Аватар проводника — манекен в форме, как `Avatar` сайта, с белым кольцом и тенью; детали — из конструктора.
struct UserAvatar: View {
    let size: CGFloat
    var ring: CGFloat = 3
    var avatar: Avatar?
    @Environment(\.myAvatar) private var myAvatar
    @Environment(\.vsm) private var colors

    var body: some View {
        AvatarDrawing(avatar: avatar ?? myAvatar)
            .frame(width: size, height: size)
            .background(colors.surface)
            .clipShape(Circle())
            .overlay(Circle().strokeBorder(colors.surface, lineWidth: ring))
            .vsmShadow(radius: 6, y: 3)
            .accessibilityHidden(true)
    }
}

private let mannequin = Color(hex: 0xC9C9C9)
private let mannequinShade = Color(hex: 0xA4A4A4)
private let gold = Color(hex: 0xE7C15A)

/// Рисунок аватара в сетке 64 × 64 — те же фигуры и координаты, что в SVG сайта.
struct AvatarDrawing: View {
    let avatar: Avatar

    var body: some View {
        Canvas { context, size in
            context.scaleBy(x: size.width / 64, y: size.height / 64)
            let background = AvatarOptions.colors(avatar.background)
            context.fill(
                Path(CGRect(x: 0, y: 0, width: 64, height: 64)),
                with: .linearGradient(Gradient(colors: [background.top, background.bottom]), startPoint: .zero, endPoint: CGPoint(x: 0, y: 64))
            )
            var body = Path()
            body.move(to: CGPoint(x: 11, y: 64))
            body.addCurve(to: CGPoint(x: 32, y: 46), control1: CGPoint(x: 12, y: 52), control2: CGPoint(x: 20, y: 46))
            body.addCurve(to: CGPoint(x: 53, y: 64), control1: CGPoint(x: 44, y: 46), control2: CGPoint(x: 52, y: 52))
            body.closeSubpath()
            context.fill(body, with: .linearGradient(
                Gradient(stops: [.init(color: mannequin, location: 0), .init(color: mannequin, location: 0.55), .init(color: mannequinShade, location: 1)]),
                startPoint: CGPoint(x: 11, y: 0), endPoint: CGPoint(x: 53, y: 0)
            ))
            var tie = Path()
            tie.addLines([CGPoint(x: 31, y: 47), CGPoint(x: 33, y: 47), CGPoint(x: 34.5, y: 57), CGPoint(x: 32, y: 60), CGPoint(x: 29.5, y: 57)])
            tie.closeSubpath()
            context.fill(tie, with: .color(AvatarOptions.color(avatar.tie)))
            context.fill(Path(roundedRect: CGRect(x: 38, y: 52, width: 6, height: 4), cornerRadius: 1), with: .color(gold))
            context.fill(Path(roundedRect: CGRect(x: 28.5, y: 37, width: 7, height: 10), cornerRadius: 3.5), with: .color(mannequin))
            context.fill(Path(ellipseIn: CGRect(x: 18, y: 13, width: 28, height: 28)), with: .radialGradient(
                Gradient(stops: [.init(color: mannequin, location: 0), .init(color: mannequin, location: 0.62), .init(color: mannequinShade, location: 1)]),
                center: CGPoint(x: 29.76, y: 24.2), startRadius: 0, endRadius: 20.16
            ))
            if avatar.headwear == .cap {
                // Фуражка анфас: тулья, околыш с кантом, кокарда и симметричный козырёк
                var crown = Path()
                crown.move(to: CGPoint(x: 18.6, y: 19.6))
                crown.addCurve(to: CGPoint(x: 32, y: 6.2), control1: CGPoint(x: 15, y: 15.4), control2: CGPoint(x: 20.4, y: 6.6))
                crown.addCurve(to: CGPoint(x: 45.4, y: 19.6), control1: CGPoint(x: 43.6, y: 6.6), control2: CGPoint(x: 49, y: 15.4))
                crown.closeSubpath()
                context.fill(crown, with: .color(Color(hex: 0x34487A)))
                context.fill(Path(roundedRect: CGRect(x: 18.4, y: 17.4, width: 27.2, height: 4), cornerRadius: 1.3), with: .color(Color(hex: 0x1E2848)))
                var piping = Path()
                piping.move(to: CGPoint(x: 18.6, y: 17.6))
                piping.addLine(to: CGPoint(x: 45.4, y: 17.6))
                context.stroke(piping, with: .color(gold), lineWidth: 0.8)
                context.fill(Path(ellipseIn: CGRect(x: 29.6, y: 13.3, width: 4.8, height: 5.4)), with: .color(gold))
                context.fill(Path(ellipseIn: CGRect(x: 31, y: 14.8, width: 2, height: 2.4)), with: .color(Color(hex: 0xC9483E)))
                var visor = Path()
                visor.move(to: CGPoint(x: 17.8, y: 21.2))
                visor.addCurve(to: CGPoint(x: 46.2, y: 21.2), control1: CGPoint(x: 25, y: 23), control2: CGPoint(x: 39, y: 23))
                visor.addCurve(to: CGPoint(x: 32, y: 26), control1: CGPoint(x: 44.6, y: 25), control2: CGPoint(x: 39, y: 26))
                visor.addCurve(to: CGPoint(x: 17.8, y: 21.2), control1: CGPoint(x: 25, y: 26), control2: CGPoint(x: 19.4, y: 25))
                visor.closeSubpath()
                context.fill(visor, with: .color(Color(hex: 0x141B30)))
            }
        }
    }
}

/// Образец цвета для конструктора: круг с кольцом выбора, озвучивается как «Голубой, выбран».
struct Swatch<Fill: ShapeStyle>: View {
    let label: String
    let fill: Fill
    let selected: Bool
    let action: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        Button(action: action) {
            Circle()
                .fill(fill)
                .overlay(Circle().strokeBorder(colors.controlBorder, lineWidth: 1))
                .frame(width: 36, height: 36)
                .padding(4)
                .overlay(Circle().strokeBorder(selected ? colors.brand : .clear, lineWidth: 2))
                .frame(width: 44, height: 44)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}

/// Конструктор аватара — `AvatarEditor` сайта: фон, головной убор, галстук; выбор сразу сохраняется на сервере.
struct AvatarEditor: View {
    let store: MyAvatarStore
    @Environment(\.vsm) private var colors

    var body: some View {
        let avatar = store.current
        Card(spacing: 16) {
            CardTitle("Аватар")
            HStack(alignment: .top, spacing: 20) {
                UserAvatar(size: 112, ring: 4, avatar: avatar)
                group("Фон") {
                    ForEach(Avatar.Background.allCases, id: \.self) { code in
                        let pair = AvatarOptions.colors(code)
                        Swatch(label: AvatarOptions.label(code), fill: LinearGradient(colors: [pair.top, pair.bottom], startPoint: .top, endPoint: .bottom), selected: avatar.background == code) {
                            choose { $0.background = code }
                        }
                    }
                }
            }
            group("Головной убор") {
                ForEach(Avatar.Headwear.allCases, id: \.self) { code in
                    Chip(title: AvatarOptions.label(code), selected: avatar.headwear == code) { choose { $0.headwear = code } }
                }
            }
            group("Галстук") {
                ForEach(Avatar.Tie.allCases, id: \.self) { code in
                    Swatch(label: AvatarOptions.label(code), fill: AvatarOptions.color(code), selected: avatar.tie == code) {
                        choose { $0.tie = code }
                    }
                }
            }
            Muted(store.status ?? "Аватар собирается из деталей формы — фотографии не нужны и не хранятся.", small: true)
                .accessibilityAddTraits(.updatesFrequently)
        }
    }

    private func group<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).textStyle(VsmType.bodyStrong).foregroundStyle(colors.text)
            Flow(spacing: 4, lineSpacing: 6) { content() }
        }
    }

    private func choose(_ change: (inout Avatar) -> Void) {
        var next = store.current
        change(&next)
        Task { await store.choose(next) }
    }
}
