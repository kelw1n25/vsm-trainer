import Foundation

public struct AppNotification: Codable, Hashable, Identifiable, Sendable {
    public let id: Int
    public let type: String
    public let title: String
    public let body: String
    public let createdAt: Date
    public let read: Bool

    enum CodingKeys: String, CodingKey {
        case id, type, title, body, read
        case createdAt = "created_at"
    }

    public init(id: Int, type: String, title: String, body: String, createdAt: Date, read: Bool) {
        self.id = id
        self.type = type
        self.title = title
        self.body = body
        self.createdAt = createdAt
        self.read = read
    }
}

public struct NotificationList: Codable, Hashable, Sendable {
    public let unread: Int
    public let items: [AppNotification]
}
