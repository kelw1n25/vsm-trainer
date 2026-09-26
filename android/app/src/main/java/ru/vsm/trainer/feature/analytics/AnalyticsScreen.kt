package ru.vsm.trainer.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.vsm.trainer.data.remote.dto.Analytics
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Bullet
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.GradientProgress
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.XpBarChart
import ru.vsm.trainer.feature.profile.Kpi
import ru.vsm.trainer.feature.profile.KpiGrid
import ru.vsm.trainer.feature.scenarios.HistoryList

private fun percent(value: Double?) = value?.let { "${Math.round(it * 100)}%" } ?: "—"

/** Взвешенная по числу прохождений / решений доля по всем категориям — как `overall` сайта. */
private fun overall(data: Analytics, pick: (ru.vsm.trainer.data.remote.dto.CategoryStats) -> Pair<Double?, Int>): Double? {
    var sum = 0.0
    var weight = 0
    data.categories.forEach { category ->
        val (rate, count) = pick(category)
        if (rate != null) {
            sum += rate * count
            weight += count
        }
    }
    return if (weight > 0) sum / weight else null
}

private val week = DateTimeFormatter.ofPattern("dd.MM").withZone(ZoneId.systemDefault())

/** Аналитика — `AnalyticsPage`: KPI, рекомендация, выводы, XP по неделям, навыки, типы ситуаций, история. */
@Composable
fun AnalyticsScreen(onPlay: (String) -> Unit, onDebrief: (String) -> Unit, model: AnalyticsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val history by model.history.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    LaunchedEffect(Unit) { model.load() }
    Page {
        item {
            ScreenContent(state, model::load) { data ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    PageTitle(if (model.own) "Аналитика" else "Аналитика: ${data.fullName}")
                    KpiGrid(
                        listOf(
                            Kpi("Пройдено сценариев", number = data.totalRuns),
                            Kpi("Средний результат", text = percent(overall(data) { it.successRate to it.runs }), caption = "успешных прохождений"),
                            Kpi("Набрано баллов", number = data.progress.sumOf { it.xp }, suffix = " XP", caption = "за 8 недель"),
                            Kpi("Лучшие решения", text = percent(overall(data) { it.bestChoiceRate to it.decisions }), caption = "от всех решений"),
                        ),
                    )
                    data.recommendation?.let { recommendation ->
                        VsmCard(background = Brush.linearGradient(listOf(colors.soft, colors.surface)), accentStart = colors.brand) {
                            Text("РЕКОМЕНДАЦИЯ", style = VsmType.heroEyebrow, color = colors.heroEyebrow)
                            CardTitle(recommendation.title)
                            Muted(recommendation.reason)
                            if (model.own) PrimaryButton("Пройти", { onPlay(recommendation.scenarioId) }, arrow = true)
                        }
                    }
                    if (data.totalRuns == 0) {
                        Muted("Пройдите первый сценарий — здесь появятся выводы о сильных и слабых сторонах.")
                    } else {
                        VsmCard {
                            CardTitle("Выводы")
                            if (data.strengths.isNotEmpty()) Text("💪 Сильные стороны: ${data.strengths.joinToString()}", style = VsmType.body, color = colors.text)
                            if (data.weaknesses.isNotEmpty()) Text("🎯 Стоит подтянуть: ${data.weaknesses.joinToString()}", style = VsmType.body, color = colors.text)
                            if (data.mistakes.isEmpty()) Muted("Типичных ошибок не найдено.")
                            data.mistakes.forEach { Bullet(it, colors.text) }
                        }
                        VsmCard {
                            CardTitle("Прогресс: XP по неделям")
                            XpBarChart(data.progress.map { week.format(it.weekStart) }, data.progress.map { it.xp })
                        }
                        VsmCard(spacing = 16.dp) {
                            CardTitle("Навыки")
                            val max = (data.competences.maxOfOrNull { it.points } ?: 0).coerceAtLeast(1)
                            data.competences.forEach { competence ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(competence.title, style = VsmType.body.copy(fontSize = VsmType.tag.fontSize), color = colors.text, modifier = Modifier.weight(1f))
                                        Text("${competence.points}", style = VsmType.bodyBold, color = colors.text)
                                    }
                                    GradientProgress(competence.points / max.toFloat())
                                }
                            }
                        }
                        // Таблица сайта на 6 колонок на телефоне не читается — те же данные строками «подпись — значение»
                        VsmCard {
                            CardTitle("По типам ситуаций")
                            data.categories.forEachIndexed { index, category ->
                                if (index > 0) HorizontalDivider(color = colors.border)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(category.title, style = VsmType.bodyBold, color = colors.text)
                                    Cell("Прохождений", "${category.runs}")
                                    Cell("Успех", percent(category.successRate))
                                    Cell("Лучшие решения", percent(category.bestChoiceRate))
                                    Cell("Истёк таймер", percent(category.timeoutRate))
                                    Cell("Время решения", category.averageReactionShare?.let { "${percent(it)} таймера" } ?: "—")
                                }
                            }
                        }
                        if (model.own && history.isNotEmpty()) {
                            VsmCard {
                                CardTitle("История прохождения")
                                HistoryList(history, onDebrief)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = VsmType.small, color = Vsm.colors.muted, modifier = Modifier.weight(1f))
        Text(value, style = VsmType.smallStrong, color = Vsm.colors.text)
    }
}
