package ru.vsm.trainer.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.data.remote.dto.Level
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

val SiteEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/** Полоса `.progress`: 10, дорожка track, заливка — градиент #4F8DFF → brand, растёт при появлении (0.9 с). */
@Composable
fun GradientProgress(fraction: Float, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    val reduce = Vsm.reduceMotion
    val shown = remember { Animatable(if (reduce) fraction else 0f) }
    LaunchedEffect(fraction) {
        if (reduce) shown.snapTo(fraction) else shown.animateTo(fraction, tween(900, easing = SiteEasing))
    }
    Box(modifier.fillMaxWidth().height(10.dp).clip(Shapes.pill).background(colors.track)) {
        Box(
            Modifier.fillMaxWidth(shown.value.coerceIn(0f, 1f)).fillMaxHeight().clip(Shapes.pill)
                .background(Brush.horizontalGradient(listOf(Palette.progressStart, colors.brand))),
        )
    }
}

/** `LevelProgress` сайта: «Уровень N · Название», полоса, «XP · до следующего уровня …». */
@Composable
fun LevelProgress(level: Level, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Уровень ${level.level} · ${level.title}", style = VsmType.bodyBold, color = colors.text)
        GradientProgress(level.progress, Modifier.semantics { contentDescription = "Прогресс уровня ${(level.progress * 100).toInt()}%" })
        val tail = level.nextLevelXp?.let { " · до следующего уровня ${it - level.xp} XP" } ?: " · максимальный уровень"
        Text("${level.xp} XP$tail", style = VsmType.body, color = colors.muted)
    }
}

/** «Сложность: ● ● ○» — `.difficulty`. */
@Composable
fun DifficultyDots(level: Int, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "Сложность $level из 3" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Сложность:", style = VsmType.route, color = colors.muted, modifier = Modifier.padding(end = 4.dp))
        repeat(3) { index ->
            Box(Modifier.size(12.dp).clip(CircleShape).background(if (index < level) Palette.difficultyOn else colors.dotOff))
        }
    }
}
