import SwiftUI
import VSMCore

/// Вход — `LoginPage` сайта: панель с фото ВСМ и затемнением, поверх — карточка формы.
struct LoginView: View {
    @Bindable var session: SessionViewModel
    @FocusState private var focus: Field?
    @Environment(\.vsm) private var colors

    enum Field { case number, password }

    var body: some View {
        ScrollView {
            VStack {
                Card(padding: 32, spacing: 16) {
                    HStack(spacing: 12) {
                        LogoMark(width: 46)
                        Text("ВСМ").textStyle(VsmType.brandName).foregroundStyle(colors.brandName)
                    }
                    PageTitle("Вход для сотрудников")
                    VStack(alignment: .leading, spacing: 6) {
                        FieldLabel(text: "Табельный номер")
                        TextField("", text: $session.personnelNumber)
                            .focused($focus, equals: .number)
                            .submitLabel(.next)
                            .onSubmit { focus = .password }
                            .numberPad()
                            .modifier(InputFrame(focused: focus == .number))
                            .accessibilityLabel("Табельный номер")
                            .accessibilityIdentifier("login.number")
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        FieldLabel(text: "Пароль")
                        SecureField("", text: $session.password)
                            .focused($focus, equals: .password)
                            .submitLabel(.go)
                            .onSubmit { Task { await session.submit() } }
                            .modifier(InputFrame(focused: focus == .password))
                            .accessibilityLabel("Пароль")
                            .accessibilityIdentifier("login.password")
                    }
                    if let error = session.errorMessage {
                        Text(error).textStyle(VsmType.body).foregroundStyle(colors.negative).accessibilityIdentifier("login.error")
                    }
                    PrimaryButton(title: session.isSubmitting ? "Входим…" : "Войти", large: true, enabled: session.canSubmit) {
                        Task { await session.submit() }
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("login.submit")
                    (Text("Демо-доступ (данные синтетические): проводник ")
                        + Text("100001").bold() + Text(", инструктор ") + Text("900001").bold()
                        + Text(", пароль ") + Text("demo2026").bold())
                        .textStyle(VsmType.small)
                        .foregroundStyle(colors.muted)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 48)
            }
            // Фото ВСМ с затемнением слева — фон панели, размер задаёт карточка формы
            .background {
                ZStack {
                    colors.heroGradient
                    Color.clear.overlay { AssetImage(name: "photo_train_city", contentMode: .fill) }.clipped()
                    LinearGradient(
                        colors: [Color(hex: 0x080E1E, alpha: 0.62), Color(hex: 0x080E1E, alpha: 0.28), Color(hex: 0x080E1E, alpha: 0)],
                        startPoint: .leading, endPoint: .trailing
                    )
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.storyEnd, style: .continuous))
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(SiteBackground())
    }
}

private extension View {
    func numberPad() -> some View {
        #if os(iOS)
        keyboardType(.numberPad).textContentType(.username)
        #else
        self
        #endif
    }
}
