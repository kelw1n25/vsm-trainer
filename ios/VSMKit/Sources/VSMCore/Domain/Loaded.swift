import Foundation

/// Данные экрана и их свежесть: `staleSince` задан, если сети нет и показан сохранённый ответ.
public struct Loaded<Value: Sendable>: Sendable {
    public let value: Value
    public let staleSince: Date?

    public init(value: Value, staleSince: Date? = nil) {
        self.value = value
        self.staleSince = staleSince
    }
}
