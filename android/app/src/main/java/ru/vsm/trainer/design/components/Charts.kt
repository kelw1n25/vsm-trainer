package ru.vsm.trainer.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import ru.vsm.trainer.design.Manrope
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

private val axis = TextStyle(fontFamily = Manrope, fontSize = 12.sp)
private val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 9f))

@Composable
private fun appear(): Float {
    val reduce = Vsm.reduceMotion
    val value = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) value.animateTo(1f, tween(900, easing = SiteEasing)) }
    return value.value
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, color: Color, at: Offset, alignRight: Boolean = false, center: Boolean = false) {
    val layout = measurer.measure(text, axis.copy(color = color))
    val x = when {
        alignRight -> at.x - layout.size.width
        center -> at.x - layout.size.width / 2f
        else -> at.x
    }
    drawText(layout, topLeft = Offset(x, at.y - layout.size.height / 2f))
}

/** `ScaleChart` разбора: как менялись лояльность и безопасность по шагам (0–100). */
@Composable
fun ScaleLineChart(labels: List<String>, loyalty: List<Int>, safety: List<Int>) {
    val colors = Vsm.colors
    val measurer = rememberTextMeasurer()
    val progress = appear()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = "График шкал: лояльность ${loyalty.lastOrNull()}, безопасность ${safety.lastOrNull()}" }) {
            val left = 34.dp.toPx()
            val bottom = size.height - 22.dp.toPx()
            val top = 8.dp.toPx()
            val right = size.width - 12.dp.toPx()
            fun y(value: Int) = bottom - (bottom - top) * value / 100f
            fun x(index: Int) = if (labels.size < 2) left else left + (right - left) * index / (labels.size - 1)
            for (tick in listOf(0, 25, 50, 75, 100)) {
                drawLine(colors.border, Offset(left, y(tick)), Offset(right, y(tick)), 1.dp.toPx(), pathEffect = dash)
                label(measurer, "$tick", colors.muted, Offset(left - 6.dp.toPx(), y(tick)), alignRight = true)
            }
            labels.forEachIndexed { index, text ->
                drawLine(colors.border, Offset(x(index), top), Offset(x(index), bottom), 1.dp.toPx(), pathEffect = dash)
                label(measurer, text, colors.muted, Offset(x(index), size.height - 9.dp.toPx()), center = true)
            }
            for ((series, color) in listOf(safety to colors.safety, loyalty to colors.loyalty)) {
                val path = Path()
                series.forEachIndexed { index, value ->
                    val point = Offset(x(index), bottom - (bottom - y(value)) * progress)
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, color, style = Stroke(2.dp.toPx()))
                series.forEachIndexed { index, value ->
                    drawCircle(Color.White, 3.dp.toPx(), Offset(x(index), bottom - (bottom - y(value)) * progress))
                    drawCircle(color, 3.dp.toPx(), Offset(x(index), bottom - (bottom - y(value)) * progress), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            LegendItem("Безопасность", colors.safety)
            LegendItem("Лояльность", colors.loyalty)
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(text, style = VsmType.small, color = color)
    }
}

/** «Прогресс: XP по неделям» — столбцы brand со скруглением сверху, как BarChart сайта. */
@Composable
fun XpBarChart(labels: List<String>, values: List<Int>) {
    val colors = Vsm.colors
    val measurer = rememberTextMeasurer()
    val progress = appear()
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val step = niceStep(max)
    val top = ((max + step - 1) / step) * step
    Canvas(Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = "XP по неделям: ${values.joinToString()}" }) {
        val left = 40.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val chartTop = 8.dp.toPx()
        val right = size.width
        fun y(value: Int) = bottom - (bottom - chartTop) * value / top.toFloat()
        var tick = 0
        while (tick <= top) {
            drawLine(colors.border, Offset(left, y(tick)), Offset(right, y(tick)), 1.dp.toPx(), pathEffect = dash)
            label(measurer, "$tick", colors.muted, Offset(left - 6.dp.toPx(), y(tick)), alignRight = true)
            tick += step
        }
        val slot = (right - left) / values.size.coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val width = slot * 0.7f
            val x = left + slot * index + (slot - width) / 2
            val height = (bottom - y(value)) * progress
            if (height > 0) {
                drawRoundRect(colors.brand, Offset(x, bottom - height), Size(width, height), CornerRadius(6.dp.toPx()))
                drawRect(colors.brand, Offset(x, bottom - minOf(height, 6.dp.toPx())), Size(width, minOf(height, 6.dp.toPx())))
            }
            label(measurer, labels[index], colors.muted, Offset(x + width / 2, size.height - 9.dp.toPx()), center = true)
        }
    }
}

private fun niceStep(max: Int): Int = listOf(1, 2, 5, 10, 25, 50, 100, 150, 200, 250, 500, 1000).first { max / it <= 4 }

/** Радар компетенций профиля: сетка-пятиугольник, заливка brand с прозрачностью .25. */
@Composable
fun CompetenceRadar(items: List<Pair<String, Int>>) {
    val colors = Vsm.colors
    val measurer = rememberTextMeasurer()
    val progress = appear()
    val max = (items.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(300.dp).semantics { contentDescription = items.joinToString { "${it.first}: ${it.second}" } }) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) * 0.30f
        fun point(index: Int, fraction: Float): Offset {
            val angle = -PI / 2 + 2 * PI * index / items.size
            return Offset(center.x + (radius * fraction * cos(angle)).toFloat(), center.y + (radius * fraction * sin(angle)).toFloat())
        }
        for (ring in 1..5) {
            val path = Path()
            items.indices.forEach { index ->
                val p = point(index, ring / 5f)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, colors.border, style = Stroke(1.dp.toPx()))
        }
        items.indices.forEach { index -> drawLine(colors.border, center, point(index, 1f), 1.dp.toPx()) }
        val shape = Path()
        items.forEachIndexed { index, (_, value) ->
            val p = point(index, value / max.toFloat() * progress)
            if (index == 0) shape.moveTo(p.x, p.y) else shape.lineTo(p.x, p.y)
        }
        shape.close()
        drawPath(shape, colors.brand.copy(alpha = 0.25f))
        drawPath(shape, colors.brand, style = Stroke(1.5.dp.toPx()))
        items.forEachIndexed { index, (title, _) ->
            val p = point(index, 1.18f)
            val layout = measurer.measure(title, axis.copy(color = colors.muted), constraints = androidx.compose.ui.unit.Constraints(maxWidth = (size.width * 0.42f).toInt()))
            val x = when {
                p.x < center.x - 4 -> p.x - layout.size.width
                p.x > center.x + 4 -> p.x
                else -> p.x - layout.size.width / 2f
            }
            drawText(layout, topLeft = Offset(x.coerceIn(0f, size.width - layout.size.width), p.y - layout.size.height / 2f))
        }
    }
}
