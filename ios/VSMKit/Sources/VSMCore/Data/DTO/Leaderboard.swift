import Foundation

public enum LeaderboardScope: String, CaseIterable, Codable, Sendable {
    case brigade, depot, company

    public var title: String {
        switch self {
        case .brigade: "Бригада"
        case .depot: "Депо"
        case .company: "Компания"
        }
    }
}

public enum LeaderboardPeriod: String, CaseIterable, Codable, Sendable {
    case week, month, all

    public var title: String {
        switch self {
        case .week: "Неделя"
        case .month: "Месяц"
        case .all: "Всё время"
        }
    }
}

public struct LeaderboardRow: Codable, Hashable, Identifiable, Sendable {
    public let rank: Int
    public let employeeId: Int
    public let fullName: String
    public let brigade: String
    public let depot: String
    public let levelTitle: String
    public let points: Int
    public let isMe: Bool

    public var id: Int { employeeId }

    enum CodingKeys: String, CodingKey {
        case rank, brigade, depot, points
        case employeeId = "employee_id"
        case fullName = "full_name"
        case levelTitle = "level_title"
        case isMe = "is_me"
    }
}

public struct Leaderboard: Codable, Hashable, Sendable {
    public let scope: LeaderboardScope
    public let period: LeaderboardPeriod
    public let title: String
    public let participants: Int
    public let rows: [LeaderboardRow]

    public var me: LeaderboardRow? { rows.first { $0.isMe } }
}
