import Foundation

/// Состояние экрана со списком или карточкой данных.
public enum ScreenState<Value> {
    case loading
    case loaded(Value, staleSince: Date?)
    case failed(String)

    public var value: Value? {
        if case let .loaded(value, _) = self { return value }
        return nil
    }
}

extension Error {
    var userMessage: String {
        (self as? APIError)?.userMessage ?? "Что-то пошло не так. Повторите."
    }
}
