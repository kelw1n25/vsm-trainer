import SwiftUI
#if canImport(UIKit)
import UIKit
typealias PlatformImage = UIImage
#else
import AppKit
typealias PlatformImage = NSImage
#endif

/// Картинки, экспортированные из компонентов сайта (`tools/mobile-assets`): персонажи, фоны, иллюстрации, фото.
/// Имена строятся из данных сценария (например, `sprite_p_sit_annoyed_down_none_r`), поэтому ищутся по имени.
enum Assets {
    private static let cache = NSCache<NSString, PlatformImage>()

    static func image(_ name: String) -> PlatformImage? {
        if let cached = cache.object(forKey: name as NSString) { return cached }
        guard let url = Bundle.module.url(forResource: name, withExtension: "webp", subdirectory: "Resources/Images"),
              let data = try? Data(contentsOf: url),
              let image = PlatformImage(data: data)
        else { return nil }
        cache.setObject(image, forKey: name as NSString)
        return image
    }

    static func exists(_ name: String) -> Bool {
        Bundle.module.url(forResource: name, withExtension: "webp", subdirectory: "Resources/Images") != nil
    }

    /// Иллюстрация сценария `ScenarioImage`: своя для каждого сценария, иначе — по типу ситуации, как на сайте.
    static func scenarioImage(_ scenarioId: String, category: String) -> String {
        let own = "scenario_" + scenarioId.replacingOccurrences(of: "-", with: "_")
        if exists(own) { return own }
        return [
            "conflict": "scenario_business_seat_conflict",
            "medical": "scenario_passenger_unwell",
            "safety": "scenario_unattended_item",
            "service": "scenario_train_delay",
        ][category] ?? "scenario_business_seat_conflict"
    }
}

/// Картинка из ресурсов пакета; если её нет — ничего не рисуется (размер задаёт вызывающий).
struct AssetImage: View {
    let name: String
    var contentMode: ContentMode = .fit

    var body: some View {
        if let image = Assets.image(name) {
            #if canImport(UIKit)
            Image(uiImage: image).resizable().aspectRatio(contentMode: contentMode)
            #else
            Image(nsImage: image).resizable().aspectRatio(contentMode: contentMode)
            #endif
        }
    }
}

// MARK: - Иконки сайта (inline SVG из компонентов React), перерисованные как Path в сетке 24 × 24

enum IconKind {
    case arrowRight, chevronDown, chevronLeft, chevronRight, sun, moon, grid, pin
}

struct Icon: View {
    let kind: IconKind
    var size: CGFloat = 20
    var color: Color

