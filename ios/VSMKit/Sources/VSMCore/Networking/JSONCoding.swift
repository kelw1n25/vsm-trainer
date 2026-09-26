import Foundation

/// Даты backend приходят как `2026-09-26T08:01:34.563204Z` — с микросекундами, которые
/// `ISO8601DateFormatter` не разбирает. Дробную часть отрезаем и прибавляем сами.
enum ServerDate {
    static func parse(_ string: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        guard let dot = string.firstIndex(of: ".") else { return formatter.date(from: string) }
        let fractionEnd = string[dot...].dropFirst().firstIndex { !$0.isNumber } ?? string.endIndex
        let fraction = Double("0" + string[dot..<fractionEnd]) ?? 0
        guard let whole = formatter.date(from: String(string[..<dot] + string[fractionEnd...])) else { return nil }
        return whole.addingTimeInterval(fraction)
    }

    /// Обратное преобразование с микросекундами — чтобы дата из кэша совпадала с полученной от сервера.
    static func format(_ date: Date) -> String {
        let seconds = date.timeIntervalSince1970
        var whole = seconds.rounded(.down)
        var micro = Int(((seconds - whole) * 1_000_000).rounded())
        if micro == 1_000_000 {
            whole += 1
            micro = 0
        }
        let base = ISO8601DateFormatter().string(from: Date(timeIntervalSince1970: whole)).dropLast()  // без «Z»
        return base + "." + String(format: "%06d", micro) + "Z"
    }
}

extension JSONDecoder {
    static let api: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let raw = try container.decode(String.self)
            guard let date = ServerDate.parse(raw) else {
                throw DecodingError.dataCorruptedError(in: container, debugDescription: "Неизвестный формат даты: \(raw)")
            }
            return date
        }
        return decoder
    }()
}

extension JSONEncoder {
    static let api: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(ServerDate.format(date))
        }
        return encoder
    }()
}
