import Foundation
import Observation

public enum ThemeMode: String, Codable, Sendable {
    case light, dark
}

/// Настройки интерфейса, как на сайте: тема (ползунок в шапке и переключатель в настройках)
/// и «Уменьшить анимацию». Без сохранённого выбора тема следует системной.
@MainActor
@Observable
public final class AppPreferences {
    public private(set) var theme: ThemeMode
    public private(set) var reduceMotion: Bool
    private let store: KeyValueStore
    private static let themeKey = "vsm_theme"
    private static let reduceMotionKey = "vsm_reduce_motion"

    public init(store: KeyValueStore, systemDark: Bool) {
        self.store = store
        theme = store.value(ThemeMode.self, forKey: Self.themeKey) ?? (systemDark ? .dark : .light)
        reduceMotion = store.value(Bool.self, forKey: Self.reduceMotionKey) ?? false
    }

    public func setTheme(_ mode: ThemeMode) {
        theme = mode
        store.setValue(mode, forKey: Self.themeKey)
    }

    public func toggleTheme() {
        setTheme(theme == .dark ? .light : .dark)
    }

    public func setReduceMotion(_ enabled: Bool) {
        reduceMotion = enabled
        store.setValue(enabled, forKey: Self.reduceMotionKey)
    }
}
