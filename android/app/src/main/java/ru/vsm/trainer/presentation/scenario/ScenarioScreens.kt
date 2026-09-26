package ru.vsm.trainer.presentation.scenario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.presentation.ScenarioListViewModel
import ru.vsm.trainer.presentation.components.StaleBanner
import ru.vsm.trainer.presentation.components.StateContent
import ru.vsm.trainer.presentation.home.ScenarioRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioListScreen(onScenario: (ScenarioSummary) -> Unit, model: ScenarioListViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Сценарии") }) }) { padding ->
        StateContent(state, model::load) { scenarios, staleSince ->
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                staleSince?.let { item { Column(Modifier.padding(horizontal = 16.dp)) { StaleBanner(it) } } }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Все") }) }
                        items(scenarios.map { it.category }.distinct().sorted()) { code ->
                            FilterChip(selected = category == code, onClick = { category = code }, label = { Text(Labels.category(code)) })
                        }
                    }
                }
                items(scenarios.filter { category == null || it.category == category }, key = { it.id }) { ScenarioRow(it) { onScenario(it) } }
            }
        }
    }
}

/** Карточка сценария. Данные уже есть в каталоге — второй запрос не нужен. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(scenarioId: String, onBack: () -> Unit, onPlay: (String) -> Unit, model: ScenarioListViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val scenario = (state as? ScreenState.Content)?.value?.firstOrNull { it.id == scenarioId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scenario?.title.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") } },
            )
        },
        bottomBar = {
            Button(onClick = { onPlay(scenarioId) }, modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("scenario.start")) {
                Text("Начать сценарий")
            }
        },
    ) { padding ->
        StateContent(state, model::load) { _, _ ->
            Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scenario?.let {
                    Text(it.description, style = MaterialTheme.typography.bodyLarge)
                    ListItem(headlineContent = { Text("Тип") }, trailingContent = { Text(Labels.category(it.category)) })
                    ListItem(headlineContent = { Text("Класс обслуживания") }, trailingContent = { Text(it.serviceClass) })
                    ListItem(headlineContent = { Text("Маршрут") }, trailingContent = { Text(it.route) })
                    ListItem(headlineContent = { Text("Сложность") }, trailingContent = { Text("●".repeat(it.difficulty)) })
                    ListItem(headlineContent = { Text("Финалов") }, trailingContent = { Text("${it.endingsTotal}") })
                    Text(
                        "Решения меняют лояльность пассажира и рейтинг безопасности. На критических шагах идёт таймер: промедление тоже имеет последствия.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
