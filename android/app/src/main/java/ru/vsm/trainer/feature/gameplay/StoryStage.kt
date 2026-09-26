package ru.vsm.trainer.feature.gameplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.data.remote.dto.Character
import ru.vsm.trainer.data.remote.dto.SceneCharacter
import ru.vsm.trainer.design.Palette
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.components.assetId
import ru.vsm.trainer.design.components.assetPainter

/** Фон сцены с плавной сменой (кроссфейд 0.7 с с лёгким приближением), затемнение сверху и снизу. */
@Composable
fun StoryBackground(name: String) {
    val reduce = Vsm.reduceMotion
    AnimatedContent(
        name,
        transitionSpec = {
            if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 1.03f)) togetherWith fadeOut(tween(700))
        },
        label = "background",
        modifier = Modifier.fillMaxSize(),
    ) { background ->
        Image(assetPainter("bg_$background", fallback = ru.vsm.trainer.R.drawable.bg_vestibule), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
    // `.story__shade`: сверху 45 % до 18 % высоты, снизу 55 % до 42 %
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Palette.storyBlack.copy(alpha = 0.45f), 0.18f to Color.Transparent)))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.58f to Color.Transparent, 1f to Palette.storyBlack.copy(alpha = 0.55f))))
}

// Как StoryStage.HANDS сайта: жест по умолчанию для эмоции
private val HANDS = mapOf("thinking" to "throat", "pained" to "chest", "worried" to "chest")

/** Имя картинки персонажа; если точного сочетания нет — базовое для позы и эмоции (справа — зеркально). */
private fun spriteFor(context: android.content.Context, uniform: Boolean, character: SceneCharacter, expression: String): Pair<String, Boolean> {
    val kind = if (uniform) "u" else "p"
    val right = character.position == "right"
    val hand = character.hand ?: HANDS[expression] ?: "down"
    val exact = "sprite_${kind}_${character.pose}_${expression}_${hand}_${character.item ?: "none"}_${if (right) "r" else "l"}"
    if (assetId(context, exact) != 0) return exact to false
    val base = "sprite_${kind}_${character.pose}_${expression}_${HANDS[expression] ?: "down"}_none_l"
    if (assetId(context, base) != 0) return base to right
    return "sprite_${kind}_${character.pose}_neutral_down_none_l" to right
}

/**
 * Персонажи сцены — `StoryCast`: слева 19 %, по центру 50 %, справа 81 % ширины; стоящие — 32 % высоты над окном диалога,
 * сидящие 26 %, дети 24 %. Говорящий чуть крупнее, остальные затемнены до 62 %.
 */
@Composable
fun StoryCast(characters: List<SceneCharacter>, expressions: Map<String, String>, cast: Map<String, Character>, speaker: String?, speaking: Boolean) {
    val someoneSpeaks = speaker != null && characters.any { it.id == speaker }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        characters.forEach { character ->
            androidx.compose.runtime.key(character.id) {
                val look = cast[character.id]?.look
                val uniform = character.id == "player" || look?.outfit == "uniform"
                val spriteHeight = height * when {
                    character.pose != "stand" -> 0.26f
                    look?.child == true -> 0.24f
                    else -> 0.32f
                }
                val center = width * when (character.position) {
                    "left" -> 0.19f
                    "right" -> 0.81f
                    else -> 0.5f
                }
                Sprite(
                    uniform = uniform,
                    character = character,
                    expression = expressions[character.id] ?: character.expression,
                    active = !someoneSpeaks || speaker == character.id,
                    talking = speaking && speaker == character.id,
                    spriteHeight = spriteHeight,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = center - spriteHeight * (72f / 104f) / 2, y = -height * 0.27f),
                )
            }
        }
    }
}

@Composable
private fun Sprite(uniform: Boolean, character: SceneCharacter, expression: String, active: Boolean, talking: Boolean, spriteHeight: Dp, modifier: Modifier) {
    val context = LocalContext.current
    val reduce = Vsm.reduceMotion
    val (name, mirror) = remember(uniform, character, expression) { spriteFor(context, uniform, character, expression) }
    val density = LocalDensity.current
    val shift = with(density) { 60.dp.toPx() }
    val from = when (character.position) {
        "left" -> -shift
        "right" -> shift
        else -> 0f
    }
    // Появление сбоку (`sprite-in`, 0.55 с)
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) enter.animateTo(1f, tween(550, easing = ru.vsm.trainer.design.components.SiteEasing)) }
    val scale by animateFloatAsState(if (active) 1.02f else 0.97f, tween(350), label = "scale")
    val brightness by animateFloatAsState(if (active) 1f else 0.62f, tween(350), label = "dim")
    // Говорящий покачивается (`sprite-talk`)
    val talk = rememberInfiniteTransition(label = "talk")
    val bob by talk.animateFloat(0f, 1f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "bob")
    val bobbing = talking && !reduce
    // Смена эмоции — короткая реакция: злость дрожит, удивление подпрыгивает, радость пружинит, грусть поникает
    val react = remember { Animatable(0f) }
    LaunchedEffect(expression) {
        if (reduce) return@LaunchedEffect
        react.snapTo(0f)
        react.animateTo(1f, tween(if (expression in setOf("sad", "pained")) 700 else 450))
    }
    val r = react.value
    val reactX = if (expression == "angry" && r < 1f) (if ((r * 4).toInt() % 2 == 0) -0.025f else 0.025f) else 0f
    val reactY = when (expression) {
        "surprised" -> -0.05f * bell(r)
        "happy" -> -0.02f * bell(r)
        "sad", "pained" -> 0.015f * bell(r)
        else -> 0f
    }
    val saturation = 0.7f + 0.3f * (brightness - 0.62f) / 0.38f
    val filter = remember(brightness, saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }.also { it.timesAssign(ColorMatrix().apply { setToScale(brightness, brightness, brightness, 1f) }) })
    }
    val heightPx = with(density) { spriteHeight.toPx() }
    Box(
        modifier.height(spriteHeight).aspectRatio(72f / 104f).graphicsLayer {
            alpha = enter.value
            translationX = (1f - enter.value) * from + reactX * size.width
            translationY = (if (bobbing) -0.012f * bob else 0f) * heightPx + reactY * heightPx
            rotationZ = if (bobbing) -0.6f * bob else 0f
            val appear = 0.96f + 0.04f * enter.value
            scaleX = scale * appear * (if (mirror) -1f else 1f)
            scaleY = scale * appear
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
    ) {
        // Тень `drop-shadow(0 18px 30px rgba(5,8,15,.35))`: размытая тёмная копия фигуры (Android 12+)
        Image(
            assetPainter(name), null,
            Modifier.fillMaxSize().graphicsLayer { translationY = 18.dp.toPx() * heightPx / 540f; alpha = 0.35f }.blur(15.dp),
            colorFilter = ColorFilter.tint(Palette.storyBlack),
        )
        Image(assetPainter(name), null, Modifier.fillMaxSize(), colorFilter = filter)
    }
}

/** 0 → 1 → 0 за время реакции. */
private fun bell(t: Float) = if (t < 0.4f) t / 0.4f else (1f - t) / 0.6f

