package ru.vsm.trainer.feature.gameplay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.local.HistoryEntry
import ru.vsm.trainer.data.remote.dto.Line
import ru.vsm.trainer.data.remote.dto.RunState
import ru.vsm.trainer.data.remote.dto.RunStatus
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.AnimatedNumber
import ru.vsm.trainer.design.components.CompetenceList
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.PrimaryButton
import ru.vsm.trainer.design.components.Rewards
import ru.vsm.trainer.design.components.SiteEasing
import ru.vsm.trainer.domain.story.StoryPhase
import ru.vsm.trainer.domain.story.StoryView

/** Полноэкранный режим визуальной новеллы — `StoryPlayer` сайта: сцена, персонажи, диалог, выбор, финал. */
@Composable
fun StoryPlayerScreen(
    onExit: (String?) -> Unit,
    onMap: (String) -> Unit,
    onDebrief: (String) -> Unit,
    onScenarios: () -> Unit,
    model: StoryViewModel = hiltViewModel(),
) {
    val titles by model.titles.collectAsStateWithLifecycle()
    val view by model.engine.view.collectAsStateWithLifecycle()
    val muted by model.muted.collectAsStateWithLifecycle()
    val endings by model.endings.collectAsStateWithLifecycle()
    var historyOpen by remember { mutableStateOf(false) }
    val reduce = Vsm.reduceMotion
    val run = view.run

    // Музыка — пока открыта история; голоса: детский и женский выше, мужской ниже, у проводника — ровный средний
    DisposableEffect(Unit) {
        model.audio.startMusic()
        onDispose { model.audio.stopMusic() }
    }
    LaunchedEffect(run?.characters) {
        val voices = mutableMapOf("player" to 190.0)
        run?.characters?.forEach { voices[it.id] = if (it.look.child) 330.0 else if (it.look.hairStyle == "short") 150.0 else 250.0 }
        model.audio.setVoices(voices)
    }
    LaunchedEffect(view.phase) { if (view.phase == StoryPhase.ENDING && run != null) model.loadEndings(run.scenarioId) }
    val exit = {
        model.exit()
        onExit(run?.scenarioId)
    }
    BackHandler { if (historyOpen) historyOpen = false else exit() }

    // Новелла проявляется из темноты (`story-reveal`, 0.9 с)
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(view.phase == StoryPhase.INTRO) { if (!reduce && view.phase != StoryPhase.INTRO) reveal.animateTo(1f, tween(900)) }

    Box(Modifier.fillMaxSize().background(Palette.storyBlack)) {
        if (view.phase == StoryPhase.ERROR) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(view.notice.orEmpty(), style = VsmType.body, color = Color.White, textAlign = TextAlign.Center)
                PrimaryButton("К сценариям", onScenarios)
            }
            return@Box
        }
        val scene = view.scene
        if (run == null || scene == null) {
            if (view.phase == StoryPhase.INTRO && run != null) Intro(run) { model.engine.advance() }
            return@Box
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (view.phase == StoryPhase.INTRO) 1f else reveal.value.coerceAtLeast(0.001f) }) {
            StoryBackground(scene.background)
            val cast = remember(run.characters) { run.characters.associateBy { it.id } }
            StoryCast(scene.characters, view.expressions, cast, view.line?.speaker, view.typing && view.line?.kind == "speech")
        }
        // Касание сцены — то же, что «Далее»: мгновенно допечатывает реплику или листает дальше
        if (view.phase == StoryPhase.DIALOGUE) {
            Box(
                Modifier.fillMaxSize().padding(top = 150.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { model.engine.advance() }
                    .semantics { contentDescription = "Далее" },
            )
        }
        StoryBar(
            run, view, muted, onExit = exit, onHistory = { historyOpen = true }, onSound = model::toggleSound,
            onAuto = model.engine::toggleAuto, onSkip = model.engine::skip,
        )
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp)) {
            when (view.phase) {
                StoryPhase.DIALOGUE -> view.line?.let { DialogueBox(it, view.shownChars, view.typing, model.playerName, cast = run.characters.associateBy { c -> c.id }) { model.engine.advance() } }
                StoryPhase.CHOICES -> run.node?.let { Choices(run, view, model) }
                else -> Unit
            }
        }
        view.notice?.takeIf { view.phase != StoryPhase.ENDING }?.let {
            Text(
                it, style = VsmType.small, color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 150.dp).clip(Shapes.button)
                    .background(Palette.storyBlack.copy(alpha = 0.8f)).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        if (view.phase == StoryPhase.INTRO) Intro(run) { model.engine.advance() }
        if (view.phase == StoryPhase.ENDING && run.final != null) {
            StoryEnd(run, endings, titles, onMap = { onMap(run.scenarioId) }, onDebrief = { onDebrief(run.id) }, onRestart = { model.engine.restart() }, onScenarios = onScenarios)
        }
        AnimatedVisibility(historyOpen, enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 10 }, exit = fadeOut(tween(200))) {
            HistoryPanel(view.history) { historyOpen = false }
        }
    }
}

