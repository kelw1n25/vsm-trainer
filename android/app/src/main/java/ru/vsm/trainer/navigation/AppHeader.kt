package ru.vsm.trainer.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.R
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.UserAvatar
import ru.vsm.trainer.design.components.vsmShadow

/** Шапка сайта: логотип «ВСМ», ползунок темы, аватар с индикатором «онлайн», бейджем и меню. */
@Composable
fun AppHeader(
    fullName: String,
    roleTitle: String,
    unread: Int,
    dark: Boolean,
    instructor: Boolean,
    onHome: () -> Unit,
    onToggleTheme: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onTeam: () -> Unit,
    onLogout: () -> Unit,
) {
    val colors = Vsm.colors
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clip(Shapes.tab).clickable(onClick = onHome).semantics { contentDescription = "ВСМ — на главную" },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.width(46.dp).height(30.dp))
            Text("ВСМ", style = VsmType.brandName, color = colors.brandName)
        }
        Spacer(Modifier.weight(1f))
        ThemeToggle(dark, onToggleTheme)
        Spacer(Modifier.width(14.dp))
        Box {
            Row(
                Modifier.clip(Shapes.pill).clickable { open = true }.padding(4.dp)
                    .semantics { contentDescription = "Меню пользователя, непрочитанных уведомлений: $unread" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box {
                    UserAvatar(52.dp)
                    OnlineDot(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
                    if (unread > 0) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 2.dp).height(20.dp)
                                .clip(Shapes.pill).background(colors.brand).border(2.dp, colors.surface, Shapes.pill).padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("$unread", style = VsmType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = VsmType.caption.fontSize * 0.85f), color = Color.White) }
                    }
                }
                val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
                Icon(painterResource(R.drawable.ic_chevron_down), null, tint = colors.navText, modifier = Modifier.size(20.dp).rotate(turn))
            }
            DropdownMenu(
                open,
                onDismissRequest = { open = false },
                modifier = Modifier.width(270.dp),
                shape = Shapes.block,
                containerColor = colors.surface,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(fullName, style = VsmType.smallStrong.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.text)
                    Text(roleTitle, style = VsmType.small, color = colors.muted)
                }
                HorizontalDivider(color = colors.border)
                MenuItem("Профиль") { open = false; onProfile() }
                MenuItem("Уведомления", badge = unread) { open = false; onNotifications() }
                MenuItem("Настройки") { open = false; onSettings() }
                if (instructor) MenuItem("Команда") { open = false; onTeam() }
                MenuItem("Выйти", color = colors.negative) { open = false; onLogout() }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, badge: Int = 0, color: Color = Vsm.colors.text, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = VsmType.bodyStrong.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = color) },
        trailingIcon = if (badge > 0) { { ru.vsm.trainer.design.components.CountBadge(badge) } } else null,
        onClick = onClick,
    )
}

/** Зелёная точка «онлайн» с расходящимся кольцом (`ping`, 2.2 с). */
@Composable
private fun OnlineDot(modifier: Modifier) {
    val colors = Vsm.colors
    val ping = rememberInfiniteTransition(label = "ping")
    val t = ping.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "ring")
    val reduce = Vsm.reduceMotion
    Box(modifier.size(12.dp), contentAlignment = Alignment.Center) {
        // Значение читается только при отрисовке слоя — кольцо анимируется без перекомпоновки шапки
        if (!reduce) {
            Box(
                Modifier.size(12.dp).graphicsLayer {
                    val value = t.value
                    scaleX = 1f + 1.3f * value
                    scaleY = 1f + 1.3f * value
                    alpha = 1f - value
                }.border(2.dp, Palette.online, CircleShape),
            )
        }
        Box(Modifier.size(12.dp).clip(CircleShape).background(Palette.online).border(2.dp, colors.surface, CircleShape))
    }
}

/** Ползунок темы: солнце — светлая, луна — тёмная; бегунок едет с «пружинкой», как на сайте. */
@Composable
fun ThemeToggle(dark: Boolean, onToggle: () -> Unit) {
    val colors = Vsm.colors
    val offset by animateDpAsState(if (dark) 34.dp else 0.dp, tween(300), label = "thumb")
    Box(
        Modifier.width(68.dp).height(34.dp).vsmShadow(Shapes.pill, 6.dp).clip(Shapes.pill).background(colors.surface)
            .border(1.dp, colors.border, Shapes.pill)
            .clickable(role = SemanticsRole.Switch, onClick = onToggle)
            .semantics { contentDescription = "Тёмная тема"; stateDescription = if (dark) "включена" else "выключена" },
    ) {
        Box(
            Modifier.padding(3.dp).offset(x = offset).size(26.dp).clip(CircleShape)
                .background(Brush.verticalGradient(if (dark) listOf(Palette.moonTop, Palette.moonBottom) else listOf(Palette.sunTop, Palette.sunBottom))),
        )
        Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sun), null, tint = if (dark) colors.muted else Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_moon), null, tint = if (dark) Color.White else colors.muted, modifier = Modifier.size(16.dp))
        }
    }
}
