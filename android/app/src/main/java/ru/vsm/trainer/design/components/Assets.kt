package ru.vsm.trainer.design.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import ru.vsm.trainer.R

/**
 * Картинки, экспортированные из компонентов сайта (`tools/mobile-assets`): персонажи, фоны, иллюстрации.
 * Имена строятся из данных сценария (например, `sprite_p_sit_annoyed_down_none_r`), поэтому ищутся по имени.
 */
@SuppressLint("DiscouragedApi")
fun assetId(context: android.content.Context, name: String): Int =
    context.resources.getIdentifier(name, "drawable", context.packageName)

@Composable
fun assetPainter(name: String, fallback: Int = R.drawable.avatar): Painter {
    val context = LocalContext.current
    val id = remember(name) { assetId(context, name).takeIf { it != 0 } ?: fallback }
    return painterResource(id)
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
