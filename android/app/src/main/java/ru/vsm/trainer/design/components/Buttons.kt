package ru.vsm.trainer.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.R
import ru.vsm.trainer.design.Dimens
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** Нажатие вместо наведения: кнопка чуть уменьшается, как `.button:active { transform: scale(.98) }`. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val reduce = Vsm.reduceMotion
    return animateFloatAsState(if (pressed && !reduce) 0.98f else 1f, label = "press").value
}

/** Основная кнопка `.button`: градиент #2A78FF → #1561F0, радиус 12, 17/700, стрелка справа. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    enabled: Boolean = true,
    arrow: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val brand = Palette.buttonTop
    Row(
        modifier
            .scale(pressScale(source))
            .alpha(if (enabled) 1f else 0.6f)
            .shadow(if (enabled) 8.dp else 0.dp, Shapes.button, ambientColor = brand.copy(alpha = 0.4f), spotColor = brand.copy(alpha = 0.4f))
            .clip(Shapes.button)
            .background(Brush.verticalGradient(listOf(Palette.buttonTop, Palette.buttonBottom)))
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .height(if (large) Dimens.buttonLargeHeight else Dimens.buttonHeight)
            .defaultMinSize(minWidth = 120.dp)
            .padding(horizontal = if (large) 34.dp else 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = VsmType.button, color = Color.White)
        if (arrow) Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Вторичная кнопка `.button--ghost`: фон surface, текст brand-text, рамка control-border. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, large: Boolean = false, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val colors = Vsm.colors
    Row(
        modifier
            .scale(pressScale(source))
            .alpha(if (enabled) 1f else 0.6f)
            .clip(Shapes.button)
            .background(colors.surface)
            .border(1.dp, colors.controlBorder, Shapes.button)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .height(if (large) Dimens.buttonLargeHeight else Dimens.buttonHeight)
            .padding(horizontal = if (large) 34.dp else 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = VsmType.button, color = colors.brandText)
    }
}

/** Ссылка-действие `.link-action` / `.back-link`: brand-text 17/600 с иконками. */
@Composable
fun LinkAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: Int? = null,
    trailing: Int? = null,
    small: Boolean = false,
) {
    val colors = Vsm.colors
    Row(
        modifier.clip(Shapes.tab).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { Icon(painterResource(it), null, tint = colors.brandText, modifier = Modifier.size(if (small) 18.dp else 20.dp)) }
        Text(text, style = if (small) VsmType.link.copy(fontSize = VsmType.tag.fontSize) else VsmType.link, color = colors.brandText)
        trailing?.let { Icon(painterResource(it), null, tint = colors.brandText, modifier = Modifier.size(18.dp)) }
    }
}

/** «← Все сценарии»: `.back-link`. */
@Composable
fun BackLink(text: String, onClick: () -> Unit) = LinkAction(text, onClick, leading = R.drawable.ic_chevron_left, small = true)
