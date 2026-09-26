import Foundation
import Observation

/// Вход, выход и истечение сессии.
@MainActor
@Observable
public final class SessionViewModel {
    public enum State: Equatable {
        case signedOut
        case signedIn(employeeId: Int, fullName: String, role: Role)
    }

    public private(set) var state: State
    public var personnelNumber = ""
    public var password = ""
    public private(set) var isSubmitting = false
    public private(set) var errorMessage: String?

    private let api: APIClient
    private let repository: TrainerRepository

    public init(api: APIClient, tokens: TokenStore, repository: TrainerRepository) {
        self.api = api
        self.repository = repository
        state = tokens.load().map { .signedIn(employeeId: $0.employeeId, fullName: $0.fullName, role: $0.role) } ?? .signedOut
    }

    public var canSubmit: Bool {
        !isSubmitting && !personnelNumber.trimmingCharacters(in: .whitespaces).isEmpty && !password.isEmpty
    }

    public func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }
        do {
            let session = try await api.login(personnelNumber: personnelNumber.trimmingCharacters(in: .whitespaces), password: password)
            password = ""
            state = .signedIn(employeeId: session.employeeId, fullName: session.fullName, role: session.role)
        } catch {
            errorMessage = error.userMessage
        }
    }

    public func signOut() async {
        await api.logout()
        forget()
    }

    /// Сервер отказал в обновлении сессии: данные прежнего сотрудника на устройстве не оставляем.
    public func sessionExpired() {
        guard state != .signedOut else { return }
        forget()
        errorMessage = APIError.sessionExpired.userMessage
    }

    private func forget() {
        repository.clearCache()
        state = .signedOut
    }
}
