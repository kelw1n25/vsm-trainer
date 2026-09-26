import SwiftUI
import VSMCore

/// Главная — `HomePage`: hero-карусель, «Сценарии» со счётчиком, три карточки, «Все сценарии».
struct HomeScreen: View {
    @State private var model: HomeViewModel
    @Environment(Navigator.self) private var navigator

    init(container: AppContainer, fullName: String) {
        _model = State(initialValue: HomeViewModel(repository: container.trainer, fullName: fullName))
    }

    var body: some View {
        Page {
            ScreenContent(state: model.state, retry: model.load) { content in
                HeroCarousel(slides: content.slides)
                VStack(alignment: .leading, spacing: 16) {
                    SectionTitle(text: "Сценарии", count: content.scenarios.count)
                    LinkAction(title: "Все сценарии", leading: .grid, trailing: .chevronRight) { navigator.openTab(.scenarios) }
                }
                ForEach(Array(content.scenarios.prefix(3).enumerated()), id: \.element.id) { index, scenario in
                    ScenarioCard(scenario: scenario, index: index)
                }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}

/// Сколько поезд проезжает за весь ход ползунка (единицы viewBox иллюстрации 1000 × 300).
private let trainDistance: CGFloat = 220
private let railSlope: CGFloat = 0.0198

/// Hero с ползунком — `HeroCarousel` сайта: приветствие, челлендж недели, рекомендация. Ползунок ведёт поезд;
/// отпустили — поезд докатывается до ближайшего слайда. На телефоне вместо курсора и колеса — палец.
struct HeroCarousel: View {
    let slides: [HeroSlide]
    @State private var progress: CGFloat = 0
    @Environment(\.vsm) private var colors
    @Environment(\.vsmReduceMotion) private var reduce

    private var segments: CGFloat { CGFloat(max(slides.count - 1, 1)) }
    private var index: Int { min(max(Int((progress * segments).rounded()), 0), slides.count - 1) }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
        let slide = slides[index]
        ZStack(alignment: .bottomLeading) {
            VStack(alignment: .leading, spacing: 0) {
                SlideText(slide: slide)
                    .id(index)
                    .transition(.asymmetric(insertion: .opacity.combined(with: .offset(x: -12)), removal: .opacity))
                HeroTrain(progress: progress)
                    .padding(.top, 24)
                    .padding(.bottom, slides.count > 1 ? 84 : 0)
            }
            .animation(reduce ? nil : .easeOut(duration: 0.4), value: index)
            if slides.count > 1 {
                HeroScroller(value: progress, steps: slides.count, label: slide.eyebrow, change: { progress = $0 }, release: snap)
                    .padding(.leading, 18)
                    .padding(.bottom, 14)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            ZStack(alignment: .topLeading) {
                colors.heroGradient
                // Мягкое светлое пятно в углу (`.hero::before`)
                RadialGradient(colors: [colors.heroGlow, .clear], center: .center, startRadius: 0, endRadius: 210)
                    .frame(width: 420, height: 420)
                    .offset(x: -140, y: -200)
            }
        }
        .clipShape(shape)
        .overlay(shape.strokeBorder(colors.heroBorder, lineWidth: 1))
        .gesture(
            // Свайп по hero листает слайды — как колесо мыши на сайте
            DragGesture(minimumDistance: 20).onEnded { drag in
                let next = drag.translation.width < -60 ? index + 1 : (drag.translation.width > 60 ? index - 1 : index)
                move(to: CGFloat(min(max(next, 0), slides.count - 1)) / segments)
            }
        )
        .accessibilityElement(children: .contain)
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: move(to: CGFloat(min(index + 1, slides.count - 1)) / segments)
            case .decrement: move(to: CGFloat(max(index - 1, 0)) / segments)
            @unknown default: break
            }
        }
    }

    private func snap() {
        move(to: (progress * segments).rounded() / segments)
    }

    private func move(to point: CGFloat) {
        let target = min(max(point, 0), 1)
        if reduce { progress = target } else { withAnimation(.easeOut(duration: 0.3)) { progress = target } }
    }
}

private struct SlideText: View {
    let slide: HeroSlide
    @Environment(\.vsm) private var colors

    var body: some View {
        let long = slide.titleBottom.count > 24
        VStack(alignment: .leading, spacing: 10) {
            Text(slide.eyebrow.uppercased()).textStyle(VsmType.heroEyebrow).foregroundStyle(colors.heroEyebrow)
            Text("\(slide.titleTop)\n\(slide.titleBottom)")
                .textStyle(long ? VsmType.heroTitleLong : VsmType.heroTitle)
                .foregroundStyle(colors.heading)
                .accessibilityAddTraits(.isHeader)
            Text(slide.text)
                .textStyle(VsmType.heroText)
                .foregroundStyle(colors.heroText)
                .lineLimit(long ? 1 : 2)
                .padding(.top, long ? 2 : 10)
        }
        .padding(.horizontal, 22)
        .padding(.top, 28)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Поезд hero из трёх слоёв сайта: город смещается медленнее (параллакс), поезд — вместе с ползунком.
private struct HeroTrain: View {
    let progress: CGFloat

    var body: some View {
        GeometryReader { proxy in
            let unit = proxy.size.width / 1000
            let offset = progress * trainDistance
            ZStack {
                AssetImage(name: "hero_parallax").offset(x: -offset * 0.35 * unit)
                AssetImage(name: "hero_static")
                AssetImage(name: "hero_drive").offset(x: -offset * unit, y: offset * railSlope * unit)
            }
        }
        .aspectRatio(1000 / 300, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

/// Ползунок hero (`HeroScroller`): стеклянная плашка, дорожка с делениями, бегунок с синей обводкой.
private struct HeroScroller: View {
    let value: CGFloat
    let steps: Int
    let label: String
    let change: (CGFloat) -> Void
    let release: () -> Void
    @Environment(\.vsm) private var colors
    private let width: CGFloat = 220

    var body: some View {
        VStack(spacing: 8) {
            ZStack(alignment: .leading) {
                Capsule().fill(colors.trackStrong).frame(height: 6)
                Capsule()
                    .fill(LinearGradient(colors: [Palette.progressStart, colors.brand], startPoint: .leading, endPoint: .trailing))
                    .frame(width: width * value, height: 6)
                ForEach(0..<steps, id: \.self) { step in
                    Circle().fill(.white).frame(width: 4, height: 4).offset(x: width * CGFloat(step) / CGFloat(steps - 1) - 2)
                }
                Circle()
                    .fill(.white)
                    .overlay(Circle().strokeBorder(colors.brand, lineWidth: 3))
                    .frame(width: 22, height: 22)
                    .shadow(color: colors.brand.opacity(0.35), radius: 4, y: 2)
                    .offset(x: width * value - 11)
            }
            .frame(width: width, height: 22)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { change(min(max($0.location.x / width, 0), 1)) }
                    .onEnded { _ in release() }
            )
            Text("⇆ ведите пальцем по ползунку").textStyle(VsmType.caption.sized(12)).foregroundStyle(colors.muted)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(colors.glassSoft, in: RoundedRectangle(cornerRadius: Radius.storyBar, style: .continuous))
        .shadow(color: colors.shadow.opacity(0.1), radius: 6, y: 3)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Слайд: \(label)")
    }
}
