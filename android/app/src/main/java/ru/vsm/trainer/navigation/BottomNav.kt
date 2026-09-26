package ru.vsm.trainer.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.vsmShadow

enum class Tab(val route: String, val title: String) {
    HOME("home", "Главная"),
    SCENARIOS("scenarios", "Сценарии"),
    RATING("rating", "Рейтинг"),
    ANALYTICS("analytics", "Аналитика"),
    HANDBOOK("handbook", "Справочник"),
}

/**
 * Навигация сайта (`.nav`): белая пилюля с тенью, текстовые пункты, активный — цвет heading и синяя полоса снизу.
 * На телефоне она внизу экрана — под большой палец; Профиль открывается по аватару в шапке, как в меню сайта.
 */
@Composable
fun BottomNav(current: Tab?, onSelect: (Tab) -> Unit) {
    val colors = Vsm.colors
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).padding(horizontal = 0.dp)
            .vsmShadow(Shapes.nav, 8.dp).clip(Shapes.nav).background(colors.surface).height(58.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == current
            // Ширина пункта — по длине подписи: «Справочник» длиннее «Главной»
            val weight = tab.title.length + 4f
            val underline by animateFloatAsState(if (active) 1f else 0f, tween(350), label = "underline")
            Box(
                Modifier.weight(weight).fillMaxHeight().semantics { selected = active }.clickable(role = Role.Tab) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.title, style = if (active) VsmType.navActive else VsmType.nav, color = if (active) colors.heading else colors.navText,
                    maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 2.dp),
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.8f).height(3.dp)
                        .graphicsLayer { scaleX = underline; alpha = underline }
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(colors.brand),
                )
            }
        }
    }
}
