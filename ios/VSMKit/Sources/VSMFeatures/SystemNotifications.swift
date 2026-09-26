import Foundation
import UserNotifications
import VSMCore

/// Системные уведомления о новых сценариях, челленджах, сгорающих баллах и ачивках.
/// Сами уведомления формирует backend; здесь они только показываются баннером.
public final class SystemNotifications: Sendable {
    public init() {}

    public func requestPermission() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
    }

    func show(_ items: [AppNotification]) async {
        let center = UNUserNotificationCenter.current()
        guard await center.notificationSettings().authorizationStatus == .authorized else { return }
        for item in items {
            let content = UNMutableNotificationContent()
            content.title = item.title
            content.body = item.body
            content.sound = .default
            content.userInfo = ["notification_id": item.id]
            // id уведомления сервера как идентификатор: повторный показ заменит баннер, а не продублирует
            try? await center.add(UNNotificationRequest(identifier: "notification-\(item.id)", content: content, trigger: nil))
        }
    }
}
