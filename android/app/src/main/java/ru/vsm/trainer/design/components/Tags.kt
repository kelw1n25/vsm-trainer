package ru.vsm.trainer.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** Тег `.tag` (высота 32, пилюля) и `.tag--category` (синий). */
@Composable
fun Tag(text: String, category: Boolean = false) {
    val colors = Vsm.colors
    Box(
        Modifier.height(32.dp).clip(Shapes.pill).background(if (category) colors.softStrong else colors.tagBg).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (category) VsmType.tagStrong else VsmType.tag, color = if (category) colors.brandText else colors.tagText)
    }
}

/** Чип фильтра `.chip`: 38, рамка control-border, выбранный — фон brand и белый текст. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = Vsm.colors
    val background = animateColorAsState(if (selected) colors.brand else colors.surface, label = "chip").value
    Box(
        modifier
            .height(38.dp)
            .clip(Shapes.pill)
            .background(background)
            .border(1.dp, if (selected) colors.brand else colors.controlBorder, Shapes.pill)
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = VsmType.bodyStrong, color = if (selected) Color.White else colors.text)
    }
}

/** Счётчик `.count-badge` рядом с заголовком секции. */
@Composable
fun CountBadge(count: Int) {
    val colors = Vsm.colors
    Box(
        Modifier.defaultMinSize(minWidth = 28.dp).height(28.dp).clip(Shapes.pill).background(colors.softStrong).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", style = VsmType.tagStrong.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.brandText)
    }
}

/** Бейдж исхода `.outcome--success|partial|failure`. */
@Composable
fun OutcomeBadge(status: RunStatus) {
    val colors = Vsm.colors
    val (background, text) = when (status) {
        RunStatus.SUCCESS -> colors.successBg to colors.positive
        RunStatus.PARTIAL -> colors.partialBg to colors.warning
        RunStatus.FAILURE -> colors.failureBg to colors.negative
        RunStatus.IN_PROGRESS -> colors.tagBg to colors.text
    }
    Box(Modifier.clip(RoundedPill).background(background).padding(horizontal = 12.dp, vertical = 3.dp)) {
        Text(Labels.outcome(status), style = VsmType.smallStrong, color = text)
    }
}

private val RoundedPill = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
