package ru.vsm.trainer.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.R
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.SiteBackground
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.VsmTextField
import ru.vsm.trainer.design.components.assetPainter

/**
 * Вход — `LoginPage` сайта: панель с фото ВСМ и затемнением, поверх — карточка формы.
 * Состояние и действия приходят параметрами, поэтому экран проверяется UI-тестом без сервера.
 */
@Composable
fun LoginScreen(state: LoginUiState, onNumberChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onSubmit: () -> Unit) {
    val colors = Vsm.colors
    SiteBackground(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Box(Modifier.fillMaxWidth().clip(Shapes.storyEnd).background(colors.heroBrush)) {
            Image(assetPainter("photo_train_city"), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0x9E080E1E), Color(0x47080E1E), Color(0x00080E1E)))))
            Column(Modifier.padding(horizontal = 24.dp, vertical = 48.dp)) {
                VsmCard(padding = androidx.compose.foundation.layout.PaddingValues(32.dp), spacing = 16.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_logo), null, Modifier.width(46.dp).height(30.dp))
                        Text("ВСМ", style = VsmType.brandName, color = colors.brandName)
                    }
                    Text("Вход для сотрудников", style = VsmType.pageTitle, color = colors.heading)
                    VsmTextField(
                        state.personnelNumber, onNumberChange, label = "Табельный номер", tag = "login.number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    )
                    VsmTextField(
                        state.password, onPasswordChange, label = "Пароль", password = true, tag = "login.password",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    )
                    state.error?.let { Text(it, style = VsmType.body, color = colors.negative, modifier = Modifier.testTag("login.error")) }
                    PrimaryButton(
                        if (state.submitting) "Входим…" else "Войти", onSubmit,
                        Modifier.fillMaxWidth().testTag("login.submit"), large = true, enabled = state.canSubmit,
                    )
                    Text(
                        buildAnnotatedString {
                            append("Демо-доступ (данные синтетические): проводник ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("100001") }
                            append(", инструктор ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("900001") }
                            append(", пароль ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("demo2026") }
                        },
                        style = VsmType.small,
                        color = colors.muted,
                    )
                }
            }
        }
    }
    }
}
