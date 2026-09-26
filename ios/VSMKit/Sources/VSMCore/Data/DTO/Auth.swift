import Foundation

public enum Role: String, Codable, Sendable {
    case conductor
    case instructor
}

struct LoginRequest: Encodable {
    let personnelNumber: String
    let password: String

    enum CodingKeys: String, CodingKey {
        case personnelNumber = "personnel_number"
        case password
    }
}

struct RefreshRequest: Encodable {
    let refreshToken: String

    enum CodingKeys: String, CodingKey {
        case refreshToken = "refresh_token"
    }
}

/// Ответ `POST /api/auth/login` и `/refresh`: пара токенов и кто вошёл.
public struct SessionTokens: Codable, Equatable, Sendable {
    public let accessToken: String
    public let refreshToken: String
    public let expiresIn: Int
    public let employeeId: Int
    public let fullName: String
    public let role: Role

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case expiresIn = "expires_in"
        case employeeId = "employee_id"
        case fullName = "full_name"
        case role
    }

    public init(accessToken: String, refreshToken: String, expiresIn: Int, employeeId: Int, fullName: String, role: Role) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.expiresIn = expiresIn
        self.employeeId = employeeId
        self.fullName = fullName
        self.role = role
    }
}
