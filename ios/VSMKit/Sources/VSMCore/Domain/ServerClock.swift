import Foundation

/// Часы телефона могут спешить или отставать от сервера. Дедлайн приходит во времени сервера,
/// поэтому остаток считаем с поправкой: смещение = время сервера из ответа − время телефона при получении.
/// Показ — задача клиента; принимать ли ответ, решает сервер.
public struct ServerClock: Sendable {
    public private(set) var offset: TimeInterval = 0

    public init() {}

    public mutating func sync(serverTime: Date, receivedAt: Date) {
        offset = serverTime.timeIntervalSince(receivedAt)
    }

    public func serverNow(at local: Date) -> Date {
        local.addingTimeInterval(offset)
    }

    public func remaining(until deadline: Date, at local: Date) -> TimeInterval {
        max(0, deadline.timeIntervalSince(serverNow(at: local)))
    }
}
