package ru.vsm.trainer.feature.scenarios

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.core.plural
import ru.vsm.trainer.data.remote.dto.HistoryItem
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.data.remote.dto.ScenarioSummary
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.BackLink
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.Chip
import ru.vsm.trainer.design.components.DifficultyDots
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.OutcomeBadge
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.RouteRow
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.SectionTitle
import ru.vsm.trainer.design.components.Tag
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.assetPainter
import ru.vsm.trainer.design.components.riseIn
import ru.vsm.trainer.design.components.scenarioImageName
import ru.vsm.trainer.design.components.siteDateTime
import ru.vsm.trainer.design.components.vsmShadow

/** Иллюстрация сценария `ScenarioImage` в рамке с радиусом 14 и тенью, кадр заполняется как `slice` на сайте. */
@Composable
fun ScenarioIllustration(scenario: ScenarioSummary, modifier: Modifier = Modifier) {
    Image(
        assetPainter(scenarioImageName(scenario.id, scenario.category)), contentDescription = null,
        modifier = modifier.vsmShadow(Shapes.image, 8.dp).clip(Shapes.image),
        contentScale = ContentScale.Crop,
    )
}

/**
 * Карточка сценария `ScenarioCard` в ширине телефона: иллюстрация сверху, теги, название, маршрут, сложность,
 * «Начать →». Касание карточки открывает страницу сценария, кнопка — сразу прохождение. Появляется по очереди.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenarioCard(scenario: ScenarioSummary, index: Int, onOpen: () -> Unit, onStart: () -> Unit) {
    VsmCard(
        Modifier.riseIn(index).clip(Shapes.card).clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "Сценарий «${scenario.title}»" },
        padding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        spacing = 0.dp,
    ) {
        ScenarioIllustration(scenario, Modifier.fillMaxWidth().height(150.dp))
        FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(Labels.category(scenario.category), category = true)
            Tag("Финалов: ${scenario.endingsTotal}")
        }
        Text(scenario.title, style = VsmType.cardTitle, color = Vsm.colors.text, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp))
        RouteRow(scenario.route, scenario.serviceClass)
        DifficultyDots(scenario.difficulty, Modifier.padding(top = 16.dp))
        PrimaryButton("Начать", onStart, Modifier.padding(top = 16.dp).defaultMinSize(minWidth = 160.dp), arrow = true)
    }
}

/** Раздел «Сценарии»: все сценарии с фильтром по типу ситуации. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenarioListScreen(onScenario: (String) -> Unit, onPlay: (String) -> Unit, model: ScenarioListViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val category by model.category.collectAsStateWithLifecycle()
    Page {
        item { PageTitle("Сценарии") }
        when (val current = state) {
            is ScreenState.Content -> {
                val categories = current.value.map { it.category }.distinct()
                val visible = current.value.filter { category == null || it.category == category }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Chip("Все", category == null, { model.category.value = null })
                        categories.forEach { code -> Chip(Labels.category(code), category == code, { model.category.value = code }) }
                    }
                }
                item { SectionTitle(category?.let(Labels::category) ?: "Все сценарии", visible.size) }
                if (visible.isEmpty()) item { Muted("В этой категории пока нет сценариев.") }
                itemsIndexed(visible, key = { _, it -> it.id }) { index, scenario ->
                    ScenarioCard(scenario, index, onOpen = { onScenario(scenario.id) }, onStart = { onPlay(scenario.id) })
                }
            }
            else -> item { ScreenContent(current, model::load) {} }
        }
    }
}

private val outcomeRank = mapOf(RunStatus.SUCCESS to 3, RunStatus.PARTIAL to 2, RunStatus.FAILURE to 1, RunStatus.IN_PROGRESS to 0)

/** Страница сценария `ScenarioPage`: описание, три особенности, ситуации из справочника, свои попытки. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenarioDetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onMap: (String) -> Unit,
    onSituation: (Int) -> Unit,
    onDebrief: (String) -> Unit,
    model: ScenarioDetailViewModel = hiltViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    LaunchedEffect(Unit) { model.load() }
    Page {
        item { BackLink("Все сценарии", onBack) }
        item {
            ScreenContent(state, model::load) { detail ->
                val scenario = detail.scenario
                if (scenario == null) {
                    VsmCard {
                        PageTitle("Сценарий не найден")
                        PrimaryButton("К списку сценариев", onBack)
                    }
                    return@ScreenContent
                }
                val best = detail.attempts.maxByOrNull { outcomeRank[it.outcome] ?: 0 }
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    VsmCard(spacing = 14.dp) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tag(Labels.category(scenario.category), category = true)
                            Tag("Финалов: ${scenario.endingsTotal}")
                        }
                        PageTitle(scenario.title)
                        RouteRow(scenario.route, scenario.serviceClass)
                        DifficultyDots(scenario.difficulty)
                        Text(scenario.description, style = VsmType.bodyLarge, color = colors.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            Kpi("Попыток", "${detail.attempts.size}")
                            Kpi("Лучший результат", best?.let { Labels.outcome(it.outcome) } ?: "—")
                        }
                        PrimaryButton("Начать сценарий", { onPlay(scenario.id) }, large = true, arrow = true)
                        if (detail.attempts.isNotEmpty()) GhostButton("Развитие истории", { onMap(scenario.id) }, large = true)
                        ScenarioIllustration(scenario, Modifier.fillMaxWidth().aspectRatio(160f / 150f).padding(top = 12.dp))
                    }
                    Feature("🎬 Интерактивная история", "Сцены, диалоги и реакции персонажей. Выбор меняет сюжет — у сценария ${scenario.endingsTotal} ${plural(scenario.endingsTotal, "финал", "финала", "финалов")}.", 0)
                    Feature("⏱ Решения под таймером", "Таймер стартует, когда появились варианты. Не успели — ситуация развивается без вас.", 1)
                    Feature("⚖️ Две шкалы", "Лояльность пассажира и рейтинг безопасности. Падение любой до нуля — провал.", 2)
                    VsmCard {
                        CardTitle("Какие ситуации отрабатываются")
                        Muted("По материалам «Ситуации на борту» — после финала разбор покажет, как действовать по стандарту.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                            scenario.situations.forEach { number ->
                                Box(
                                    Modifier.height(38.dp).clip(Shapes.pill).background(colors.surface)
                                        .border(1.dp, colors.controlBorder, Shapes.pill).clickable(role = Role.Button) { onSituation(number) }
                                        .padding(horizontal = 18.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    Text("Ситуация $number", style = VsmType.bodyStrong.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = colors.text)
                                }
                            }
                        }
                    }
                    if (detail.attempts.isNotEmpty()) {
                        VsmCard {
                            CardTitle("Ваши попытки")
                            HistoryList(detail.attempts, onDebrief, showTitle = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = VsmType.kpiLabel, color = Vsm.colors.muted)
        Text(value, style = VsmType.kpiValue, color = Vsm.colors.heading)
    }
}

@Composable
private fun Feature(title: String, text: String, index: Int) {
    VsmCard(Modifier.riseIn(index, stepMs = 60, durationMs = 450), spacing = 6.dp) {
        Text(title, style = VsmType.bodyBold, color = Vsm.colors.text)
        Muted(text)
    }
}

/** Список `.history`: дата · сценарий · бейдж исхода · XP · «Разбор». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryList(items: List<HistoryItem>, onDebrief: (String) -> Unit, showTitle: Boolean = true, showCategory: Boolean = false) {
    val colors = Vsm.colors
    Column {
        items.forEachIndexed { index, item ->
            FlowRow(
                Modifier.fillMaxWidth().riseIn(index, stepMs = 60, durationMs = 450).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(item.finishedAt.siteDateTime(), style = VsmType.body, color = colors.muted)
                if (showTitle) {
                    Text(
                        item.scenarioTitle + if (showCategory) " · ${Labels.category(item.category)}" else "",
                        style = VsmType.body, color = colors.text, modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutcomeBadge(item.outcome)
                Text("+${item.xpEarned} XP", style = VsmType.body, color = colors.text)
                Text(
                    "Разбор", style = VsmType.body.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = colors.brandText,
                    modifier = Modifier.clickable(role = Role.Button) { onDebrief(item.runId) },
                )
            }
            if (index < items.lastIndex) HorizontalDivider(color = colors.border)
        }
    }
}
