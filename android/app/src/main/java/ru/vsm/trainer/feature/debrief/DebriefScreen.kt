package ru.vsm.trainer.feature.debrief

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.DebriefStep
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.Situation
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Bullet
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.CompetenceList
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.ScaleLineChart
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.SoftBlock
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.riseIn

/** Разбор — `DebriefPage`: итог, статистика, график шкал, разбор каждого шага, стандарты по ситуациям. */
@Composable
fun DebriefScreen(onMap: (String) -> Unit, onScenario: (String) -> Unit, onScenarios: () -> Unit, model: DebriefViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val titles by model.titles.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    Page {
        item {
            ScreenContent(state, model::load) { debrief ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    PageTitle("Разбор: ${debrief.scenarioTitle}")
                    val accent = when (debrief.outcome) {
                        RunStatus.SUCCESS -> colors.positive
                        RunStatus.PARTIAL -> colors.warning
                        RunStatus.FAILURE -> colors.negative
                        RunStatus.IN_PROGRESS -> colors.brand
                    }
                    VsmCard(
                        accentTop = accent,
                        background = if (debrief.outcome == RunStatus.SUCCESS) Brush.verticalGradient(0f to colors.successBg, 0.6f to colors.surface) else null,
                    ) {
                        Text(Labels.outcome(debrief.outcome).uppercase(), style = VsmType.eyebrow.copy(letterSpacing = VsmType.eyebrow.letterSpacing * 0.7f), color = colors.muted)
                        CardTitle(debrief.ending)
                        Text(debrief.finalText, style = VsmType.body, color = colors.text)
                        Stats(
                            listOf(
                                "${debrief.bestDecisions} из ${debrief.decisions}" to "лучших решений",
                                "${debrief.timeouts}" to "истёкших таймеров",
                                (debrief.averageReactionSeconds?.let { "$it с" } ?: "—") to "среднее время решения",
                                "+${debrief.xpEarned}" to "XP",
                            ),
                        )
                        CompetenceList(debrief.competencePoints, titles)
                    }
                    VsmCard {
                        CardTitle("Как менялись шкалы")
                        ScaleLineChart(
                            listOf("Старт") + debrief.steps.indices.map { "Шаг ${it + 1}" },
                            listOf(debrief.initialLoyalty) + debrief.steps.map { it.loyaltyAfter },
                            listOf(debrief.initialSafety) + debrief.steps.map { it.safetyAfter },
                        )
                    }
                    debrief.steps.forEachIndexed { index, step -> StepReview(step, index, titles) }
                    VsmCard {
                        CardTitle("Как действовать по стандарту")
                        Muted("Рекомендации из «Ситуаций на борту» для ситуаций этого сценария.")
                        debrief.situations.forEach { StandardBlock(it) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton("Развитие истории", { onMap(debrief.scenarioId) }, Modifier.fillMaxWidth())
                        GhostButton("Пройти заново", { onScenario(debrief.scenarioId) }, Modifier.fillMaxWidth())
                        GhostButton("Вернуться к сценариям", onScenarios, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Сетка `.stats`: крупное число и подпись. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Stats(items: List<Pair<String, String>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = 2) {
        items.forEach { (value, label) ->
            Column(Modifier.weight(1f)) {
                Text(value, style = VsmType.stat, color = Vsm.colors.text)
                Muted(label)
            }
        }
    }
}

/** Разбор шага `.review`: полоса слева — зелёная (лучший), оранжевая (другой), красная (таймаут). */
@Composable
private fun StepReview(step: DebriefStep, index: Int, titles: Map<String, String>) {
    val colors = Vsm.colors
    val accent = when {
        step.kind == "timeout" -> colors.negative
        step.wasBest -> colors.positive
        else -> colors.warning
    }
    VsmCard(Modifier.riseIn(index, stepMs = 60), accentStart = accent, spacing = 6.dp) {
        val timing = if (step.elapsedSeconds != null && step.timerSeconds != null) " · решение за ${"%.1f".format(step.elapsedSeconds)} из ${step.timerSeconds} с" else ""
        Muted("Шаг ${index + 1}$timing")
        Text(step.situation, style = VsmType.body, color = colors.text)
        Text(
            if (step.kind == "timeout") "⏱ Время истекло, решение не принято" else "Ваш выбор: ${step.chosenText}",
            style = VsmType.bodyBold, color = colors.text,
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (step.loyaltyDelta < 0) colors.negative else colors.positive)) { append("Лояльность ${Labels.signed(step.loyaltyDelta)}") }
                append(" · ")
                withStyle(SpanStyle(color = if (step.safetyDelta < 0) colors.negative else colors.positive)) { append("Безопасность ${Labels.signed(step.safetyDelta)}") }
            },
            style = VsmType.body,
        )
        CompetenceList(step.competences, titles)
        step.explanation?.let { Muted(it) }
        if (!step.wasBest && step.bestText != null) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(Shapes.button).background(colors.successBg)
                    .border(1.dp, colors.successBorder, Shapes.button).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Лучший вариант: ${step.bestText}", style = VsmType.bodyBold, color = colors.text)
                step.bestExplanation?.let { Text(it, style = VsmType.body, color = colors.text) }
            }
        }
    }
}

/** Стандарт ситуации `.standard`: номер и название, реакция, фразы синим. */
@Composable
fun StandardBlock(situation: Situation, extra: @Composable () -> Unit = {}) {
    val colors = Vsm.colors
    SoftBlock {
        Text("${situation.number}. ${situation.title}", style = VsmType.h3, color = colors.heading)
        Text(situation.reaction, style = VsmType.body, color = colors.text)
        situation.phrases.forEach { Bullet(it, colors.brandText) }
        extra()
    }
}

