package ru.vsm.trainer.feature.scenarios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.MapEnding
import ru.vsm.trainer.data.remote.dto.MapNode
import ru.vsm.trainer.data.remote.dto.StoryMap
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.BackLink
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.VsmCard

private const val ROOT = "root"

/**
 * Развитие истории — `StoryMapPage`: какие развилки сотрудник уже исследовал и какие финалы открыл.
 * Закрытое сервер отдаёт без текста — подсмотреть его нельзя.
 */
@Composable
fun StoryMapScreen(onBack: () -> Unit, onPlay: (String) -> Unit, model: StoryMapViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.load() }
    Page {
        item { BackLink("К сценарию", onBack) }
        item { PageTitle("Развитие истории") }
        item {
            ScreenContent(state, model::load) { content ->
                val map = content.map
                val reached = map.endings.count { it.reached }
                val owners = remember(map) { firstAppearance(map) }
                val nodes = remember(map) { map.nodes.associateBy { it.id } }
                val endings = remember(map) { map.endings.associateBy { it.id } }
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Muted(
                        "«${content.title}» — пройдено раз: ${map.playthroughs}, исследовано решений: ${map.choicesExplored} из ${map.choicesTotal}, " +
                            "открыто финалов: $reached из ${map.endings.size}. Неисследованные ветки остаются закрытыми.",
                    )
                    VsmCard(spacing = 8.dp) {
                        Text("НАЧАЛО", style = VsmType.eyebrow.copy(letterSpacing = VsmType.eyebrow.letterSpacing * 0.7f), color = Vsm.colors.brandText)
                        Branch(map.startNode, ROOT, nodes, endings, owners)
                    }
                    VsmCard {
                        CardTitle("Финалы")
                        map.endings.forEach { EndingTile(it) }
                        PrimaryButton("Пройти заново", { onPlay(model.scenarioId) }, Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

/** Узел разворачивается там, где встретился впервые при обходе в глубину; в других местах — «сходится с веткой». */
private fun firstAppearance(map: StoryMap): Map<String, String> {
    val nodes = map.nodes.associateBy { it.id }
    val owners = mutableMapOf<String, String>()
    fun visit(nodeId: String, via: String) {
        if (nodeId in owners || nodeId !in nodes) return
        owners[nodeId] = via
        val node = nodes.getValue(nodeId)
        node.choices.forEach { choice -> choice.next?.let { visit(it, "$nodeId:${choice.id}") } }
        node.timeoutNext?.let { visit(it, "$nodeId:timeout") }
    }
    visit(map.startNode, ROOT)
    return owners
}

@Composable
private fun Branch(nodeId: String, via: String, nodes: Map<String, MapNode>, endings: Map<String, MapEnding>, owners: Map<String, String>) {
    val colors = Vsm.colors
    endings[nodeId]?.let { ending ->
        Text(
            buildAnnotatedString {
                append("Финал: ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ending.ending.orEmpty()) }
            },
            style = VsmType.body, color = colors.text,
            modifier = Modifier.padding(vertical = 4.dp).clip(Shapes.image).background(colors.tagBg).padding(horizontal = 12.dp, vertical = 8.dp),
        )
        return
    }
    val node = nodes[nodeId] ?: return
    if (owners[nodeId] != via) {
        Text("→ сходится с веткой «${node.situation}»", style = VsmType.small, color = colors.muted, modifier = Modifier.padding(vertical = 4.dp))
        return
    }
    Column(Modifier.padding(top = 6.dp)) {
        Text(node.situation, style = VsmType.bodyBold, color = colors.heading, modifier = Modifier.padding(bottom = 8.dp))
        // Линия ветвей слева, как `border-left` на сайте. Рисуется поверх уже измеренной колонки: вложенные
        // intrinsic-измерения в рекурсивном дереве стоили бы экспоненциально много
        val line = colors.border
        Row {
            Column(Modifier.drawBehind { drawLine(line, Offset(1.dp.toPx(), 0f), Offset(1.dp.toPx(), size.height), 2.dp.toPx()) }) {
                node.choices.forEach { choice ->
                    BranchItem(explored = choice.explored) {
                        if (choice.explored) {
                            Text("✓ ${choice.text}", style = VsmType.body, color = colors.text)
                            choice.next?.let { Branch(it, "${node.id}:${choice.id}", nodes, endings, owners) }
                        } else {
                            Text("🔒 Ветка не исследована", style = VsmType.body, color = colors.muted)
                        }
                    }
                }
                if (node.timer) {
                    BranchItem(explored = node.timeoutExplored) {
                        if (node.timeoutExplored) {
                            Text("⏱ Время на решение истекло", style = VsmType.body, color = colors.text)
                            node.timeoutNext?.let { Branch(it, "${node.id}:timeout", nodes, endings, owners) }
                        } else {
                            Text("🔒 Ветка не исследована", style = VsmType.body, color = colors.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchItem(explored: Boolean, content: @Composable () -> Unit) {
    val colors = Vsm.colors
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.padding(top = 11.dp).width(14.dp).height(2.dp).background(if (explored) colors.brand else colors.border))
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun EndingTile(ending: MapEnding) {
    val colors = Vsm.colors
    Column(
        Modifier.fillMaxWidth().clip(Shapes.block).background(if (ending.reached) colors.soft else colors.tagBg).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (ending.reached) {
            Text(ending.ending.orEmpty(), style = VsmType.bodyBold, color = colors.text)
            Text(Labels.outcome(ending.outcome), style = VsmType.caption, color = colors.muted)
        } else {
            Text("🔒 Финал не открыт", style = VsmType.bodyBold, color = colors.text)
            Text("Попробуйте другую линию поведения", style = VsmType.caption, color = colors.muted)
        }
    }
}
