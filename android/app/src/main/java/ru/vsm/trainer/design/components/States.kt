package ru.vsm.trainer.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

private val dateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss", Locale("ru")).withZone(ZoneId.systemDefault())
private val date = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru")).withZone(ZoneId.systemDefault())

/** Формат дат сайта (`toLocaleString("ru-RU")`). */
fun Instant.siteDateTime(): String = dateTime.format(this)
fun Instant.siteDate(): String = date.format(this)

/**
 * Состояния экрана как на сайте: «Загрузка…» приглушённым текстом, ошибка — красным с кнопкой повтора,
 * данные без сети — плашка `.notice` с датой.
 */
@Composable
fun <T> ScreenContent(state: ScreenState<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        ScreenState.Loading -> Text("Загрузка…", style = VsmType.body, color = Vsm.colors.muted)
        is ScreenState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.message, style = VsmType.body, color = Vsm.colors.negative)
            GhostButton("Повторить", retry)
        }
        is ScreenState.Content -> Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            state.staleSince?.let { Notice("Нет связи с сервером. Показаны данные от ${it.siteDateTime()}") }
            content(state.value)
        }
    }
}

/** Плашка `.notice`: фон partial-bg, рамка notice-border, радиус 16. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    val colors = Vsm.colors
    Text(
        text,
        style = VsmType.body,
        color = colors.text,
        modifier = modifier.fillMaxWidth().clip(Shapes.block).background(colors.partialBg)
            .border(1.dp, colors.noticeBorder, Shapes.block).padding(horizontal = 18.dp, vertical = 14.dp),
    )
}
