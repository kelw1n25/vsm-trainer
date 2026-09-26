package ru.vsm.trainer.feature.handbook

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Bullet
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.Chip
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.SoftBlock
import ru.vsm.trainer.design.components.Tag
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.VsmTextField
import ru.vsm.trainer.design.components.assetPainter

// Слайды кейсодержателя о подвижном составе и классах обслуживания ВСМ
private val SLIDES = listOf(
    "photo_service_classes" to "Классы обслуживания в поездах ВСМ",
    "photo_service_classes_layout" to "Компоновка вагонов по классам",
    "photo_rolling_stock" to "Планировка вагонов комфорт и стандарт, предлагаемые сервисы",
)

/** Справочник — `HandbookPage`: ролевая модель, классы обслуживания и слайды, стандарты, 51 ситуация с поиском. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandbookScreen(onScenario: (String) -> Unit, model: HandbookViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val query by model.query.collectAsStateWithLifecycle()
    val category by model.category.collectAsStateWithLifecycle()
    val colors = Vsm.colors
    val list = rememberLazyListState()
    var slide by remember { mutableStateOf<Pair<String, String>?>(null) }
    val content = (state as? ScreenState.Content)?.value
    val needle = query.trim().lowercase()
    val situations = content?.handbook?.situations.orEmpty().filter {
        (category == null || it.category == category) &&
            (needle.isEmpty() || "${it.title} ${it.reaction} ${it.phrases.joinToString(" ")}".lowercase().contains(needle))
    }
    // Ссылка «Ситуация N» со страницы сценария — прокрутить к ней, когда справочник загрузится
    LaunchedEffect(content, model.focusSituation) {
        val focus = model.focusSituation ?: return@LaunchedEffect
        val index = situations.indexOfFirst { it.number == focus }
        if (content != null && index >= 0) list.animateScrollToItem(FIRST_SITUATION_ITEM + index)
    }

    Page(state = list) {
        item { PageTitle("Справочник проводника") }
        item {
            Muted(
                "Материалы кейсодержателя: «Примеры ситуаций взаимодействия поездного персонала с пассажирами», стандарты " +
                    "СТО РЖД 03.011, 03.013 и 03.014, слайды о подвижном составе ВСМ. На эти материалы опираются все сценарии.",
            )
        }
        if (content == null) {
            item { ScreenContent(state, {}) {} }
            return@Page
        }
        val handbook = content.handbook
        item {
            VsmCard {
                CardTitle(handbook.roleModel.title)
                handbook.roleModel.steps.forEachIndexed { index, step ->
                    SoftBlock {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.brandText)) { append("${index + 1}. ") }
                                append(step.title)
                            },
                            style = VsmType.bodyBold, color = colors.text,
                        )
                        step.phrases.forEach { Bullet(it, colors.muted) }
                    }
                }
            }
        }
        item {
            VsmCard {
                CardTitle("Классы обслуживания")
                handbook.serviceClasses.forEach { item ->
                    Column(
                        Modifier.fillMaxWidth().clip(Shapes.block).border(1.dp, colors.border, Shapes.block).padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.title, style = VsmType.h3.copy(fontSize = VsmType.h2.fontSize * 0.85f), color = colors.heading, modifier = Modifier.padding(bottom = 4.dp))
                        Spec("Компоновка", item.layout)
                        Spec("Шаг кресел", "${item.pitchMm} мм")
                        Spec("Ширина кресла", "${item.seatMm} мм")
                        Spec("Проход", "${item.aisleMm} мм")
                        Spec("Ожидание", "до ${item.maxWaitMinutes} мин")
                        Muted(item.summary, Modifier.padding(top = 4.dp))
                    }
                }
                SLIDES.forEach { (name, caption) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(
                            assetPainter(name), caption,
                            Modifier.fillMaxWidth().clip(Shapes.image).border(1.dp, colors.border, Shapes.image).clickable(role = Role.Image) { slide = name to caption },
                            contentScale = ContentScale.FillWidth,
                        )
                        Text(caption, style = VsmType.caption, color = colors.muted)
                    }
                }
            }
        }
        item {
            VsmCard {
                CardTitle("Стандарты обслуживания")
                handbook.standards.forEach {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(it.title, style = VsmType.bodyBold, color = colors.text)
                        Muted(it.text)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CardTitle("Ситуации на борту и на посадке · ${handbook.situations.size}")
                VsmTextField(query, { model.query.value = it }, placeholder = "Поиск: билет, питомец, аллергия…")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Chip("Все", category == null, { model.category.value = null })
                    listOf("conflict", "medical", "service", "safety").forEach { code -> Chip(Labels.category(code), category == code, { model.category.value = code }) }
                }
                if (situations.isEmpty()) Muted("Ничего не найдено.")
            }
        }
        itemsIndexed(situations, key = { _, it -> it.number }) { _, situation ->
            val focused = situation.number == model.focusSituation
            SoftBlock(Modifier.then(if (focused) Modifier.border(2.dp, colors.brand, Shapes.block) else Modifier)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(Labels.category(situation.category), category = true)
                    Tag(Labels.stage(situation.stage))
                }
                Text("${situation.number}. ${situation.title}", style = VsmType.h3, color = colors.heading)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Реакция. ") }
                        append(situation.reaction)
                    },
                    style = VsmType.body, color = colors.text,
                )
                situation.phrases.forEach { Bullet(it, colors.brandText) }
                situation.comment.forEach { Bullet(it, colors.muted) }
                content.trainedIn[situation.number]?.let { scenarios ->
                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Отрабатывается:", style = VsmType.small, color = colors.text)
                        scenarios.forEachIndexed { index, scenario ->
                            Text(
                                scenario.title + if (index < scenarios.lastIndex) "," else "",
                                style = VsmType.small.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                                color = colors.brandText,
                                modifier = Modifier.clickable(role = Role.Button) { onScenario(scenario.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    slide?.let { (name, caption) -> SlideViewer(name, caption) { slide = null } }
}

// Порядок элементов списка до первой ситуации: заголовок, вступление, ролевая модель, классы, стандарты, фильтры
private const val FIRST_SITUATION_ITEM = 6

@Composable
private fun Spec(label: String, value: String) {
    Row {
        Text(label, style = VsmType.body, color = Vsm.colors.muted, modifier = Modifier.padding(end = 12.dp))
        Text(value, style = VsmType.bodyBold, color = Vsm.colors.text)
    }
}

/** Слайд на весь экран с увеличением двумя пальцами (на сайте — ссылка на файл). */
@Composable
private fun SlideViewer(name: String, caption: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Palette.storyBlack).clickable(onClick = onClose)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        offset += pan
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                assetPainter(name), caption,
                Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                contentScale = ContentScale.FillWidth,
            )
            Text(caption, style = VsmType.small, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp))
        }
    }
}
