import XCTest

/// Основной вход в приложение без backend: экран входа, блокировка пустой формы,
/// понятное сообщение при недоступном сервере вместо зависания или падения.
final class LoginFlowUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testLoginFormValidatesAndReportsUnreachableServer() {
        let app = XCUIApplication()
        app.launch()

        let submit = app.buttons["login.submit"]
        XCTAssertTrue(submit.waitForExistence(timeout: 5))
        XCTAssertFalse(submit.isEnabled, "без табельного номера и пароля вход недоступен")

        app.textFields["login.number"].tap()
        app.textFields["login.number"].typeText("100001")
        app.secureTextFields["login.password"].tap()
        app.secureTextFields["login.password"].typeText("demo2026")
        XCTAssertTrue(submit.isEnabled)
        submit.tap()

        // В CI backend не запущен: приложение должно показать ошибку связи или войти, если сервер доступен
        let error = app.staticTexts.containing(NSPredicate(format: "label CONTAINS 'связи'")).firstMatch
        let home = app.navigationBars["Главная"]
        XCTAssertTrue(error.waitForExistence(timeout: 20) || home.exists)
    }
}
