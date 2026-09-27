package ru.vsm.trainer.design.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Картинки, экспортированные из компонентов сайта (`tools/mobile-assets`): персонажи, фоны, иллюстрации.
 * Имена строятся из данных сценария (например, `sprite_p_sit_annoyed_down_none_r`), поэтому ищутся по имени.
 */
@SuppressLint("DiscouragedApi")
fun assetId(context: android.content.Context, name: String): Int =
    context.resources.getIdentifier(name, "drawable", context.packageName)

/**
 * Распакованные картинки. Фон сцены — 1290 × 2796 в WebP: распаковка на главном потоке заметно
 * подтормаживала смену сцен и прокрутку карточек, поэтому она идёт в фоне, а результат переиспользуется.
 */
private object AssetCache {
    private val bitmaps = object : LruCache<Int, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()) {
        override fun sizeOf(key: Int, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    fun cached(id: Int): ImageBitmap? = bitmaps.get(id)

    fun load(context: Context, id: Int): ImageBitmap {
        cached(id)?.let { return it }
        val bitmap = BitmapFactory.decodeResource(context.resources, id)
        // Передать в видеопамять заранее, чтобы первый кадр с картинкой не ждал загрузки текстуры
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap().also { bitmaps.put(id, it) }
    }

    /** Размер без распаковки: пока картинка готовится, её место уже занято, и вёрстка не прыгает. */
    fun size(context: Context, id: Int): Size {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, id, options)
        return Size(options.outWidth.toFloat(), options.outHeight.toFloat())
    }
}

/** Пустая картинка нужного размера — пока настоящая распаковывается (доли секунды). */
private class Placeholder(override val intrinsicSize: Size) : Painter() {
    override fun DrawScope.onDraw() = Unit
}

/** Картинка из ресурсов по имени; нет такой и нет запасной — пустое место, а не чужая картинка. */
@Composable
fun assetPainter(name: String, fallback: Int? = null): Painter {
    val context = LocalContext.current
    val id = remember(name) { assetId(context, name).takeIf { it != 0 } ?: fallback }
    // При смене картинки (другая эмоция персонажа) до готовности новой видна прежняя — без мигания пустотой
    val bitmap by produceState(id?.let(AssetCache::cached), id) {
        value = id?.let { AssetCache.cached(it) ?: withContext(Dispatchers.IO) { AssetCache.load(context, it) } }
    }
    return remember(id, bitmap) { bitmap?.let(::BitmapPainter) ?: Placeholder(id?.let { AssetCache.size(context, it) } ?: Size.Zero) }
}

/** Иллюстрация сценария `ScenarioImage`: своя для каждого сценария, иначе — по типу ситуации, как на сайте. */
fun scenarioImageName(scenarioId: String, category: String): String {
    val own = "scenario_" + scenarioId.replace("-", "_")
    val byCategory = mapOf(
        "conflict" to "scenario_business_seat_conflict",
        "medical" to "scenario_passenger_unwell",
        "safety" to "scenario_unattended_item",
        "service" to "scenario_train_delay",
    )
    return if (own in KnownScenarios) own else byCategory[category] ?: "scenario_business_seat_conflict"
}

private val KnownScenarios = setOf(
    "boarding-ticket", "pet-and-bicycle", "drunk-passenger", "business-seat-conflict", "noisy-night", "passenger-unwell",
    "panic-attack", "smoke-vestibule", "unattended-item", "lost-child", "business-catering", "first-class-comfort",
    "train-delay", "wheelchair-boarding",
).map { "scenario_" + it.replace("-", "_") }.toSet()
