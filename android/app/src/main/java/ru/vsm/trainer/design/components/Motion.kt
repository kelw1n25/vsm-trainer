package ru.vsm.trainer.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.vsm.trainer.design.Vsm

/**
 * Появление блока `rise-in` сайта: снизу на 18 и из прозрачности, по очереди с шагом [stepMs].
 * «Уменьшить анимацию» показывает блок сразу.
 */
@Composable
fun Modifier.riseIn(index: Int = 0, stepMs: Int = 70, durationMs: Int = 550): Modifier {
    val reduce = Vsm.reduceMotion
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay((index * stepMs).toLong())
            progress.animateTo(1f, tween(durationMs, easing = SiteEasing))
        }
    }
    val shift = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * shift
    }
}

/** `AnimatedNumber`: число досчитывает до значения за 0.9 с (ease-out cubic). */
@Composable
fun AnimatedNumber(value: Int, style: TextStyle, color: Color, prefix: String = "", suffix: String = "") {
    val reduce = Vsm.reduceMotion
    var shown by remember { mutableIntStateOf(if (reduce) value else 0) }
    LaunchedEffect(value) {
        if (reduce) {
            shown = value
            return@LaunchedEffect
        }
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 900_000_000f).coerceAtMost(1f)
            val eased = 1 - (1 - t) * (1 - t) * (1 - t)
            shown = (value * eased).toInt()
            if (t >= 1f) break
            delay(16)
        }
    }
    Text("$prefix$shown$suffix", style = style, color = color)
}
