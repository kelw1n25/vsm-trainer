#if canImport(Security)
import Foundation
import Security

/// Сессия в Keychain: запись доступна только на этом устройстве и только после первой разблокировки —
/// фоновая синхронизация уведомлений работает, а в резервные копии и на другие устройства токен не попадает.
public final class KeychainTokenStore: TokenStore, @unchecked Sendable {
    private let service: String
    private let account = "session"

    public init(service: String = "ru.vsm.trainer") {
        self.service = service
    }

    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account]
    }

    public func load() -> SessionTokens? {
        var request = query
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &item) == errSecSuccess, let data = item as? Data else { return nil }
        return try? JSONDecoder().decode(SessionTokens.self, from: data)
    }

    public func save(_ tokens: SessionTokens) {
        guard let data = try? JSONEncoder().encode(tokens) else { return }
        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        if SecItemUpdate(query as CFDictionary, attributes as CFDictionary) == errSecItemNotFound {
            SecItemAdd(query.merging(attributes) { $1 } as CFDictionary, nil)
        }
    }

    public func clear() {
        SecItemDelete(query as CFDictionary)
    }
}
#endif
