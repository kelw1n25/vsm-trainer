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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.ScreenContent
import ru.vsm.trainer.design.components.assetPainter

/** Главная: hero-карусель (приветствие, челлендж недели, рекомендация) и «Про ВСМ» — общая картина магистрали. */
@Composable
fun HomeScreen(onHandbook: () -> Unit, model: HomeViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.load() }
    Page {
        item("content") {
            ScreenContent(state, model::load) { content ->
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroCarousel(content.slides)
                    AboutVsm(content.serviceClasses, onHandbook)
                }
            }
        }
    }
}
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
        Modifier.fillMaxWidth().clip(Shapes.card).background(colors.heroBrush)
            // Мягкое светлое пятно в углу (`.hero::before`) — фоном, не влияя на высоту карточки
            .drawBehind {
                val center = Offset(70.dp.toPx(), 10.dp.toPx())
                drawCircle(Brush.radialGradient(listOf(colors.heroGlow, Color.Transparent), center = center, radius = 210.dp.toPx()), 210.dp.toPx(), center)
            }
            .border(1.dp, colors.heroBorder, Shapes.card)
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
        Column {
            // Высота блока — по самому длинному слайду: все слайды невидимо лежат под текущим,
            // поэтому текст виден целиком, а при листании поезд и карточка не прыгают
            Box {
                slides.forEach { slide -> HeroSlideText(slide, slides.size > 1, Modifier.alpha(0f).clearAndSetSemantics {}) }
                AnimatedContent(
                    current,
                    transitionSpec = { (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 30 }) togetherWith fadeOut(tween(150)) },
                    label = "slide",
                ) { slide -> HeroSlideText(slide, slides.size > 1) }
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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp),
            )
        }
    }
}

/** Текст слайда hero; справа — место под вертикальный ползунок. */
@Composable
private fun HeroSlideText(slide: HeroSlide, withScroller: Boolean, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Column(
        modifier.fillMaxWidth().padding(start = 22.dp, end = if (withScroller) 44.dp else 22.dp, top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(slide.eyebrow.uppercase(), style = VsmType.heroEyebrow, color = colors.heroEyebrow)
        val long = slide.titleBottom.length > 24
        Text("${slide.titleTop}\n${slide.titleBottom}", style = if (long) VsmType.heroTitleLong else VsmType.heroTitle, color = colors.heading)
        Text(slide.text, style = VsmType.heroText, color = colors.heroText, modifier = Modifier.padding(top = if (long) 2.dp else 10.dp))
    }
}

/** Во сколько раз поезд крупнее ширины карточки: нос в кадре, хвост уходит за край — без пустоты под поездом. */
private const val TRAIN_SCALE = 1.35f

/** Поезд hero из трёх слоёв сайта: город смещается медленнее (параллакс), поезд — вместе с ползунком. */
@Composable
private fun HeroTrain(progress: Float) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp).aspectRatio(1000f / (300f * TRAIN_SCALE))) {
        val unit = with(LocalDensity.current) { maxWidth.toPx() } / 1000f
        val offset = progress * TRAIN_DISTANCE
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().aspectRatio(1000f / 300f).graphicsLayer {
                scaleX = TRAIN_SCALE
                scaleY = TRAIN_SCALE
                transformOrigin = TransformOrigin(0f, 1f)
            },
        ) {
            Image(assetPainter("hero_parallax"), null, Modifier.fillMaxWidth().graphicsLayer { translationX = -offset * 0.35f * unit }, contentScale = ContentScale.FillWidth)
            Image(assetPainter("hero_static"), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            Image(
                assetPainter("hero_drive"), null,
                Modifier.fillMaxWidth().graphicsLayer { translationX = -offset * unit; translationY = offset * RAIL_SLOPE * unit },
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

/**
 * Ползунок hero: тонкая вертикальная линия справа с точками слайдов и бегунком. Сверху — первый слайд,
 * снизу — последний; ведётся пальцем вверх-вниз, касание по линии — сразу к слайду.
 */
@Composable
private fun HeroScroller(value: Float, steps: Int, label: String, onChange: (Float) -> Unit, onRelease: () -> Unit, modifier: Modifier) {
    val colors = Vsm.colors
    // Зона касания — полоса 52 dp у правого края с запасом сверху и снизу: палец не промахивается,
    // и вертикальный жест забирает ползунок, а не прокрутка страницы
    val track = 132.dp
    val margin = 12.dp
    BoxWithConstraints(
        modifier.width(52.dp).height(track + margin * 2)
            .semantics { contentDescription = "Слайд: $label" }
            .pointerInput(Unit) {
                fun at(y: Float) = ((y - margin.toPx()) / track.toPx()).coerceIn(0f, 1f)
                detectVerticalDragGestures(onDragEnd = onRelease, onDragCancel = onRelease) { change, _ ->
                    change.consume()
                    onChange(at(change.position.y))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { onChange(((it.y - margin.toPx()) / track.toPx()).coerceIn(0f, 1f)); onRelease() }
            }
            .padding(vertical = margin),
        contentAlignment = Alignment.TopCenter,
    ) {
        val height = maxHeight
        Box(Modifier.width(3.dp).fillMaxHeight().clip(Shapes.pill).background(colors.trackStrong))
        Box(Modifier.width(3.dp).height(height * value).clip(Shapes.pill).background(Brush.verticalGradient(listOf(Palette.progressStart, colors.brand))))
        for (step in 0 until steps) {
            val reached = step / (steps - 1f) <= value + 0.001f
            Box(
                Modifier.offset(y = height * (step / (steps - 1f)) - 3.dp).size(6.dp).clip(CircleShape)
                    .background(if (reached) colors.brand else colors.trackStrong),
            )
        }
        Box(
            Modifier.offset(y = height * value - 7.dp).size(14.dp)
                .shadow(3.dp, CircleShape, ambientColor = colors.brand, spotColor = colors.brand)
                .clip(CircleShape).background(Color.White).border(2.5.dp, colors.brand, CircleShape),
        )
    }
}