/** Верхняя стеклянная панель: «← Выйти», точки шагов, две шкалы, История / Звук / Авто / Пропустить. */
@Composable
private fun StoryBar(
    run: RunState,
    view: StoryView,
    muted: Boolean,
    onExit: () -> Unit,
    onHistory: () -> Unit,
    onSound: () -> Unit,
    onAuto: () -> Unit,
    onSkip: () -> Unit,
) {
    val total = run.stepsTaken + maxOf(run.stepsLeft, if (run.node != null) 1 else 0)
    val step = run.lastSteps.lastOrNull()
    val brand = Vsm.colors.brand
    Column(
        Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth().clip(Shapes.storyBar).background(Palette.storyBlack.copy(alpha = 0.42f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "← Выйти", style = VsmType.storyBar.copy(fontWeight = FontWeight.SemiBold), color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.clip(Shapes.tab).clickable(role = Role.Button, onClick = onExit).padding(vertical = 6.dp)
                    .semantics { contentDescription = "Выйти, прогресс сохранится" },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = "Шаг ${run.stepsTaken + 1}" }) {
                repeat(total) { index ->
                    val done = index < run.stepsTaken
                    val current = index == run.stepsTaken && run.node != null
                    Box(
                        Modifier.size(if (current) 14.dp else 8.dp)
                            .then(if (current) Modifier.clip(CircleShape).background(brand.copy(alpha = 0.35f)).padding(3.dp) else Modifier)
                            .clip(CircleShape)
                            .background(if (done) Color.White else if (current) brand else Color.Transparent)
                            .border(1.5.dp, if (done || current) Color.White else Color.White.copy(alpha = 0.7f), CircleShape),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScaleMeter("Лояльность", run.loyalty, step?.loyaltyDelta ?: 0, Palette.storyLoyalty, run.stepsTaken, Modifier.weight(1f))
            ScaleMeter("Безопасность", run.safety, step?.safetyDelta ?: 0, Palette.storySafety, run.stepsTaken, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("История", on = false, enabled = true, modifier = Modifier.weight(1f), onClick = onHistory)
            ToolButton(if (muted) "Звук выкл." else "Звук", on = !muted, enabled = true, modifier = Modifier.weight(1f), onClick = onSound)
            ToolButton("Авто", on = view.auto, enabled = true, modifier = Modifier.weight(1f), onClick = onAuto)
            ToolButton("Пропустить", on = false, enabled = view.canSkip, modifier = Modifier.weight(1f), onClick = onSkip)
        }
    }
}

/** Шкала панели: подпись, дорожка, значение и всплывающая дельта после решения (`delta-float`, 2.6 с). */
@Composable
private fun ScaleMeter(label: String, value: Int, delta: Int, color: Color, stepKey: Int, modifier: Modifier) {
    val reduce = Vsm.reduceMotion
    val fill by animateFloatAsState(value / 100f, tween(if (reduce) 0 else 800, easing = SiteEasing), label = "scale")
    val float = remember(stepKey) { Animatable(0f) }
    LaunchedEffect(stepKey) { if (delta != 0) float.animateTo(1f, tween(if (reduce) 0 else 2600)) }
    Box(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value из 100"
        }) {
            Text(label, style = VsmType.storyBar, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            Box(Modifier.weight(1f).height(6.dp).clip(Shapes.pill).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(fill.coerceIn(0f, 1f)).height(6.dp).clip(Shapes.pill).background(color))
            }
            Text("$value", style = VsmType.storyBar.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        if (delta != 0 && float.value < 1f) {
            val t = float.value
            val alpha = when {
                t < 0.15f -> t / 0.15f
                t < 0.7f -> 1f
                else -> (1f - t) / 0.3f
            }
            Text(
                Labels.signed(delta), style = VsmType.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp),
                color = if (delta < 0) Palette.deltaDown else Palette.deltaUp,
                modifier = Modifier.align(Alignment.TopEnd).graphicsLayer {
                    this.alpha = alpha
                    translationY = -14.dp.toPx() + (if (t < 0.15f) 6.dp.toPx() * (1 - t / 0.15f) else if (t > 0.7f) -6.dp.toPx() * (t - 0.7f) / 0.3f else 0f)
                },
            )
        }
    }
}

@Composable
private fun ToolButton(text: String, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(32.dp).alpha(if (enabled) 1f else 0.4f).clip(Shapes.tab)
            .background(if (on) Color.White.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (on) Color.White else Color.White.copy(alpha = 0.35f), Shapes.tab)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = VsmType.storyBar, color = Color.White, maxLines = 1)
    }
}

