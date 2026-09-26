package ru.vsm.trainer.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.LeaderboardPeriod
import ru.vsm.trainer.data.remote.dto.LeaderboardScope
import ru.vsm.trainer.presentation.AnalyticsViewModel
import ru.vsm.trainer.presentation.LeaderboardViewModel
import ru.vsm.trainer.presentation.NotificationsViewModel
import ru.vsm.trainer.presentation.ProfileViewModel
import ru.vsm.trainer.presentation.components.SectionTitle
import ru.vsm.trainer.presentation.components.StaleBanner
import ru.vsm.trainer.presentation.components.StateContent
import ru.vsm.trainer.presentation.components.shortDate
import ru.vsm.trainer.presentation.home.LevelCard

/** Профиль: уровень, компетенции (сильные сверху), достижения, история прохождений. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onAnalytics: () -> Unit, onDebrief: (String) -> Unit, onSignOut: () -> Unit, model: ProfileViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { model.load() }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Выйти из аккаунта?") },
            confirmButton = { TextButton(onClick = onSignOut) { Text("Выйти") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Отмена") } },
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Профиль") }) }) { padding ->
        StateContent(state, model::load) { content, staleSince ->
            val profile = content.profile
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                staleSince?.let { item { StaleBanner(it) } }
                item { LevelCard(profile) }
                item { ListItem(headlineContent = { Text("Аналитика компетенций") }, modifier = Modifier.clickable(onClick = onAnalytics)) }
                item { SectionTitle("Компетенции") }
                items(content.competences) { (title, points) ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row {
                            Text(title, modifier = Modifier.weight(1f))
                            Text("$points", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { (points.coerceAtMost(100)) / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                }
                item { SectionTitle("Достижения") }
                items(profile.achievements, key = { it.code }) {
                    ListItem(
                        headlineContent = { Text(it.title) },
                        supportingContent = { Text(it.description) },
                        trailingContent = { Text(if (it.earnedAt != null) "🏅" else "🔒") },
                        modifier = Modifier.alpha(if (it.earnedAt == null) 0.6f else 1f),
                    )
                }
                item { SectionTitle("Пройдено: ${profile.runsCompleted}") }
                items(profile.history, key = { it.runId }) {
                    ListItem(
                        headlineContent = { Text(it.scenarioTitle) },
                        supportingContent = { Text("${Labels.outcome(it.outcome)} · +${it.xpEarned} XP · ${it.finishedAt.shortDate()}") },
                        modifier = Modifier.clickable { onDebrief(it.runId) },
                    )
                }
                item { TextButton(onClick = { confirmSignOut = true }) { Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(model: LeaderboardViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val scope by model.scope.collectAsStateWithLifecycle()
    val period by model.period.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Рейтинг") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                LeaderboardScope.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == scope,
                        onClick = { model.select(scope = option) },
                        shape = SegmentedButtonDefaults.itemShape(index, LeaderboardScope.entries.size),
                    ) { Text(option.title) }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                LeaderboardPeriod.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == period,
                        onClick = { model.select(period = option) },
                        shape = SegmentedButtonDefaults.itemShape(index, LeaderboardPeriod.entries.size),
                    ) { Text(option.title) }
                }
            }
            StateContent(state, model::load) { board, staleSince ->
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    staleSince?.let { item { StaleBanner(it) } }
                    item { Text("${board.title} · участников: ${board.participants}", style = MaterialTheme.typography.labelLarge) }
                    items(board.rows, key = { it.employeeId }) { row ->
                        ListItem(
                            leadingContent = { Text("${row.rank}", style = MaterialTheme.typography.titleMedium) },
                            headlineContent = { Text(row.fullName, fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("${row.levelTitle} · ${row.brigade}") },
                            trailingContent = { Text("${row.points} XP") },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, model: NotificationsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Уведомления") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") } },
            actions = { TextButton(onClick = model::markAllRead) { Text("Прочитать все") } },
        )
    }) { padding ->
        StateContent(state, model::load) { list, staleSince ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
                staleSince?.let { item { StaleBanner(it) } }
                if (list.items.isEmpty()) item { Text("Уведомлений нет") }
                items(list.items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.title, fontWeight = if (item.read) FontWeight.Normal else FontWeight.Bold) },
                        supportingContent = { Text("${item.body}\n${item.createdAt.shortDate()}") },
                        modifier = Modifier.clickable { model.open(item) },
                    )
                }
            }
        }
    }
}

/** Аналитика: выводы о сильных и проседающих компетенциях, типичные ошибки, прогресс по неделям. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit, model: AnalyticsViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Аналитика") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") } },
        )
    }) { padding ->
        StateContent(state, model::load) { data, staleSince ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                staleSince?.let { item { StaleBanner(it) } }
                item {
                    ListItem(headlineContent = { Text("Пройдено сценариев") }, trailingContent = { Text("${data.totalRuns}") })
                    data.completionRate?.let { ListItem(headlineContent = { Text("Доведено до финала") }, trailingContent = { Text("${(it * 100).toInt()}%") }) }
                    data.avgDecisionSeconds?.let { ListItem(headlineContent = { Text("Среднее время решения") }, trailingContent = { Text("%.1f с".format(it)) }) }
                }
                if (data.strengths.isNotEmpty()) {
                    item { SectionTitle("Сильные стороны") }
                    items(data.strengths) { Text("• $it") }
                }
                if (data.weaknesses.isNotEmpty()) {
                    item { SectionTitle("Стоит подтянуть") }
                    items(data.weaknesses) { Text("• $it") }
                }
                if (data.mistakes.isNotEmpty()) {
                    item { SectionTitle("Типичные ошибки") }
                    items(data.mistakes) { Text(it) }
                }
                data.recommendation?.let {
                    item {
                        SectionTitle("Рекомендация")
                        Text(it.title, style = MaterialTheme.typography.titleMedium)
                        Text(it.reason)
                    }
                }
                item { SectionTitle("XP по неделям") }
                val maxXp = data.progress.maxOfOrNull { it.xp }?.coerceAtLeast(1) ?: 1
                items(data.progress) { week ->
                    Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(week.weekStart.shortDate().substringBefore(","), modifier = Modifier.weight(0.3f))
                        LinearProgressIndicator(progress = { week.xp.toFloat() / maxXp }, modifier = Modifier.weight(0.5f).padding(top = 8.dp))
                        Text("${week.xp}", modifier = Modifier.weight(0.2f))
                    }
                }
            }
        }
    }
}
