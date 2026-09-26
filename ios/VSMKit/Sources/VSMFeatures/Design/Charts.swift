import SwiftUI

/// Строка с переносом (`flex-wrap: wrap` сайта): теги, чипы, пункты истории.
struct Flow: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(width: proposal.width ?? .infinity, subviews: subviews)
        let width = rows.map(\.width).max() ?? 0
        let height = rows.map(\.height).reduce(0, +) + CGFloat(max(0, rows.count - 1)) * lineSpacing
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(width: bounds.width, subviews: subviews) {
            var x = bounds.minX
            for index in row.items {
                let size = subviews[index].sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
                subviews[index].place(at: CGPoint(x: x, y: y + (row.height - size.height) / 2), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + lineSpacing
        }
    }

    private struct Row {
        var items: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(width: CGFloat, subviews: Subviews) -> [Row] {
        var rows: [Row] = []
        var row = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(ProposedViewSize(width: width, height: nil))
            let needed = row.items.isEmpty ? size.width : row.width + spacing + size.width
            if !row.items.isEmpty, needed > width {
                rows.append(row)
                row = Row()
            }
            row.width = row.items.isEmpty ? size.width : row.width + spacing + size.width
            row.height = max(row.height, size.height)
            row.items.append(index)
        }
        if !row.items.isEmpty { rows.append(row) }
        return rows
    }
}

/// Появление графика: линии и столбцы вырастают за 0.9 с (ease-out), как анимация Recharts на сайте.
/// Canvas не интерполирует состояние сам, поэтому доля роста считается по часам и отдаётся в рисование.
private struct Growing<Content: View>: View {
    @ViewBuilder let content: (CGFloat) -> Content
    @State private var start = Date()
    @State private var finished = false
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        TimelineView(.animation(paused: finished || reduce)) { timeline in
            let t = reduce || finished ? 1 : min(1, timeline.date.timeIntervalSince(start) / 0.9)
            content(CGFloat(1 - pow(1 - t, 3)))
        }
        .task {
            start = Date()
            try? await Task.sleep(for: .seconds(1))
            finished = true
        }
    }
}

private func axisLabel(_ text: String, color: Color) -> Text {
    Text(text).font(.custom(Manrope.name(.regular), size: 12)).foregroundColor(color)
}

/// Штриховая линия сетки `strokeDasharray="3 3"`.
private let dash = StrokeStyle(lineWidth: 1, dash: [3, 3])

/// `ScaleChart` разбора: как менялись лояльность и безопасность по шагам (0–100).
struct ScaleLineChart: View {
    let labels: [String]
    let loyalty: [Int]
    let safety: [Int]
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(spacing: 8) {
            Growing { progress in Canvas { context, size in
                // Справа — место под подпись последнего шага
                let left: CGFloat = 34, top: CGFloat = 8, right = size.width - 24, bottom = size.height - 22
                func y(_ value: Int) -> CGFloat { bottom - (bottom - top) * CGFloat(value) / 100 }
                func x(_ index: Int) -> CGFloat { labels.count < 2 ? left : left + (right - left) * CGFloat(index) / CGFloat(labels.count - 1) }
                for tick in [0, 25, 50, 75, 100] {
                    context.stroke(Path { $0.move(to: CGPoint(x: left, y: y(tick))); $0.addLine(to: CGPoint(x: right, y: y(tick))) }, with: .color(colors.border), style: dash)
                    context.draw(axisLabel("\(tick)", color: colors.muted), at: CGPoint(x: left - 6, y: y(tick)), anchor: .trailing)
                }
                for (index, label) in labels.enumerated() {
                    context.stroke(Path { $0.move(to: CGPoint(x: x(index), y: top)); $0.addLine(to: CGPoint(x: x(index), y: bottom)) }, with: .color(colors.border), style: dash)
                    context.draw(axisLabel(label, color: colors.muted), at: CGPoint(x: x(index), y: size.height - 9), anchor: .center)
                }
                for (series, color) in [(safety, colors.safety), (loyalty, colors.loyalty)] {
                    let points = series.enumerated().map { CGPoint(x: x($0.offset), y: bottom - (bottom - y($0.element)) * progress) }
                    var line = Path()
                    for (index, point) in points.enumerated() {
                        if index == 0 { line.move(to: point) } else { line.addLine(to: point) }
                    }
                    context.stroke(line, with: .color(color), lineWidth: 2)
                    for point in points {
                        let dot = Path(ellipseIn: CGRect(x: point.x - 3, y: point.y - 3, width: 6, height: 6))
                        context.fill(dot, with: .color(.white))
                        context.stroke(dot, with: .color(color), lineWidth: 2)
                    }
                }
            } }
            .frame(height: 220)
            HStack(spacing: 16) {
                legend("Безопасность", colors.safety)
                legend("Лояльность", colors.loyalty)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("График шкал: лояльность \(loyalty.last ?? 0), безопасность \(safety.last ?? 0)")
    }

    private func legend(_ title: String, _ color: Color) -> some View {
        HStack(spacing: 6) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(title).textStyle(VsmType.small).foregroundStyle(color)
        }
    }
}

