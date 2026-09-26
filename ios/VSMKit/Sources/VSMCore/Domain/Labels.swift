import Foundation

/// Подписи кодов сервера для интерфейса. Коды компетенций подписывает сервер (`/api/meta`), здесь — только
/// статусы и категории, которые в API приходят кодом.
public enum Labels {
    public static func outcome(_ status: RunStatus) -> String {
        switch status {
        case .success: "Успех"
        case .partial: "Частичный успех"
        case .failure: "Провал"
        case .inProgress: "В процессе"
        }
    }

    public static func category(_ code: String) -> String {
        ["conflict": "Конфликт", "medical": "Медицина", "service": "Сервис", "safety": "Безопасность"][code] ?? code
    }

    public static func role(_ role: Role) -> String {
        switch role {
        case .conductor: "Проводник ВСМ"
        case .instructor: "Инструктор"
        }
    }

    public static func stage(_ code: String) -> String {
        ["boarding": "На посадке", "onboard": "В пути"][code] ?? code
    }

    public static func signed(_ value: Int) -> String {
        value > 0 ? "+\(value)" : "\(value)"
    }

    /// «Смирнов Алексей Андреевич» → «Алексей», как `firstName` сайта.
    public static func firstName(_ fullName: String) -> String {
        let parts = fullName.split(separator: " ")
        return parts.count > 1 ? String(parts[1]) : fullName
    }

    /// Склонение по числу: plural(3, "финал", "финала", "финалов") → «финала».
    public static func plural(_ count: Int, _ one: String, _ few: String, _ many: String) -> String {
        let tens = count % 100
        let units = count % 10
        if (11...14).contains(tens) { return many }
        if units == 1 { return one }
        if (2...4).contains(units) { return few }
        return many
    }
}
