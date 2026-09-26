package ru.vsm.trainer.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.AnimatedNumber
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.CompetenceRadar
import ru.vsm.trainer.design.components.LevelProgress
import ru.vsm.trainer.design.components.LinkAction
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.OutlinedBlock
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.UserAvatar
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.riseIn
import ru.vsm.trainer.design.components.siteDate
import ru.vsm.trainer.feature.scenarios.HistoryList

/** Профиль — `ProfilePage`: шапка с аватаром и уровнем, 4 KPI, радар компетенций, ачивки, история. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onDebrief: (String) -> Unit, onScenarios: () -> Unit, onSettings: () -> Unit, model: ProfileViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val titles by model.titles.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    LaunchedEffect(Unit) { model.load() }
    Page {
        item {
            ScreenContent(state, model::load) { profile ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    VsmCard(background = Brush.linearGradient(listOf(colors.surface, colors.soft)), spacing = 16.dp) {
                        UserAvatar(112.dp, ring = 4.dp)
                        LinkAction("Изменить аватар", onSettings)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PageTitle(profile.fullName)
                            Muted("${Labels.role(profile.role)} · ${profile.brigade} · ${profile.depot}")
                            Muted("Табельный № ${profile.personnelNumber}")
                        }
                        LevelProgress(profile.level)
                    }
                    val earned = profile.achievements.count { it.earnedAt != null }
                    KpiGrid(
                        listOf(
                            Kpi("Общий балл", number = profile.level.xp, suffix = " XP"),
                            Kpi("Пройдено сценариев", number = profile.runsCompleted),
                            Kpi("Достижения", text = "$earned из ${profile.achievements.size}"),
                            Kpi("Уровень", text = "${profile.level.level} · ${profile.level.title}"),
                        ),
                    )
                    VsmCard {
                        CardTitle("Компетенции")
                        CompetenceRadar(profile.competencePoints.map { (code, value) -> (titles[code] ?: code) to value })
                    }
                    VsmCard {
                        CardTitle("Достижения")
                        profile.achievements.forEachIndexed { index, achievement ->
                            val earnedAt = achievement.earnedAt
                            OutlinedBlock(
                                borderColor = if (earnedAt != null) colors.successBorder else colors.border,
                                background = if (earnedAt != null) colors.surface else colors.surfaceMuted,
                                index = index,
                            ) {
                                Text("${if (earnedAt != null) "🏅" else "🔒"} ${achievement.title}", style = VsmType.bodyBold, color = if (earnedAt != null) colors.text else colors.muted)
                                Muted(achievement.description)
                                earnedAt?.let { Muted(it.siteDate()) }
                            }
                        }
                    }
                    VsmCard {
                        CardTitle("История прохождений")
                        if (profile.history.isEmpty()) {
                            Muted("Пока нет завершённых сценариев.")
                            Text(
                                "Выбрать сценарий", style = VsmType.body.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                                color = colors.brandText, modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onScenarios),
                            )
                        } else {
                            HistoryList(profile.history, onDebrief, showCategory = true)
                        }
                    }
                }
            }
        }
    }
}

data class Kpi(val label: String, val number: Int? = null, val suffix: String = "", val text: String? = null, val caption: String? = null)

/** KPI-плитки `.kpis`: на телефоне две колонки, число досчитывает при появлении. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KpiGrid(items: List<Kpi>) {
    val colors = Vsm.colors
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = 2) {
        items.forEachIndexed { index, kpi ->
            VsmCard(Modifier.weight(1f).riseIn(index, stepMs = 60, durationMs = 450), padding = androidx.compose.foundation.layout.PaddingValues(16.dp), spacing = 4.dp) {
                Text(kpi.label, style = VsmType.kpiLabel, color = colors.muted)
                if (kpi.number != null) AnimatedNumber(kpi.number, VsmType.kpiValue, colors.heading, suffix = kpi.suffix)
                else Text(kpi.text.orEmpty(), style = VsmType.kpiValue, color = colors.heading)
                kpi.caption?.let { Muted(it) }
            }
        }
    }
}
