package ru.vsm.trainer.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.vsm.trainer.R

/** Шрифт сайта — Manrope (SIL OFL), начертания 400–800 встроены в приложение. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Float, weight: FontWeight, line: Float? = null, spacing: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (line ?: (size * 1.5f)).sp,
    letterSpacing = spacing.em,
)

/** Шкала размеров сайта для телефона (CSS px = sp). */
object VsmType {
    /** `.page-title` на телефоне. */
    val pageTitle = style(25.6f, FontWeight.ExtraBold, 32f, -0.01f)
    /** `.section__title` на телефоне. */
    val sectionTitle = style(28f, FontWeight.ExtraBold, 34f, -0.01f)
    val heroTitle = style(24f, FontWeight.ExtraBold, 31f, -0.01f)
    val heroTitleLong = style(21f, FontWeight.ExtraBold, 27f, -0.01f)
    val heroEyebrow = style(15f, FontWeight.Medium, 20f, 0.02f)
    val heroText = style(15f, FontWeight.Normal, 24f)
    /** `h2` в карточках. */
    val h2 = style(20.8f, FontWeight.ExtraBold, 27f)
    val h3 = style(16f, FontWeight.ExtraBold, 22f)
    val cardTitle = style(20.5f, FontWeight.Bold, 30f)
    val body = style(16f, FontWeight.Normal)
    val bodyStrong = style(16f, FontWeight.SemiBold)
    val bodyBold = style(16f, FontWeight.Bold)
    val bodyLarge = style(17f, FontWeight.Normal, 27f)
    val button = style(17f, FontWeight.Bold, 22f)
    val link = style(17f, FontWeight.SemiBold, 22f)
    val tag = style(15f, FontWeight.Medium, 20f)
    val tagStrong = style(15f, FontWeight.SemiBold, 20f)
    val route = style(15.5f, FontWeight.Normal, 22f)
    val kpiLabel = style(14f, FontWeight.SemiBold, 20f)
    val kpiValue = style(20.8f, FontWeight.ExtraBold, 25f)
    val stat = style(22.4f, FontWeight.ExtraBold, 28f)
    val small = style(14f, FontWeight.Normal, 20f)
    val smallStrong = style(14f, FontWeight.SemiBold, 20f)
    val caption = style(13f, FontWeight.Normal, 18f)
    val eyebrow = style(13f, FontWeight.ExtraBold, 18f, 0.14f)
    val nav = style(13f, FontWeight.Medium, 18f)
    val navActive = style(13f, FontWeight.SemiBold, 18f)
    val brandName = style(26f, FontWeight.ExtraBold, 30f, -0.02f)
    val storyName = style(14f, FontWeight.ExtraBold, 18f, 0.08f)
    val storyText = style(17f, FontWeight.Normal, 26f)
    val storyNarration = style(16f, FontWeight.Normal, 25f, 0.01f)
    val storyChoice = style(15f, FontWeight.Normal, 21f)
    val storyBar = style(13f, FontWeight.Normal, 18f)
    val storyIntroTitle = style(28f, FontWeight.ExtraBold, 32f)
    val storyEndTitle = style(26f, FontWeight.ExtraBold, 31f)
}
