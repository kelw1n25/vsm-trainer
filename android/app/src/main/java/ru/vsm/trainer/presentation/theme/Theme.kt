package ru.vsm.trainer.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Синий ВСМ и цвета шкал. На Android 12+ основная палитра берётся из обоев (Material You). */
object VsmColors {
    val Brand = Color(0xFF1F5FD6)
    val Loyalty = Color(0xFF4078F2)
    val Safety = Color(0xFF21AD78)
    val Positive = Color(0xFF2E9E5B)
    val Negative = Color(0xFFD64545)
    val Warning = Color(0xFFE08A1E)
}

@Composable
fun VsmTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme(primary = Color(0xFF8AB0FF))
        else -> lightColorScheme(primary = VsmColors.Brand)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