/**
 * Окно диалога `.story-box`: имя говорящего заглавными, текст печатается по символу; речь — в «ёлочках»,
 * мысли — курсивом с синей полосой, рассказчик — тёмное стекло. Ненапечатанный остаток держит высоту окна.
 */
@Composable
private fun DialogueBox(line: Line, shown: Int, typing: Boolean, playerName: String, cast: Map<String, ru.vsm.trainer.data.remote.dto.Character>, onNext: () -> Unit) {
    val colors = Vsm.colors
    val narration = line.kind == "narration"
    val name = when {
        narration -> "Рассказчик"
        line.speaker == "player" -> playerName
        else -> line.name ?: cast[line.speaker]?.name.orEmpty()
    }
    val ink = if (narration) Color(0xFFEEF3FB) else colors.storyInk
    val accent = if (narration) Palette.storyNarration else colors.brand
    val pulse = rememberInfiniteTransition(label = "next")
    val nudge by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "nudge")
    Column(
        Modifier.fillMaxWidth()
            .shadow(20.dp, Shapes.storyBox, ambientColor = Palette.storyBlack, spotColor = Palette.storyBlack)
            .clip(Shapes.storyBox)
            .background(if (narration) Palette.storyBlack.copy(alpha = 0.72f) else colors.storyGlass)
            .border(1.dp, if (narration) Color.White.copy(alpha = 0.12f) else colors.storyGlassBorder, Shapes.storyBox)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            Text(name.uppercase(), style = VsmType.storyName, color = accent)
            if (!narration) line.role?.let { Text(it, style = VsmType.caption, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (line.kind == "thought") Text("мысли", style = VsmType.caption, color = colors.muted)
        }
        val quote = line.kind == "speech"
        val text = buildAnnotatedString {
            if (quote) append("«")
            append(line.text.take(shown))
            if (quote && !typing) append("»")
            withStyle(SpanStyle(color = Color.Transparent)) { append(line.text.drop(shown)) }
        }
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (line.kind == "thought") Box(Modifier.padding(end = 13.dp).width(3.dp).fillMaxHeight().background(colors.brand.copy(alpha = 0.45f)))
            Text(
                text,
                style = (if (narration) VsmType.storyNarration else VsmType.storyText).copy(fontStyle = if (line.kind == "thought") FontStyle.Italic else FontStyle.Normal),
                color = ink.copy(alpha = if (line.kind == "thought") 0.78f else 1f),
            )
        }
        Text(
            (if (typing) "Показать" else "Далее") + " →",
            style = VsmType.bodyBold, color = accent,
            modifier = Modifier.align(Alignment.End).padding(top = 8.dp).graphicsLayer { translationX = 4.dp.toPx() * nudge }
                .clip(Shapes.tab).clickable(role = Role.Button, onClick = onNext).padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}


/** Варианты `.story-choices`: полоса таймера «N с на решение» (последние 5 с — красная), карточки с номерами по очереди. */
@Composable
private fun Choices(run: RunState, view: StoryView, model: StoryViewModel) {
    val node = run.node ?: return
    val colors = Vsm.colors
    var picked by remember(node.id) { mutableStateOf<String?>(null) }
    var remaining by remember(node.id) { mutableLongStateOf(model.engine.remainingMs() ?: -1L) }
    LaunchedEffect(node.id, view.run?.node?.deadlineAt) {
        while (true) {
            remaining = model.engine.remainingMs() ?: -1L
            delay(100)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val timer = node.timerSeconds
        if (timer != null && remaining >= 0) {
            val hurry = remaining < 5000
            Box(Modifier.fillMaxWidth().height(30.dp).clip(Shapes.button).background(Palette.storyBlack.copy(alpha = 0.55f)).semantics { contentDescription = "Осталось ${(remaining + 999) / 1000} секунд" }) {
                Box(
                    Modifier.fillMaxWidth((remaining / (timer * 1000f)).coerceIn(0f, 1f)).height(30.dp)
                        .background(Brush.horizontalGradient(if (hurry) listOf(Palette.timerHurryStart, Palette.timerHurryEnd) else listOf(Color(0xFF146BFF), Color(0xFF4F8DFF)))),
                )
                Text("${(remaining + 999) / 1000} с на решение", style = VsmType.caption.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
        if (node.choices.isEmpty()) Text("…", style = VsmType.body, color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        node.choices.forEachIndexed { index, choice ->
            val enabled = !view.busy && picked == null && remaining != 0L
            val appear = remember(node.id, choice.id) { Animatable(0f) }
            LaunchedEffect(node.id, choice.id) {
                delay((index + 1) * 100L)
                appear.animateTo(1f, tween(400, easing = SiteEasing))
            }
            Row(
                Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = appear.value * (if (enabled || picked == choice.id) 1f else 0.55f); translationY = (1f - appear.value) * 14.dp.toPx() }
                    .shadow(10.dp, Shapes.storyChoice, ambientColor = Palette.storyBlack, spotColor = Palette.storyBlack)
                    .clip(Shapes.storyChoice)
                    .background(colors.storyGlass)
                    .border(if (picked == choice.id) 3.dp else 1.dp, if (picked == choice.id) colors.brand else colors.storyGlassBorder, Shapes.storyChoice)
                    .clickable(enabled = enabled, role = Role.Button) {
                        picked = choice.id
                        model.engine.choose(choice.id)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("story.choice.$index"),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(30.dp).clip(Shapes.tab).background(colors.softStrong), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = VsmType.bodyBold.copy(fontWeight = FontWeight.ExtraBold), color = colors.brandText)
                }
                Text(choice.text, style = VsmType.storyChoice, color = colors.storyInk)
            }
        }
    }
    // Если ответ не принят (обрыв связи), разрешаем выбрать снова
    LaunchedEffect(view.busy, view.notice) { if (!view.busy && view.notice != null) picked = null }
}

/** Заставка: на тёмном экране категория, класс и маршрут, затем название; касание — сразу к сцене. */
@Composable
private fun Intro(run: RunState, onSkip: () -> Unit) {
    val reduce = Vsm.reduceMotion
    val t = remember { Animatable(if (reduce) 0.5f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) t.animateTo(1f, tween(2400)) }
    val textAlpha = when {
        t.value < 0.25f -> t.value / 0.25f
        else -> 1f
    }
    val fade = if (t.value > 0.75f) 1f - (t.value - 0.75f) / 0.25f else 1f
    Box(
        Modifier.fillMaxSize().alpha(fade).background(Palette.storyBlack)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(24.dp).alpha(textAlpha), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "${Labels.category(run.category)} · ${run.serviceClass} · ${run.route}".uppercase(),
                style = VsmType.small.copy(letterSpacing = VsmType.eyebrow.letterSpacing * 0.85f), color = Palette.storyNarration, textAlign = TextAlign.Center,
            )
            Text(run.scenarioTitle, style = VsmType.storyIntroTitle, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

/** Финал — `StoryEnd`: название финала, к чему привели решения, XP, шкалы, финалы, компетенции, награды, действия. */
@Composable
private fun StoryEnd(
    run: RunState,
    endings: Pair<Int, Int>?,
    titles: Map<String, String>,
    onMap: () -> Unit,
    onDebrief: () -> Unit,
    onRestart: () -> Unit,
    onScenarios: () -> Unit,
) {
    val final = run.final ?: return
    val colors = Vsm.colors
    val reduce = Vsm.reduceMotion
    val dim = remember { Animatable(if (reduce) 0.72f else 1f) }
    val card = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        dim.animateTo(0.72f, tween(1100))
    }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(400)
        card.animateTo(1f, tween(600, easing = SiteEasing))
    }
    val eyebrow = when (final.outcome) {
        RunStatus.FAILURE -> colors.negative
        RunStatus.PARTIAL -> colors.warning
        else -> colors.brandText
    }
    Box(Modifier.fillMaxSize().background(Palette.storyBlack.copy(alpha = dim.value)).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp)) {
        Column(
            Modifier.fillMaxWidth().graphicsLayer { alpha = card.value; translationY = (1f - card.value) * 18.dp.toPx() }
                .shadow(30.dp, Shapes.storyEnd).clip(Shapes.storyEnd).background(colors.surface).padding(horizontal = 20.dp, vertical = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("СЦЕНАРИЙ ЗАВЕРШЁН", style = VsmType.eyebrow, color = eyebrow)
            Text(final.ending, style = VsmType.storyEndTitle, color = colors.heading)
            Text(final.text, style = VsmType.body.copy(lineHeight = 26.sp), color = colors.muted)
            val stats = buildList<@Composable () -> Unit> {
                add { EndStat("Получено XP") { AnimatedNumber(final.xpEarned, VsmType.storyEndTitle.copy(fontSize = 24.sp), colors.heading, prefix = "+") } }
                add { EndStat("Лояльность пассажира") { Text("${run.loyalty}%", style = VsmType.storyEndTitle.copy(fontSize = 24.sp), color = colors.heading) } }
                add { EndStat("Рейтинг безопасности") { Text("${run.safety}%", style = VsmType.storyEndTitle.copy(fontSize = 24.sp), color = colors.heading) } }
                endings?.let { (reached, total) -> add { EndStat("Открыто финалов") { Text("$reached из $total", style = VsmType.storyEndTitle.copy(fontSize = 24.sp), color = colors.heading) } } }
            }
            stats.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { Box(Modifier.weight(1f)) { it() } }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            CompetenceList(final.competencePoints, titles)
            Rewards(run.newAchievements, run.levelUp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                PrimaryButton("Посмотреть путь", onMap, Modifier.fillMaxWidth())
                GhostButton("Разбор решений", onDebrief, Modifier.fillMaxWidth().testTag("final.debrief"))
                GhostButton("Пройти заново", onRestart, Modifier.fillMaxWidth())
                GhostButton("Вернуться к сценариям", onScenarios, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EndStat(label: String, value: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(Shapes.block).background(Vsm.colors.soft).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = VsmType.caption, color = Vsm.colors.muted)
        value()
    }
}

/** Журнал реплик `.story-history`: все прочитанные реплики, решения и истёкшие таймеры. */
@Composable
private fun HistoryPanel(entries: List<HistoryEntry>, onClose: () -> Unit) {
    val colors = Vsm.colors
    val list = rememberLazyListState()
    LaunchedEffect(entries.size) { if (entries.isNotEmpty()) list.scrollToItem(entries.size - 1) }
    Column(Modifier.fillMaxSize().background(colors.storyGlass).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("История", style = VsmType.bodyBold, color = colors.storyInk, modifier = Modifier.weight(1f))
            Text("Закрыть", style = VsmType.bodyBold, color = colors.brand, modifier = Modifier.clickable(role = Role.Button, onClick = onClose))
        }
        HorizontalDivider(color = colors.border)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(entries) { entry ->
                val who = if (entry.kind == "choice") "Ваше решение" else entry.speaker
                Column(
                    Modifier.fillMaxWidth().then(if (entry.kind == "choice") Modifier.clip(Shapes.button).background(colors.soft).padding(horizontal = 14.dp, vertical = 10.dp) else Modifier),
                ) {
                    who?.let { Text(it.uppercase(), style = VsmType.caption.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = VsmType.eyebrow.letterSpacing * 0.45f, fontSize = 12.sp), color = colors.brandText) }
                    Text(
                        entry.text,
                        style = VsmType.body.copy(
                            fontStyle = if (entry.kind == "thought") FontStyle.Italic else FontStyle.Normal,
                            fontWeight = if (entry.kind == "choice" || entry.kind == "timeout") FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = when (entry.kind) {
                            "narration" -> colors.muted
                            "timeout" -> colors.negative
                            else -> colors.storyInk.copy(alpha = if (entry.kind == "thought") 0.75f else 1f)
                        },
                    )
                }
            }
        }
    }
}
