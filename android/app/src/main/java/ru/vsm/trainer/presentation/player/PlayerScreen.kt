package ru.vsm.trainer.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.Final
import ru.vsm.trainer.data.remote.dto.Node
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.Step
import ru.vsm.trainer.presentation.components.ConsequenceCard
import ru.vsm.trainer.presentation.components.LineView
import ru.vsm.trainer.presentation.components.ScaleBar
import ru.vsm.trainer.presentation.theme.VsmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onClose: () -> Unit, onDebrief: (String) -> Unit, model: PlayerViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val requestClose = { if (state.phase == Phase.FINISHED) onClose() else confirmExit = true }

    // Несколько раз в секунду: не пора ли спросить сервер о последствиях истёкшего таймера
    LaunchedEffect(Unit) {
        while (true) {
            model.tick()
            delay(250)
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == Phase.TIMED_OUT) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    BackHandler(onBack = requestClose)

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Выйти из сценария?") },
            text = { Text("Прогресс сохранится на сервере. Если у шага идёт таймер, он не остановится.") },
            confirmButton = {
                TextButton(onClick = {
                    model.exit()
                    onClose()
                }) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Остаться") } },
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(state.run?.scenarioTitle.orEmpty(), maxLines = 1) },
            navigationIcon = { IconButton(onClick = requestClose) { Icon(Icons.Default.Close, contentDescription = "Закрыть сценарий") } },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val run = state.run
            when {
                state.phase == Phase.LOADING -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.phase == Phase.FAILED -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty())
                    Button(onClick = model::load) { Text("Повторить") }
                }
                state.phase == Phase.FINISHED && run?.final != null -> FinalContent(run, run.final, state.lastSteps, onDebrief, onClose)
                run?.node != null -> Playing(run, run.node, state, model)
            }
        }
    }
}

@Composable
private fun Playing(run: RunState, node: Node, state: PlayerUiState, model: PlayerViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(node.id) { listState.animateScrollToItem(0) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ScaleBar("Лояльность пассажира", run.loyalty, state.lastSteps.sumOf { it.loyaltyDelta }, VsmColors.Loyalty)
            ScaleBar("Рейтинг безопасности", run.safety, state.lastSteps.sumOf { it.safetyDelta }, VsmColors.Safety)
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.lastSteps) { ConsequenceCard(it) }
            items(node.dialogue) { LineView(it) }
        }
        state.notice?.let {
            Text(it, color = VsmColors.Warning, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp).testTag("player.notice"))
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state.phase) {
                    Phase.READING -> Button(onClick = model::revealChoices, modifier = Modifier.fillMaxWidth().testTag("player.reveal")) { Text("К решению") }
                    Phase.TIMED_OUT -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Время вышло — ситуация развивается без вас…")
                    }
                    else -> {
                        Countdown(node, model)
                        node.choices.forEach { choice ->
                            OutlinedButton(
                                onClick = { model.choose(choice) },
                                enabled = state.phase == Phase.CHOOSING,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(choice.text, modifier = Modifier.fillMaxWidth()) }
                        }
                        if (state.phase == Phase.SUBMITTING) CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

/** Обратный отсчёт во времени сервера: краснеет, когда остаётся четверть. */
@Composable
private fun Countdown(node: Node, model: PlayerViewModel) {
    val total = node.timerSeconds ?: return
    var remaining by remember(node.id) { mutableDoubleStateOf(model.remainingSeconds() ?: -1.0) }
    LaunchedEffect(node.id) {
        while (true) {
            remaining = model.remainingSeconds() ?: -1.0
            delay(200)
        }
    }
    if (remaining < 0) return
    val fraction = (remaining / total).toFloat()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = "Осталось ${remaining.toInt()} секунд" }) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { fraction },
                color = if (fraction < 0.25f) VsmColors.Negative else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Text("${kotlin.math.ceil(remaining).toInt()}", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text("Решите, пока не истекло время", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Финал: чем закончилось, сколько XP, какие ачивки и уровень, переход к разбору. */
@Composable
private fun FinalContent(run: RunState, final: Final, consequences: List<Step>, onDebrief: (String) -> Unit, onClose: () -> Unit) {
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(Labels.outcome(final.outcome), color = if (final.outcome.name == "FAILURE") VsmColors.Negative else VsmColors.Positive, style = MaterialTheme.typography.labelLarge)
            Text(final.ending, style = MaterialTheme.typography.headlineSmall)
            Text(final.text)
        }
        items(consequences) { ConsequenceCard(it) }
        item {
            ListItem(headlineContent = { Text("Лояльность пассажира") }, trailingContent = { Text("${run.loyalty}") })
            ListItem(headlineContent = { Text("Рейтинг безопасности") }, trailingContent = { Text("${run.safety}") })
            ListItem(headlineContent = { Text("Получено") }, trailingContent = { Text("+${final.xpEarned} XP") })
        }
        run.levelUp?.let { item { Text("Новый уровень: ${it.title}", color = VsmColors.Positive, style = MaterialTheme.typography.titleMedium) } }
        if (run.newAchievements.isNotEmpty()) {
            item { Text("Новые достижения", style = MaterialTheme.typography.titleMedium) }
            items(run.newAchievements) { ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text(it.description) }) }
        }
        item {
            Button(onClick = { onDebrief(run.id) }, modifier = Modifier.fillMaxWidth().testTag("final.debrief")) { Text("Разбор решений") }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
        }
    }
}
