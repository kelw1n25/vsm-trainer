package ru.vsm.trainer.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vsm.trainer.data.remote.dto.Avatar
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** Аватар вошедшего сотрудника — задаётся навигацией из репозитория, чтобы новый выбор был виден сразу везде. */
val LocalMyAvatar = staticCompositionLocalOf { Avatar() }

/** Варианты конструктора — те же, что у сайта (`frontend/src/avatar.ts`) и сервера (`profiles/avatar.py`). */
object AvatarOptions {
    class Background(val label: String, val top: Color, val bottom: Color)
    class Tie(val label: String, val color: Color)

    val backgrounds = linkedMapOf(
        "blue" to Background("Голубой", Color(0xFFEAF2FF), Color(0xFFC9DDFB)),
        "mint" to Background("Мятный", Color(0xFFE6F7EF), Color(0xFFBFE8D3)),
        "sand" to Background("Песочный", Color(0xFFFFF4E4), Color(0xFFF5D9AE)),
        "lilac" to Background("Сиреневый", Color(0xFFF1ECFF), Color(0xFFD8CCF7)),
        "coral" to Background("Коралловый", Color(0xFFFDECEC), Color(0xFFF6C6C6)),
        "night" to Background("Ночной", Color(0xFF2A3A5E), Color(0xFF16223F)),
    )
    val headwear = linkedMapOf("cap" to "Фуражка", "none" to "Без головного убора")
    val ties = linkedMapOf(
        "red" to Tie("Красный", Color(0xFFD23A3A)),
        "blue" to Tie("Синий", Color(0xFF2F6FDD)),
        "green" to Tie("Зелёный", Color(0xFF169C7A)),
        "graphite" to Tie("Графитовый", Color(0xFF3A4460)),
    )
}

private val Mannequin = Color(0xFFC9C9C9)
private val MannequinShade = Color(0xFFA4A4A4)
private val Gold = Color(0xFFE7C15A)

/** Аватар проводника — манекен в форме, как `Avatar` сайта, с белым кольцом и тенью; детали — из конструктора. */
@Composable
fun UserAvatar(size: Dp, modifier: Modifier = Modifier, ring: Dp = 3.dp, avatar: Avatar = LocalMyAvatar.current) {
    Box(modifier.size(size).vsmShadow(CircleShape, 6.dp).clip(CircleShape).background(Vsm.colors.surface).border(ring, Vsm.colors.surface, CircleShape)) {
        AvatarDrawing(avatar, Modifier.size(size).clip(CircleShape))
    }
}

/** Рисунок аватара в сетке 64 × 64 — те же фигуры и координаты, что в SVG сайта. */
@Composable
fun AvatarDrawing(avatar: Avatar, modifier: Modifier) {
    val background = AvatarOptions.backgrounds[avatar.background] ?: AvatarOptions.backgrounds.getValue("blue")
    val tie = AvatarOptions.ties[avatar.tie] ?: AvatarOptions.ties.getValue("red")
    Canvas(modifier) {
        scale(size.width / 64f, pivot = Offset.Zero) {
            drawRect(Brush.verticalGradient(listOf(background.top, background.bottom), 0f, 64f), size = Size(64f, 64f))
            val body = Path().apply {
                moveTo(11f, 64f)
                cubicTo(12f, 52f, 20f, 46f, 32f, 46f)
                cubicTo(44f, 46f, 52f, 52f, 53f, 64f)
                close()
            }
            drawPath(body, Brush.horizontalGradient(0f to Mannequin, 0.55f to Mannequin, 1f to MannequinShade, startX = 11f, endX = 53f))
            val tiePath = Path().apply {
                moveTo(31f, 47f); lineTo(33f, 47f); lineTo(34.5f, 57f); lineTo(32f, 60f); lineTo(29.5f, 57f); close()
            }
            drawPath(tiePath, tie.color)
            drawRoundRect(Gold, Offset(38f, 52f), Size(6f, 4f), CornerRadius(1f))
            drawRoundRect(Mannequin, Offset(28.5f, 37f), Size(7f, 10f), CornerRadius(3.5f))
            drawCircle(
                Brush.radialGradient(0f to Mannequin, 0.62f to Mannequin, 1f to MannequinShade, center = Offset(29.76f, 24.2f), radius = 20.16f),
                radius = 14f, center = Offset(32f, 27f),
            )
            if (avatar.headwear == "cap") {
                // Фуражка анфас: тулья, околыш с кантом, кокарда и симметричный козырёк
                val crown = Path().apply {
                    moveTo(18.6f, 19.6f)
                    cubicTo(15f, 15.4f, 20.4f, 6.6f, 32f, 6.2f)
                    cubicTo(43.6f, 6.6f, 49f, 15.4f, 45.4f, 19.6f)
                    close()
                }
                drawPath(crown, Color(0xFF34487A))
                drawRoundRect(Color(0xFF1E2848), Offset(18.4f, 17.4f), Size(27.2f, 4f), CornerRadius(1.3f))
                drawLine(Gold, Offset(18.6f, 17.6f), Offset(45.4f, 17.6f), strokeWidth = 0.8f)
                drawOval(Gold, Offset(29.6f, 13.3f), Size(4.8f, 5.4f))
                drawOval(Color(0xFFC9483E), Offset(31f, 14.8f), Size(2f, 2.4f))
                val visor = Path().apply {
                    moveTo(17.8f, 21.2f)
                    cubicTo(25f, 23f, 39f, 23f, 46.2f, 21.2f)
                    cubicTo(44.6f, 25f, 39f, 26f, 32f, 26f)
                    cubicTo(25f, 26f, 19.4f, 25f, 17.8f, 21.2f)
                    close()
                }
                drawPath(visor, Color(0xFF141B30))
            }
        }
    }
}

/** Образец цвета для конструктора: круг с кольцом выбора. */
@Composable
fun Swatch(brush: Brush, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Canvas(modifier.size(44.dp)) {
        val radius = 18.dp.toPx()
        drawCircle(brush, radius)
        drawCircle(colors.controlBorder, radius, style = Stroke(1.dp.toPx()))
        if (selected) drawCircle(colors.brand, radius + 4.dp.toPx(), style = Stroke(2.dp.toPx()))
    }
}

/** `InitialsAvatar`: инициалы «ИФ» на градиенте soft-strong → brand-border. */
@Composable
fun InitialsAvatar(fullName: String, size: Dp) {
    val colors = Vsm.colors
    val parts = fullName.split(" ")
    val initials = (parts.getOrNull(1)?.take(1).orEmpty() + parts.getOrNull(0)?.take(1).orEmpty())
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(colors.softStrong, colors.brandBorder))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = VsmType.bodyBold.copy(fontSize = (size.value * 0.38f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = colors.brandText)
    }
}
