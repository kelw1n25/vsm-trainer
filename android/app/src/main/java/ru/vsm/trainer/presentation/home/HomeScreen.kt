package ru.vsm.trainer.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.presentation.HomeViewModel
import ru.vsm.trainer.presentation.components.SectionTitle
import ru.vsm.trainer.presentation.components.StaleBanner
import ru.vsm.trainer.presentation.components.StateContent

/** Главная: уровень и XP, незавершённый сценарий, рекомендация, место в бригаде, последние ачивки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScenario: (ScenarioSummary) -> Unit,
    onPlay: (String) -> Unit,
    onNotifications: () -> Unit,
    model: HomeViewModel = hiltViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    // Перезагрузка при каждом возвращении на экран: после сценария XP и рейтинг уже другие
    LaunchedEffect(Unit) { model.load() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Главная") }, actions = {
            val unread = (state as? ru.vsm.trainer.core.ScreenState.Content)?.value?.unread ?: 0
            IconButton(onClick = onNotifications) {
                BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Уведомления: $unread непрочитанных")
                }
            }
        })
    }) { padding ->
        StateContent(state, model::load) { content, staleSince ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                staleSince?.let { item { StaleBanner(it) } }
                item { LevelCard(content.profile) }
                content.activeRun?.let { active ->
                    item {
                        ListItem(
                            headlineContent = { Text("Продолжить «${active.title}»") },
                            supportingContent = { Text("Незавершённый сценарий") },
                            leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            modifier = Modifier.clickable { onPlay(active.scenarioId) },
                        )
                    }
                }
                content.recommendation?.let { recommendation ->
                    val scenario = content.scenarios.firstOrNull { it.id == recommendation.scenarioId }
                    item {
                        SectionTitle("Рекомендация")
                        Card(Modifier.fillMaxWidth().clickable(enabled = scenario != null) { scenario?.let(onScenario) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(recommendation.title, style = MaterialTheme.typography.titleMedium)
                                Text(recommendation.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    SectionTitle("Рейтинг за неделю")
                    Text(
                        content.myRank?.let { "${content.rankTitle}: ${it.rank} место · ${it.points} XP" }
                            ?: "Пройдите сценарий, чтобы попасть в рейтинг",
                    )
                }
                val recent = content.profile.earnedAchievements.take(3)
                if (recent.isNotEmpty()) {
                    item { SectionTitle("Последние достижения") }
                    items(recent, key = { it.code }) {
                        ListItem(headlineContent = { Text(it.title) }, leadingContent = { Icon(Icons.Default.Star, contentDescription = null) })
                    }
                }
                item { SectionTitle("Сценарии") }
                items(content.scenarios.take(3), key = { it.id }) { ScenarioRow(it) { onScenario(it) } }
            }
        }
    }
}

@Composable
fun LevelCard(profile: Profile) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(profile.fullName, style = MaterialTheme.typography.titleMedium)
            Text("${profile.brigade} · ${profile.depot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Уровень ${profile.level.level} · ${profile.level.title} · ${profile.level.xp} XP", style = MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(progress = { profile.level.progress }, modifier = Modifier.fillMaxWidth())
            profile.level.nextLevelXp?.let {
                Text("До следующего уровня ${maxOf(0, it - profile.level.xp)} XP", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ScenarioRow(scenario: ScenarioSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(scenario.title) },
        supportingContent = {
            Text("${Labels.category(scenario.category)} · ${scenario.serviceClass} · ${"●".repeat(scenario.difficulty)}")
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