/// «Прогресс: XP по неделям» — столбцы brand со скруглением сверху, как BarChart сайта.
struct XpBarChart: View {
    let labels: [String]
    let values: [Int]
    @Environment(\.vsm) private var colors

    var body: some View {
        Growing { progress in Canvas { context, size in
            let maximum = max(values.max() ?? 0, 1)
            let step = [1, 2, 5, 10, 25, 50, 100, 150, 200, 250, 500, 1000].first { maximum / $0 <= 4 } ?? 1000
            let top = ((maximum + step - 1) / step) * step
            let left: CGFloat = 40, chartTop: CGFloat = 8, right = size.width, bottom = size.height - 22
            func y(_ value: Int) -> CGFloat { bottom - (bottom - chartTop) * CGFloat(value) / CGFloat(top) }
            var tick = 0
            while tick <= top {
                context.stroke(Path { $0.move(to: CGPoint(x: left, y: y(tick))); $0.addLine(to: CGPoint(x: right, y: y(tick))) }, with: .color(colors.border), style: dash)
                context.draw(axisLabel("\(tick)", color: colors.muted), at: CGPoint(x: left - 6, y: y(tick)), anchor: .trailing)
                tick += step
            }
            let slot = (right - left) / CGFloat(max(values.count, 1))
            for (index, value) in values.enumerated() {
                let width = slot * 0.7
                let x = left + slot * CGFloat(index) + (slot - width) / 2
                let height = (bottom - y(value)) * progress
                if height > 0 {
                    let bar = UnevenRoundedRectangle(topLeadingRadius: min(6, height), topTrailingRadius: min(6, height))
                        .path(in: CGRect(x: x, y: bottom - height, width: width, height: height))
                    context.fill(bar, with: .color(colors.brand))
                }
                context.draw(axisLabel(labels[index], color: colors.muted), at: CGPoint(x: x + width / 2, y: size.height - 9), anchor: .center)
            }
        } }
        .frame(height: 240)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("XP по неделям: \(values.map(String.init).joined(separator: ", "))")
    }
}

/// Радар компетенций профиля: сетка-пятиугольник, заливка brand с прозрачностью .25.
struct CompetenceRadar: View {
    let items: [(title: String, points: Int)]
    @Environment(\.vsm) private var colors

    var body: some View {
        Growing { progress in Canvas { context, size in
            guard !items.isEmpty else { return }
            // Центр ниже середины: над верхней вершиной помещается подпись в три строки
            let center = CGPoint(x: size.width / 2, y: size.height / 2 + 22)
            let radius = min(size.width, size.height) * 0.30
            let maximum = CGFloat(max(items.map(\.points).max() ?? 0, 1))
            func point(_ index: Int, _ fraction: CGFloat) -> CGPoint {
                let angle = -Double.pi / 2 + 2 * Double.pi * Double(index) / Double(items.count)
                return CGPoint(x: center.x + radius * fraction * cos(angle), y: center.y + radius * fraction * sin(angle))
            }
            for ring in 1...5 {
                var path = Path()
                for index in items.indices {
                    let p = point(index, CGFloat(ring) / 5)
                    if index == 0 { path.move(to: p) } else { path.addLine(to: p) }
                }
                path.closeSubpath()
                context.stroke(path, with: .color(colors.border), lineWidth: 1)
            }
            for index in items.indices {
                context.stroke(Path { $0.move(to: center); $0.addLine(to: point(index, 1)) }, with: .color(colors.border), lineWidth: 1)
            }
            var shape = Path()
            for (index, item) in items.enumerated() {
                let p = point(index, CGFloat(item.points) / maximum * progress)
                if index == 0 { shape.move(to: p) } else { shape.addLine(to: p) }
            }
            shape.closeSubpath()
            context.fill(shape, with: .color(colors.brand.opacity(0.25)))
            context.stroke(shape, with: .color(colors.brand), lineWidth: 1.5)
            for (index, item) in items.enumerated() {
                let p = point(index, 1.18)
                let anchor: UnitPoint = p.x < center.x - 4 ? .trailing : (p.x > center.x + 4 ? .leading : .center)
                let label = context.resolve(axisLabel(item.title, color: colors.muted))
                let measured = label.measure(in: CGSize(width: size.width * 0.42, height: 60))
                // Подпись над верхней и под нижней вершиной не заходит на сетку; боковые — по центру точки
                let vertical: CGFloat = abs(p.x - center.x) < 4 ? (p.y < center.y ? 1 : 0) : 0.5
                var origin = CGPoint(x: p.x - measured.width * anchor.x, y: p.y - measured.height * vertical)
                origin.x = min(max(0, origin.x), size.width - measured.width)
                context.draw(label, in: CGRect(origin: origin, size: measured))
            }
        } }
        .frame(height: 300)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(items.map { "\($0.title): \($0.points)" }.joined(separator: ", "))
    }
}
