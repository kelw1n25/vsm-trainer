package ru.vsm.trainer.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** «Уменьшить анимацию» из настроек: как `html[data-reduce-motion]` на сайте отключает переходы. */
val LocalReduceMotion = staticCompositionLocalOf { false }

object Vsm {
    val colors: VsmColors
        @Composable get() = LocalVsmColors.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

/**
 * Тема приложения — токены сайта. Material 3 оставлен только для системных контролов
 * (диалоги, выпадающее меню, выделение текста), и его цвета тоже взяты из токенов.
 */
@Composable
fun VsmTheme(dark: Boolean, reduceMotion: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.brand,
        onPrimary = Color.White,
        background = colors.bg,
        surface = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.surface,
        onSurface = colors.text,
        onBackground = colors.text,
        onSurfaceVariant = colors.muted,
        outline = colors.controlBorder,
        error = colors.negative,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalVsmColors provides colors, LocalReduceMotion provides reduceMotion) {
            ProvideTextStyle(TextStyle(fontFamily = Manrope, color = colors.text), content)
        }
    }
}
