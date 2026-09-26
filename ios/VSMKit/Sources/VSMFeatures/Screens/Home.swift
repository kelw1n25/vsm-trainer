import SwiftUI
import VSMCore

/// Главная: hero-карусель (приветствие, челлендж недели, рекомендация) и «Про ВСМ» — общая картина магистрали.
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
                AboutVsm(serviceClasses: content.serviceClasses) { navigator.openTab(.handbook) }
            }
        }
        .task { await model.load() }
        .refreshable { await model.load() }
    }
}

/// «Про ВСМ» на главной — общая картина за полминуты чтения: цифры магистрали, классы обслуживания
/// и три правила, на которых держатся сценарии. Классы и нормы — из справочника кейсодержателя на сервере.
private struct AboutVsm: View {
    let serviceClasses: [ServiceClass]
    let handbook: () -> Void
    @Environment(\.vsm) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SectionTitle(text: "Про ВСМ")
            Muted("Высокоскоростная магистраль Москва — Санкт-Петербург: премиальный сервис, где решения принимаются за секунды.")
            KpiGrid(items: [
                KpiItem(label: "Скорость", text: "до 400 км/ч"),
                KpiItem(label: "Москва — СПб", text: "≈ 2 ч 15 мин"),
                KpiItem(label: "Классы сервиса", text: serviceClasses.isEmpty ? "—" : "\(serviceClasses.count)"),
                KpiItem(label: "Стоянка на станции", text: "≈ 1 мин"),
            ])
            if !serviceClasses.isEmpty {
                Card(spacing: 10) {
                    CardTitle("Классы и ожидание сервиса")
                    ForEach(Array(serviceClasses.enumerated()), id: \.element.id) { index, item in
                        if index > 0 { Rectangle().fill(colors.border).frame(height: 1) }
                        HStack(spacing: 12) {
                            Text(item.title).textStyle(VsmType.bodyBold).foregroundStyle(colors.text).frame(maxWidth: .infinity, alignment: .leading)
                            Tag(text: item.layout)
                            Text("до \(item.maxWaitMinutes) мин").textStyle(VsmType.small).foregroundStyle(colors.muted)
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
            }
            Card(spacing: 8) {
                CardTitle("Что важно проводнику")
                Bullet(text: "О прибытии объявлять за 10–15 минут")
                Bullet(text: "Неотложные просьбы, например первая помощь, — вне очереди")
                Bullet(text: "Говорить по ролевой модели: признать → правило → решение → заверить")
            }
            LinkAction(title: "Подробнее — в справочнике", trailing: .chevronRight, action: handbook)
        }
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
        ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 0) {
                SlideText(slide: slide, trailing: slides.count > 1 ? 44 : 22)
                    .id(index)
                    .transition(.asymmetric(insertion: .opacity.combined(with: .offset(x: -12)), removal: .opacity))
                    // Высота блока — по самому длинному слайду: при листании поезд и страница не прыгают
                    .frame(minHeight: 196, alignment: .top)
                HeroTrain(progress: progress).padding(.top, 12)
            }
            .animation(reduce ? nil : .easeOut(duration: 0.4), value: index)
            if slides.count > 1 {
                HeroScroller(value: progress, steps: slides.count, label: slide.eyebrow, change: { progress = $0 }, release: snap)
                    .padding(.top, 18)
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
    /// Справа — место под вертикальный ползунок.
    let trailing: CGFloat
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
        .padding(.leading, 22)
        .padding(.trailing, trailing)
        .padding(.top, 28)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Во сколько раз поезд крупнее ширины карточки: нос в кадре, хвост уходит за край — без пустоты под поездом.
private let trainScale: CGFloat = 1.35

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
            .frame(width: proxy.size.width, height: proxy.size.width * 0.3)
            .scaleEffect(trainScale, anchor: .bottomLeading)
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .bottomLeading)
        }
        .aspectRatio(1000 / (300 * trainScale), contentMode: .fit)
        .accessibilityHidden(true)
    }
}

/// Ползунок hero: тонкая вертикальная линия справа с точками слайдов и бегунком. Сверху — первый слайд,
/// снизу — последний; ведётся пальцем вверх-вниз, касание по линии — сразу к слайду.
private struct HeroScroller: View {
    let value: CGFloat
    let steps: Int
    let label: String
    let change: (CGFloat) -> Void
    let release: () -> Void
    @Environment(\.vsm) private var colors
    private let track: CGFloat = 132
    private let margin: CGFloat = 12

    var body: some View {
        ZStack(alignment: .top) {
            Capsule().fill(colors.trackStrong).frame(width: 3, height: track)
            Capsule()
                .fill(LinearGradient(colors: [Palette.progressStart, colors.brand], startPoint: .top, endPoint: .bottom))
                .frame(width: 3, height: track * value)
            ForEach(0..<steps, id: \.self) { step in
                let at = CGFloat(step) / CGFloat(steps - 1)
                Circle().fill(at <= value + 0.001 ? colors.brand : colors.trackStrong).frame(width: 6, height: 6).offset(y: track * at - 3)
            }
            Circle()
                .fill(.white)
                .overlay(Circle().strokeBorder(colors.brand, lineWidth: 2.5))
                .frame(width: 14, height: 14)
                .shadow(color: colors.brand.opacity(0.35), radius: 3, y: 1)
                .offset(y: track * value - 7)
        }
        .padding(.vertical, margin)
        // Зона касания — полоса 52 pt с запасом сверху и снизу: палец не промахивается,
        // а жест забирает ползунок, не прокрутка страницы
        .frame(width: 52, height: track + margin * 2, alignment: .top)
        .contentShape(Rectangle())
        .highPriorityGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { change(min(max(($0.location.y - margin) / track, 0), 1)) }
                .onEnded { _ in release() }
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Слайд: \(label)")
    }
}