    var body: some View {
        Canvas { context, canvas in
            let scale = canvas.width / 24
            context.scaleBy(x: scale, y: scale)
            let stroke = StrokeStyle(lineWidth: kind.strokeWidth, lineCap: .round, lineJoin: .round)
            switch kind {
            case .pin:
                context.fill(kind.path, with: .color(color), style: FillStyle(eoFill: true))
            default:
                context.stroke(kind.path, with: .color(color), style: stroke)
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

private extension IconKind {
    var strokeWidth: CGFloat {
        switch self {
        case .chevronDown, .chevronLeft, .chevronRight: 2.4
        default: 2
        }
    }

    var path: Path {
        Path { p in
            switch self {
            case .arrowRight:
                p.move(to: CGPoint(x: 5, y: 12)); p.addLine(to: CGPoint(x: 19, y: 12))
                p.move(to: CGPoint(x: 13, y: 6)); p.addLine(to: CGPoint(x: 19, y: 12)); p.addLine(to: CGPoint(x: 13, y: 18))
            case .chevronDown:
                p.move(to: CGPoint(x: 6, y: 9)); p.addLine(to: CGPoint(x: 12, y: 15)); p.addLine(to: CGPoint(x: 18, y: 9))
            case .chevronLeft:
                p.move(to: CGPoint(x: 15, y: 6)); p.addLine(to: CGPoint(x: 9, y: 12)); p.addLine(to: CGPoint(x: 15, y: 18))
            case .chevronRight:
                p.move(to: CGPoint(x: 9, y: 6)); p.addLine(to: CGPoint(x: 15, y: 12)); p.addLine(to: CGPoint(x: 9, y: 18))
            case .sun:
                p.addEllipse(in: CGRect(x: 8, y: 8, width: 8, height: 8))
                let rays: [(CGFloat, CGFloat, CGFloat, CGFloat)] = [
                    (12, 2, 12, 4), (12, 20, 12, 22), (4.9, 4.9, 6.3, 6.3), (17.7, 17.7, 19.1, 19.1),
                    (2, 12, 4, 12), (20, 12, 22, 12), (4.9, 19.1, 6.3, 17.7), (17.7, 6.3, 19.1, 4.9),
                ]
                for ray in rays {
                    p.move(to: CGPoint(x: ray.0, y: ray.1)); p.addLine(to: CGPoint(x: ray.2, y: ray.3))
                }
            case .moon:
                // Полумесяц: внешняя дуга радиуса 8 и вогнутая внутренняя, как `M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z`
                p.move(to: CGPoint(x: 20, y: 14.5))
                p.addCurve(to: CGPoint(x: 9.5, y: 4), control1: CGPoint(x: 14.6, y: 16.2), control2: CGPoint(x: 8.2, y: 10.2))
                p.addCurve(to: CGPoint(x: 20, y: 14.5), control1: CGPoint(x: 3.6, y: 6.6), control2: CGPoint(x: 6.4, y: 24.4))
                p.closeSubpath()
            case .grid:
                for origin in [CGPoint(x: 4, y: 4), CGPoint(x: 13.5, y: 4), CGPoint(x: 4, y: 13.5)] {
                    p.addRoundedRect(in: CGRect(origin: origin, size: CGSize(width: 6.5, height: 6.5)), cornerSize: CGSize(width: 1.8, height: 1.8))
                }
                p.move(to: CGPoint(x: 13.5, y: 16.75)); p.addLine(to: CGPoint(x: 20, y: 16.75))
                p.move(to: CGPoint(x: 16.75, y: 13.5)); p.addLine(to: CGPoint(x: 16.75, y: 20))
            case .pin:
                p.move(to: CGPoint(x: 12, y: 2))
                p.addCurve(to: CGPoint(x: 5, y: 9), control1: CGPoint(x: 8.13, y: 2), control2: CGPoint(x: 5, y: 5.13))
                p.addCurve(to: CGPoint(x: 12, y: 22), control1: CGPoint(x: 5, y: 14), control2: CGPoint(x: 12, y: 22))
                p.addCurve(to: CGPoint(x: 19, y: 9), control1: CGPoint(x: 12, y: 22), control2: CGPoint(x: 19, y: 14))
                p.addCurve(to: CGPoint(x: 12, y: 2), control1: CGPoint(x: 19, y: 5.13), control2: CGPoint(x: 15.87, y: 2))
                p.closeSubpath()
                p.addEllipse(in: CGRect(x: 9.5, y: 6.5, width: 5, height: 5))
            }
        }
    }
}

/// Логотип сайта `LogoMark`: синяя «стрела» с градиентом, сетка 52 × 34.
struct LogoMark: View {
    var width: CGFloat = 52

    var body: some View {
        Canvas { context, canvas in
            let scale = canvas.width / 52
            context.scaleBy(x: scale, y: scale)
            var arrow = Path()
            arrow.move(to: CGPoint(x: 3, y: 32)); arrow.addLine(to: CGPoint(x: 17, y: 3)); arrow.addLine(to: CGPoint(x: 48, y: 3))
            arrow.addLine(to: CGPoint(x: 42.5, y: 13)); arrow.addLine(to: CGPoint(x: 24, y: 13)); arrow.addLine(to: CGPoint(x: 15, y: 32))
            arrow.closeSubpath()
            context.fill(arrow, with: .linearGradient(
                Gradient(colors: [Palette.logoTop, Palette.logoBottom]), startPoint: CGPoint(x: 3, y: 3), endPoint: CGPoint(x: 48, y: 32)
            ))
            var tail = Path()
            tail.move(to: CGPoint(x: 22, y: 32)); tail.addLine(to: CGPoint(x: 30, y: 17)); tail.addLine(to: CGPoint(x: 50, y: 17))
            tail.addLine(to: CGPoint(x: 42, y: 32)); tail.closeSubpath()
            context.fill(tail, with: .color(Palette.logoBottom))
        }
        .frame(width: width, height: width * 34 / 52)
        .accessibilityHidden(true)
    }
}
