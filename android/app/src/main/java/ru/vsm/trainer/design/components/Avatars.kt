package ru.vsm.trainer.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** Аватар проводника — та же картинка, что на сайте (`Avatar`), с белым кольцом и тенью. */
@Composable
fun UserAvatar(size: Dp, modifier: Modifier = Modifier, ring: Dp = 3.dp) {
    Box(modifier.size(size).vsmShadow(CircleShape, 6.dp).clip(CircleShape).background(Vsm.colors.surface).border(ring, Vsm.colors.surface, CircleShape)) {
        Image(assetPainter("avatar"), contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
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
