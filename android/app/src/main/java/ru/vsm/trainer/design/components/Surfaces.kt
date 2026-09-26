package ru.vsm.trainer.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.design.Dimens
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm

/** Тень сайта `--shadow: 0 8px 30px rgba(24,64,140,.06)` — мягкая, с оттенком бренда. */
@Composable
fun Modifier.vsmShadow(shape: Shape = Shapes.card, elevation: Dp = 10.dp): Modifier {
    val color = Vsm.colors.shadow.copy(alpha = if (Vsm.colors.dark) 0.5f else 0.18f)
    return shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)
}

/**
 * Карточка `.card`: surface, рамка 1, радиус 20, тень, padding 24.
 * [accentStart] — цветная полоса слева (разбор шага, рекомендация, новое уведомление),
 * [accentTop] — сверху (итог разбора).
 */
@Composable
fun VsmCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.cardPadding),
    background: Brush? = null,
    accentStart: Color? = null,
    accentStartWidth: Dp = 6.dp,
    accentTop: Color? = null,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Vsm.colors
    Box(
        modifier
            .fillMaxWidth()
            .vsmShadow()
            .clip(Shapes.card)
            .then(if (background != null) Modifier.background(background) else Modifier.background(colors.surface))
            .border(1.dp, colors.border, Shapes.card),
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
        accentStart?.let { Box(Modifier.align(Alignment.CenterStart).width(accentStartWidth).fillMaxHeight().background(it)) }
        accentTop?.let { Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(4.dp).background(it)) }
    }
}

/** Вложенный блок `radius 16, background soft` — стандарт, шаг ролевой модели, итог финала. */
@Composable
fun SoftBlock(modifier: Modifier = Modifier, color: Color = Vsm.colors.soft, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(Shapes.block).background(color).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * Фон страниц сайта: основной цвет и два мягких пятна света (`body::before` справа сверху,
 * `body::after` слева снизу).
 */
@Composable
fun SiteBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = Vsm.colors
    Box(modifier.background(colors.bg)) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val top = size.width * 1.1f
            drawCircle(
                Brush.radialGradient(listOf(Color(0x33_78AAFF), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width + 60f, -40f), radius = top),
                radius = top, center = androidx.compose.ui.geometry.Offset(size.width + 60f, -40f),
            )
            val bottom = size.width * 1.0f
            drawCircle(
                Brush.radialGradient(listOf(Color(0x21_5AC8DC), Color.Transparent), center = androidx.compose.ui.geometry.Offset(-80f, size.height + 60f), radius = bottom),
                radius = bottom, center = androidx.compose.ui.geometry.Offset(-80f, size.height + 60f),
            )
        }
        content()
    }
}
