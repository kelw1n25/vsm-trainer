import SwiftUI
import VSMCore

struct LoginView: View {
    @Bindable var session: SessionViewModel
    @FocusState private var focus: Field?

    enum Field { case number, password }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Image(systemName: "tram.fill").font(.largeTitle).foregroundStyle(Palette.brand)
                        Text("Тренажёр проводника ВСМ").font(.title2.bold())
                        Text("Нештатные ситуации на борту — с таймером и последствиями решений.")
                            .font(.subheadline).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 8)
                }
                Section {
                    TextField("Табельный номер", text: $session.personnelNumber)
                        .focused($focus, equals: .number)
                        .submitLabel(.next)
                        .onSubmit { focus = .password }
                        .accessibilityIdentifier("login.number")
                        .numberPad()
                    SecureField("Пароль", text: $session.password)
                        .focused($focus, equals: .password)
                        .submitLabel(.go)
                        .onSubmit { Task { await session.submit() } }
                        .accessibilityIdentifier("login.password")
                } footer: {
                    Text("Демо: проводник 100001, пароль demo2026. Все данные синтетические.")
                }
                if let error = session.errorMessage {
                    Section {
                        Label(error, systemImage: "exclamationmark.circle").foregroundStyle(.red)
                            .accessibilityIdentifier("login.error")
                    }
                }
                Section {
                    Button {
                        Task { await session.submit() }
                    } label: {
                        HStack {
                            Spacer()
                            if session.isSubmitting { ProgressView() } else { Text("Войти").bold() }
                            Spacer()
                        }
                    }
                    .disabled(!session.canSubmit)
                    .accessibilityIdentifier("login.submit")
                }
            }
            .inlineTitle("Вход")
        }
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
