package ru.vsm.trainer.feature.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.riseIn
import ru.vsm.trainer.design.components.siteDate

/** «Проводники депо» — `TeamPage`: таблица сайта на телефоне — карточки с теми же колонками. */
@Composable
fun TeamScreen(onMember: (Int) -> Unit, model: TeamViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    Page {
        item { PageTitle("Проводники депо") }
        when (val current = state) {
            is ScreenState.Content -> itemsIndexed(current.value, key = { _, it -> it.employeeId }) { index, member ->
                VsmCard(Modifier.riseIn(index, 40, 400).clip(Shapes.card).clickable(role = Role.Button) { onMember(member.employeeId) }, spacing = 4.dp) {
                    Text(member.fullName, style = VsmType.bodyBold.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = colors.brandText)
                    Cell("Бригада", member.brigade)
                    Cell("Уровень", member.levelTitle)
                    Cell("XP", "${member.xp}")
                    Cell("Последняя активность", member.lastActivityAt?.siteDate() ?: "—")
                    Cell("Проседает", member.weakestCompetence ?: "—")
                }
            }
            else -> item { ScreenContent(current, {}) {} }
        }
    }
}

@Composable
private fun Cell(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = VsmType.small, color = Vsm.colors.muted, modifier = Modifier.weight(1f))
        Text(value, style = VsmType.smallStrong, color = Vsm.colors.text, modifier = Modifier.weight(1f))
    }
}

