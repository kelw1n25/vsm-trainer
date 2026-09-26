import SwiftUI
import VSMCore

/// Фон сцены с плавной сменой (кроссфейд 0.7 с с лёгким приближением), затемнение сверху и снизу.
struct StoryBackground: View {
    let name: String
    @Environment(\.vsmReduceMotion) private var reduce

    var body: some View {
        ZStack {
            Color.clear
                .overlay { AssetImage(name: Assets.exists("bg_\(name)") ? "bg_\(name)" : "bg_vestibule", contentMode: .fill) }
                .clipped()
                .id(name)
                .transition(reduce ? .identity : .opacity.combined(with: .scale(scale: 1.03)))
            // `.story__shade`: сверху 45 % до 18 % высоты, снизу 55 % от 58 %
            LinearGradient(stops: [.init(color: Palette.storyBlack.opacity(0.45), location: 0), .init(color: .clear, location: 0.18)], startPoint: .top, endPoint: .bottom)
            LinearGradient(stops: [.init(color: .clear, location: 0.58), .init(color: Palette.storyBlack.opacity(0.55), location: 1)], startPoint: .top, endPoint: .bottom)
        }
        .animation(reduce ? nil : .easeInOut(duration: 0.7), value: name)
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

/// Как `StoryStage.HANDS` сайта: жест по умолчанию для эмоции.
private let hands = ["thinking": "throat", "pained": "chest", "worried": "chest"]

/// Имя картинки персонажа; если точного сочетания нет — базовое для позы и эмоции (справа — зеркально).
private func sprite(uniform: Bool, character: SceneCharacter, expression: String) -> (name: String, mirror: Bool) {
    let kind = uniform ? "u" : "p"
    let right = character.position == "right"
    let hand = character.hand ?? hands[expression] ?? "down"
    let exact = "sprite_\(kind)_\(character.pose)_\(expression)_\(hand)_\(character.item ?? "none")_\(right ? "r" : "l")"
    if Assets.exists(exact) { return (exact, false) }
    let base = "sprite_\(kind)_\(character.pose)_\(expression)_\(hands[expression] ?? "down")_none_l"
    if Assets.exists(base) { return (base, right) }
    return ("sprite_\(kind)_\(character.pose)_neutral_down_none_l", right)
}

/// Персонажи сцены — `StoryCast`: слева 19 %, по центру 50 %, справа 81 % ширины; стоящие — 32 % высоты,
/// сидящие 26 %, дети 24 %; низ фигуры — на 27 % высоты над краем. Говорящий чуть крупнее, остальные затемнены до 62 %.
struct StoryCast: View {
    let characters: [SceneCharacter]
    let expressions: [String: String]
    let cast: [String: Character]
    let speaker: String?
    let speaking: Bool

    var body: some View {
        GeometryReader { proxy in
            let size = proxy.size
            let someoneSpeaks = speaker.map { id in characters.contains { $0.id == id } } ?? false
            ZStack(alignment: .topLeading) {
                ForEach(characters) { character in
                    let look = cast[character.id]?.look
                    let height = size.height * (character.pose != "stand" ? 0.26 : (look?.child == true ? 0.24 : 0.32))
                    let center = size.width * (character.position == "left" ? 0.19 : (character.position == "right" ? 0.81 : 0.5))
                    Sprite(
                        uniform: character.id == "player" || look?.outfit == "uniform",
                        character: character,
                        expression: expressions[character.id] ?? character.expression,
                        active: !someoneSpeaks || speaker == character.id,
                        talking: speaking && speaker == character.id
                    )
                    .frame(width: height * 72 / 104, height: height)
                    .position(x: center, y: size.height * 0.73 - height / 2)
                }
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

/// Смещение реакции на смену эмоции — доли ширины и высоты фигуры.
private struct Reaction {
    var x: CGFloat = 0
    var y: CGFloat = 0
}

/// Смена эмоции — короткая реакция: злость дрожит, удивление подпрыгивает, радость пружинит, грусть поникает.
private func reactionSteps(_ expression: String) -> (x: [(CGFloat, Double)], y: [(CGFloat, Double)]) {
    switch expression {
    case "angry": ([(-0.025, 0.09), (0.025, 0.09), (-0.025, 0.09), (0.025, 0.09), (0, 0.09)], [(0, 0.01)])
    case "surprised": ([(0, 0.01)], [(-0.05, 0.18), (0, 0.27)])
    case "happy": ([(0, 0.01)], [(-0.02, 0.18), (0, 0.27)])
    case "sad", "pained": ([(0, 0.01)], [(0.015, 0.28), (0, 0.42)])
    default: ([(0, 0.01)], [(0, 0.01)])
    }
}

private struct Sprite: View {
    let uniform: Bool
    let character: SceneCharacter
    let expression: String
    let active: Bool
    let talking: Bool
    @Environment(\.vsmReduceMotion) private var reduce
    @State private var entered = false

    var body: some View {
        let picture = sprite(uniform: uniform, character: character, expression: expression)
        let from: CGFloat = character.position == "left" ? -60 : (character.position == "right" ? 60 : 0)
        GeometryReader { proxy in
            let size = proxy.size
            TimelineView(.animation(paused: !talking || reduce)) { timeline in
                // Говорящий покачивается (`sprite-talk`, 0.25 с туда и обратно)
                let phase = talking && !reduce ? abs(sin(timeline.date.timeIntervalSinceReferenceDate * .pi / 0.5)) : 0
                AssetImage(name: picture.name)
                    .saturation(active ? 1 : 0.7)
                    .colorMultiply(Color(white: active ? 1 : 0.62))
                    // Тень `drop-shadow(0 18px 30px rgba(5,8,15,.35))` — по контуру фигуры
                    .shadow(color: Palette.storyBlack.opacity(0.35), radius: 15, y: 18 * size.height / 540)
                .scaleEffect(x: picture.mirror ? -1 : 1, y: 1)
                .offset(y: -0.012 * phase * size.height)
                .rotationEffect(.degrees(-0.6 * phase), anchor: .bottom)
            }
            .keyframeAnimator(initialValue: Reaction(), trigger: reduce ? "" : expression) { view, value in
                view.offset(x: value.x * size.width, y: value.y * size.height)
            } keyframes: { _ in
                let steps = reactionSteps(expression)
                KeyframeTrack(\.x) {
                    for (value, duration) in steps.x { LinearKeyframe(value, duration: duration) }
                }
                KeyframeTrack(\.y) {
                    for (value, duration) in steps.y { LinearKeyframe(value, duration: duration) }
                }
            }
        }
        .scaleEffect((active ? 1.02 : 0.97) * (entered ? 1 : 0.96), anchor: .bottom)
        .opacity(entered ? 1 : 0)
        .offset(x: entered ? 0 : from)
        .animation(.easeOut(duration: 0.35), value: active)
        .onAppear {
            if reduce { entered = true } else { withAnimation(.site(0.55)) { entered = true } }
        }
    }
}
