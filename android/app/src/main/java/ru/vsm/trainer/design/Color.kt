package ru.vsm.trainer.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Цветовые токены сайта (`frontend/src/styles.css`, `:root` и `html[data-theme="dark"]`).
 * Имена совпадают с CSS-переменными: `--soft-strong` → softStrong. Новых цветов нет.
 */
@Immutable
data class VsmColors(
    val bg: Color,
    val surface: Color,
    val soft: Color,
    val softStrong: Color,
    val text: Color,
    val heading: Color,
    val muted: Color,
    val border: Color,
    val brand: Color,
    val brandText: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val loyalty: Color,
    val safety: Color,
    val track: Color,
    val trackStrong: Color,
    val navText: Color,
    val brandName: Color,
    val divider: Color,
    val caption: Color,
    val heroBg: List<Color>,
    val heroBorder: Color,
    val heroGlow: Color,
    val heroEyebrow: Color,
    val heroText: Color,
    val glass: Color,
    val glassSoft: Color,
    val glassText: Color,
    val surfaceMuted: Color,
    val tagBg: Color,
    val tagText: Color,
    val dotOff: Color,
    val controlBorder: Color,
    val inputBg: Color,
    val brandBorder: Color,
    val successBg: Color,
    val successBorder: Color,
    val partialBg: Color,
    val noticeBorder: Color,
    val failureBg: Color,
    /** Тень карточек `--shadow`: синеватая в светлой теме, чёрная в тёмной. */
    val shadow: Color,
    /** Стекло новеллы `--story-glass`. */
    val storyGlass: Color,
    val storyGlassBorder: Color,
    val storyInk: Color,
    val dark: Boolean,
) {
    val heroBrush: Brush get() = Brush.linearGradient(heroBg)
}

val LightColors = VsmColors(
    bg = Color(0xFFF7FAFE), surface = Color(0xFFFFFFFF), soft = Color(0xFFEEF6FF), softStrong = Color(0xFFE4EEFF),
    text = Color(0xFF121C38), heading = Color(0xFF0F1A38), muted = Color(0xFF6D7891), border = Color(0xFFE8EEF6),
    brand = Color(0xFF146BFF), brandText = Color(0xFF1E5FD6), positive = Color(0xFF16915A), negative = Color(0xFFD6363E),
    warning = Color(0xFFC26A00), loyalty = Color(0xFF2F6FDD), safety = Color(0xFF169C7A), track = Color(0xFFE6ECF5),
    trackStrong = Color(0xFFD5DFED), navText = Color(0xFF1B2645), brandName = Color(0xFF16223F), divider = Color(0xFFD7DFEA),
    caption = Color(0xFF6B7890), heroBg = listOf(Color(0xFFEEF5FF), Color(0xFFE7F1FE), Color(0xFFDCE9FA)),
    heroBorder = Color(0xFFE3EDF9), heroGlow = Color(0xE6FFFFFF), heroEyebrow = Color(0xFF4F86E8), heroText = Color(0xFF5B6680),
    glass = Color(0xE0FFFFFF), glassSoft = Color(0xB8FFFFFF), glassText = Color(0xFF243152), surfaceMuted = Color(0xFFF7FAFF),
    tagBg = Color(0xFFF0F3F7), tagText = Color(0xFF6C7890), dotOff = Color(0xFFC9D3E1), controlBorder = Color(0xFFD8E2F1),
    inputBg = Color(0xFFFBFDFF), brandBorder = Color(0xFFC4D8FF), successBg = Color(0xFFEAF8F0), successBorder = Color(0xFFA6DCBF),
    partialBg = Color(0xFFFFF4E4), noticeBorder = Color(0xFFFFE0B5), failureBg = Color(0xFFFDE8E9),
    shadow = Color(0xFF18408C), storyGlass = Color(0xE6FFFFFF), storyGlassBorder = Color(0xB3FFFFFF), storyInk = Color(0xFF0F1A38),
    dark = false,
)

val DarkColors = VsmColors(
    bg = Color(0xFF0B1222), surface = Color(0xFF131C30), soft = Color(0xFF18233B), softStrong = Color(0xFF1D2A47),
    text = Color(0xFFD6DFEE), heading = Color(0xFFF2F6FC), muted = Color(0xFF8E9AB3), border = Color(0xFF243049),
    brand = Color(0xFF3D86FF), brandText = Color(0xFF82B2FF), positive = Color(0xFF3FCF8E), negative = Color(0xFFFF6B72),
    warning = Color(0xFFF0A040), loyalty = Color(0xFF6FA0FF), safety = Color(0xFF3FCFA8), track = Color(0xFF243049),
    trackStrong = Color(0xFF2C3854), navText = Color(0xFFC9D4E8), brandName = Color(0xFFF2F6FC), divider = Color(0xFF2C3854),
    caption = Color(0xFF8E9AB3), heroBg = listOf(Color(0xFF131F3A), Color(0xFF162543), Color(0xFF1A2C52)),
    heroBorder = Color(0xFF22314F), heroGlow = Color(0x245A8CFF), heroEyebrow = Color(0xFF82B2FF), heroText = Color(0xFFA9B6CD),
    glass = Color(0xE6131C30), glassSoft = Color(0xBF131C30), glassText = Color(0xFFD6DFEE), surfaceMuted = Color(0xFF162036),
    tagBg = Color(0xFF1D2740), tagText = Color(0xFF9AA7C0), dotOff = Color(0xFF33405E), controlBorder = Color(0xFF2C3854),
    inputBg = Color(0xFF0F1729), brandBorder = Color(0xFF2F4F8F), successBg = Color(0xFF10291F), successBorder = Color(0xFF22603F),
    partialBg = Color(0xFF2B2112), noticeBorder = Color(0xFF5A4218), failureBg = Color(0xFF321419),
    shadow = Color(0xFF000000), storyGlass = Color(0xE60F1729), storyGlassBorder = Color(0x407896D2), storyInk = Color(0xFFE6ECF7),
    dark = true,
)

/** Фиксированные цвета сайта, не зависящие от темы. */
object Palette {
    val buttonTop = Color(0xFF2A78FF)
    val buttonBottom = Color(0xFF1561F0)
    val difficultyOn = Color(0xFF1F5FE0)
    val online = Color(0xFF2BD17E)
    val sunTop = Color(0xFFFFD35C)
    val sunBottom = Color(0xFFFFB020)
    val moonTop = Color(0xFF5B8CFF)
    val moonBottom = Color(0xFF2F5FD8)
    val progressStart = Color(0xFF4F8DFF)
    val rankTop1 = Color(0xFFFFE8A3) to Color(0xFF8A5A00)
    val rankTop2 = Color(0xFFE5E9F0) to Color(0xFF4A5568)
    val rankTop3 = Color(0xFFF6D7BF) to Color(0xFF7A4420)
    val storyBlack = Color(0xFF05080F)
    val storyLoyalty = Color(0xFF6FA0FF)
    val storySafety = Color(0xFF3FCFA8)
    val storyNarration = Color(0xFF9FBDF2)
    val deltaUp = Color(0xFF8FE8C6)
    val deltaDown = Color(0xFFFF9AA0)
    val timerHurryStart = Color(0xFFD6363E)
    val timerHurryEnd = Color(0xFFFF6B72)
}

val LocalVsmColors = staticCompositionLocalOf { LightColors }
