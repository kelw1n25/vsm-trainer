package ru.vsm.trainer.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.riseIn
import ru.vsm.trainer.design.components.siteDateTime

/** Уведомления — `NotificationsPage`: карточки, новые — с синей полосой слева. */
@Composable
fun NotificationsScreen(onChanged: () -> Unit, model: NotificationsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    Page {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageTitle("Уведомления")
                val unread = (state as? ScreenState.Content)?.value?.unread ?: 0
                if (unread > 0) GhostButton("Отметить все прочитанными", { model.readAll(); onChanged() })
            }
        }
        when (val current = state) {
            is ScreenState.Content -> {
                if (current.value.items.isEmpty()) item { Muted("Уведомлений пока нет.") }
                itemsIndexed(current.value.items, key = { _, it -> it.id }) { index, item ->
                    VsmCard(
                        Modifier.riseIn(index, stepMs = 40, durationMs = 400).clip(Shapes.card)
                            .clickable(role = Role.Button) { model.open(item); onChanged() },
                        padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                        accentStart = if (item.read) null else colors.brand,
                        accentStartWidth = 5.dp,
                        spacing = 4.dp,
                    ) {
                        Text(item.title, style = VsmType.bodyBold, color = colors.text)
                        Text(item.body, style = VsmType.body, color = colors.text)
                        Muted(item.createdAt.siteDateTime())
                    }
                }
            }
            else -> item { ScreenContent(current, model::load) {} }
        }
    }
}
