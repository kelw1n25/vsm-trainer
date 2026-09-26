package ru.vsm.trainer.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ru.vsm.trainer.R
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.LinkAction
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.SectionTitle
import ru.vsm.trainer.design.components.assetPainter
import ru.vsm.trainer.feature.scenarios.ScenarioCard

/** Главная — `HomePage`: hero-карусель, «Сценарии» со счётчиком, три карточки, «Все сценарии». */
@Composable
fun HomeScreen(
    onScenario: (String) -> Unit,
    onPlay: (String) -> Unit,
    onAllScenarios: () -> Unit,
    model: HomeViewModel = hiltViewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.load() }
    Page {
        item("content") {
            ScreenContent(state, model::load) { content ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroCarousel(content.slides)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionTitle("Сценарии", content.scenarios.size)
                        LinkAction("Все сценарии", onAllScenarios, leading = R.drawable.ic_grid, trailing = R.drawable.ic_chevron_right)
                    }
                    content.scenarios.take(PREVIEW_COUNT).forEachIndexed { index, scenario ->
                        ScenarioCard(scenario, index, onOpen = { onScenario(scenario.id) }, onStart = { onPlay(scenario.id) })
                    }
                }
            }
        }
    }
}

private const val PREVIEW_COUNT = 3
// Сколько поезд проезжает за весь ход ползунка (единицы viewBox иллюстрации 1000 × 300)
private const val TRAIN_DISTANCE = 220f
private const val RAIL_SLOPE = 0.0198f

/**
 * Hero с ползунком — `HeroCarousel` сайта: приветствие, челлендж недели, рекомендация. Ползунок ведёт поезд;
 * отпустили — поезд докатывается до ближайшего слайда. На телефоне вместо курсора и колеса — палец.
 */
@Composable
fun HeroCarousel(slides: List<HeroSlide>) {
    val colors = Vsm.colors
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val segments = (slides.size - 1).coerceAtLeast(1)
    val current = slides[(progress.value * (slides.size - 1)).roundToInt().coerceIn(0, slides.size - 1)]
    fun snapTo(point: Float) = scope.launch { progress.animateTo(point.coerceIn(0f, 1f), tween(300)) }

    Box(
        Modifier.fillMaxWidth().clip(Shapes.card).background(colors.heroBrush).border(1.dp, colors.heroBorder, Shapes.card)
            .pointerInput(slides.size) {
                // Свайп по hero листает слайды — как колесо мыши на сайте
                var total = 0f
                detectHorizontalDragGestures(onDragEnd = {
                    val index = (progress.value * segments).roundToInt()
                    val next = when {
                        total < -60 -> index + 1
                        total > 60 -> index - 1
                        else -> index
                    }
                    total = 0f
                    snapTo(next.coerceIn(0, segments) / segments.toFloat())
                }) { _, delta -> total += delta }
            },
    ) {
        // Мягкое светлое пятно в углу (`.hero::before`)
        Box(Modifier.offset((-140).dp, (-200).dp).size(420.dp).background(Brush.radialGradient(listOf(colors.heroGlow, Color.Transparent))))
        Column {
            AnimatedContent(
                current,
                transitionSpec = { (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 30 }) togetherWith fadeOut(tween(150)) },
                label = "slide",
            ) { slide ->
                Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(slide.eyebrow.uppercase(), style = VsmType.heroEyebrow, color = colors.heroEyebrow)
                    val long = slide.titleBottom.length > 24
                    Text("${slide.titleTop}\n${slide.titleBottom}", style = if (long) VsmType.heroTitleLong else VsmType.heroTitle, color = colors.heading)
                    Text(
                        slide.text, style = VsmType.heroText, color = colors.heroText,
                        maxLines = if (long) 1 else 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = if (long) 2.dp else 10.dp),
                    )
                }
            }
            HeroTrain(progress.value)
        }
        if (slides.size > 1) {
            HeroScroller(
                value = progress.value,
                steps = slides.size,
                label = current.eyebrow,
                onChange = { scope.launch { progress.snapTo(it) } },
                onRelease = { snapTo((progress.value * segments).roundToInt() / segments.toFloat()) },
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 14.dp),
            )
        }
    }
}

/** Поезд hero из трёх слоёв сайта: город смещается медленнее (параллакс), поезд — вместе с ползунком. */
@Composable
private fun HeroTrain(progress: Float) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 24.dp).aspectRatio(1000f / 300f)) {
        val unit = with(LocalDensity.current) { maxWidth.toPx() } / 1000f
        val offset = progress * TRAIN_DISTANCE
        Image(assetPainter("hero_parallax"), null, Modifier.fillMaxWidth().graphicsLayer { translationX = -offset * 0.35f * unit }, contentScale = ContentScale.FillWidth)
        Image(assetPainter("hero_static"), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        Image(
            assetPainter("hero_drive"), null,
            Modifier.fillMaxWidth().graphicsLayer { translationX = -offset * unit; translationY = offset * RAIL_SLOPE * unit },
            contentScale = ContentScale.FillWidth,
        )
    }
}

/** Ползунок hero (`HeroScroller`): стеклянная плашка, дорожка с делениями, бегунок с синей обводкой. */
@Composable
private fun HeroScroller(value: Float, steps: Int, label: String, onChange: (Float) -> Unit, onRelease: () -> Unit, modifier: Modifier) {
    val colors = Vsm.colors
    Column(
        modifier.shadow(6.dp, Shapes.storyBar, ambientColor = colors.shadow.copy(alpha = 0.1f), spotColor = colors.shadow.copy(alpha = 0.1f))
            .clip(Shapes.storyBar).background(colors.glassSoft).padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = "Слайд: $label" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(
            Modifier.width(220.dp).height(22.dp).pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = onRelease) { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) }
            }.pointerInput(Unit) {
                detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)); onRelease() }
            },
            contentAlignment = Alignment.CenterStart,
        ) {
            val width = maxWidth
            Box(Modifier.fillMaxWidth().height(6.dp).clip(Shapes.pill).background(colors.trackStrong))
            Box(Modifier.width(width * value).height(6.dp).clip(Shapes.pill).background(Brush.horizontalGradient(listOf(Palette.progressStart, colors.brand))))
            for (step in 0 until steps) {
                Box(Modifier.offset(x = width * (step / (steps - 1f)) - 2.dp).size(4.dp).clip(CircleShape).background(Color.White))
            }
            Box(
                Modifier.offset(x = width * value - 11.dp).size(22.dp)
                    .shadow(4.dp, CircleShape, ambientColor = colors.brand, spotColor = colors.brand)
                    .clip(CircleShape).background(Color.White).border(3.dp, colors.brand, CircleShape),
            )
        }
        Text("⇆ ведите пальцем по ползунку", style = VsmType.caption.copy(fontSize = VsmType.caption.fontSize * 0.92f), color = colors.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
