package ru.vsm.trainer.presentation.debrief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.DebriefStep
import ru.vsm.trainer.presentation.DebriefViewModel
import ru.vsm.trainer.presentation.components.SectionTitle
import ru.vsm.trainer.presentation.components.StateContent
import ru.vsm.trainer.presentation.theme.VsmColors

/** Обучающий разбор: по каждому шагу — решение, почему оно так сказалось на шкалах, как было лучше. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebriefScreen(onBack: () -> Unit, model: DebriefViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Разбор") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") } },
        )
    }) { padding ->
        StateContent(state, model::load) { debrief, _ ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(debrief.scenarioTitle, style = MaterialTheme.typography.titleLarge)
                    ListItem(headlineContent = { Text("Итог") }, trailingContent = { Text(Labels.outcome(debrief.outcome)) })
                    ListItem(headlineContent = { Text("Лояльность") }, trailingContent = { Text("${debrief.initialLoyalty} → ${debrief.loyalty}") })
                    ListItem(headlineContent = { Text("Безопасность") }, trailingContent = { Text("${debrief.initialSafety} → ${debrief.safety}") })
                    ListItem(headlineContent = { Text("Лучших решений") }, trailingContent = { Text("${debrief.bestDecisions} из ${debrief.decisions}") })
                    if (debrief.timeouts > 0) ListItem(headlineContent = { Text("Истёк таймер") }, trailingContent = { Text("${debrief.timeouts}") })
                    debrief.averageReactionSeconds?.let {
                        ListItem(headlineContent = { Text("Среднее время решения") }, trailingContent = { Text("%.1f с".format(it)) })
                    }
                    ListItem(headlineContent = { Text("Получено") }, trailingContent = { Text("+${debrief.xpEarned} XP") })
                }
                if (debrief.competencePoints.isNotEmpty()) {
                    item { SectionTitle("Компетенции") }
                    items(debrief.competencePoints.entries.sortedByDescending { it.value }.toList()) { (code, points) ->
                        ListItem(headlineContent = { Text(model.competenceTitles[code] ?: code) }, trailingContent = { Text(Labels.signed(points)) })
                    }
                }
                itemsIndexed(debrief.steps) { index, step ->
                    SectionTitle("Шаг ${index + 1}. ${step.situation}")
                    StepCard(step)
                }
                if (debrief.missedSteps.isNotEmpty()) {
                    item { SectionTitle("Что можно было сделать иначе") }
                    items(debrief.missedSteps) { step ->
                        Column {
                            Text(step.bestText.orEmpty(), style = MaterialTheme.typography.titleSmall)
                            step.bestExplanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                items(debrief.situations, key = { it.number }) { situation ->
                    SectionTitle("Стандарт: ситуация №${situation.number}")
                    Text(situation.title, style = MaterialTheme.typography.titleSmall)
                    Text(situation.reaction)
                    // Фразы в справочнике уже в кавычках — показываем как есть
                    situation.phrases.forEach { Text(it, fontStyle = FontStyle.Italic) }
                }
            }
        }
    }
}

@Composable
private fun StepCard(step: DebriefStep) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                step.kind == "timeout" -> Text("Время истекло — решение принято за вас", color = VsmColors.Warning)
                step.chosenText != null -> Text(
                    (if (step.wasBest) "✓ " else "! ") + step.chosenText,
                    color = if (step.wasBest) VsmColors.Positive else VsmColors.Warning,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            step.explanation?.let { Text(it) }
            Text(
                "Лояльность ${Labels.signed(step.loyaltyDelta)} → ${step.loyaltyAfter} · Безопасность ${Labels.signed(step.safetyDelta)} → ${step.safetyAfter}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (step.elapsedSeconds != null && step.timerSeconds != null) {
                Text("Решение за %.1f с из %d".format(step.elapsedSeconds, step.timerSeconds), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
