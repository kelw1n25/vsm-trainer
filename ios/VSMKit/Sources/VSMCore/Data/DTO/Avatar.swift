import Foundation

/// Аватар сотрудника — конструктор из деталей формы, а не фотография (152-ФЗ).
/// Варианты те же, что принимает сервер (`backend/app/profiles/avatar.py`); экраны лишь рисуют выбор.
public struct Avatar: Codable, Hashable, Sendable {
    public enum Background: String, Codable, CaseIterable, Sendable { case blue, mint, sand, lilac, coral, night }
    public enum Headwear: String, Codable, CaseIterable, Sendable { case cap, none }
    public enum Tie: String, Codable, CaseIterable, Sendable { case red, blue, green, graphite }

    public var background: Background
    public var headwear: Headwear
    public var tie: Tie

    /// По умолчанию — голубой фон, фуражка, красный галстук: как у всех до настройки.
    public init(background: Background = .blue, headwear: Headwear = .cap, tie: Tie = .red) {
        self.background = background
        self.headwear = headwear
        self.tie = tie
    }
}
