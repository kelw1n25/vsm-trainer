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

    public static func signed(_ value: Int) -> String {
        value > 0 ? "+\(value)" : "\(value)"
    }
}
