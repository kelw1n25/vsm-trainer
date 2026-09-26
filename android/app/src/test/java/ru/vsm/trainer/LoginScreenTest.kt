package ru.vsm.trainer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.vsm.trainer.feature.auth.LoginScreen
import ru.vsm.trainer.feature.auth.LoginUiState

/** Compose UI-тест экрана входа на Robolectric — без эмулятора. */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun submitIsEnabledOnlyWithNumberAndPassword() {
        var state by mutableStateOf(LoginUiState())
        var submitted = 0
        compose.setContent {
            ru.vsm.trainer.design.VsmTheme(dark = false, reduceMotion = true) {
            LoginScreen(
                state = state,
                onNumberChange = { state = state.copy(personnelNumber = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onSubmit = { submitted++ },
            )
            }
        }
        compose.onNodeWithTag("login.submit").assertIsNotEnabled()
        compose.onNodeWithTag("login.number").performTextInput("100001")
        compose.onNodeWithTag("login.submit").assertIsNotEnabled()
        compose.onNodeWithTag("login.password").performTextInput("demo2026")
        compose.onNodeWithTag("login.submit").assertIsEnabled().performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun serverErrorIsShown() {
        compose.setContent {
            ru.vsm.trainer.design.VsmTheme(dark = false, reduceMotion = true) {
                LoginScreen(LoginUiState(error = "Неверный табельный номер или пароль"), {}, {}, {})
            }
        }
        compose.onNodeWithTag("login.error").assertIsDisplayed()
    }
}
