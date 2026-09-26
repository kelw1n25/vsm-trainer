package ru.vsm.trainer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.data.remote.dto.Line
import ru.vsm.trainer.data.remote.dto.Step
import ru.vsm.trainer.presentation.theme.VsmColors

private val dateFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru")).withZone(ZoneId.systemDefault())

fun Instant.shortDate(): String = dateFormat.format(this)

/** Загрузка / ошибка с повтором / данные — одинаково на всех экранах. */
@Composable
fun <T> StateContent(state: ScreenState<T>, retry: () -> Unit, content: @Composable (T, Instant?) -> Unit) {
    when (state) {
        ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is ScreenState.Failed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Не удалось загрузить", style = MaterialTheme.typography.titleMedium)
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Button(onClick = retry) { Text("Повторить") }
        }
        is ScreenState.Content -> content(state.value, state.staleSince)
    }
}

/** Показ сохранённых данных без сети — цифры могут быть неактуальны. */
@Composable
fun StaleBanner(since: Instant) {
    Text(
        "Нет связи. Данные от ${since.shortDate()}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().background(Color(0x26FFC107), RoundedCornerShape(10.dp)).padding(10.dp),
    )
}

/** Шкала лояльности или безопасности 0–100 с изменением за последний шаг. */
@Composable
fun ScaleBar(title: String, value: Int, delta: Int, tint: Color) {
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "$title: $value из 100" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (delta != 0) {
                Text(
                    Labels.signed(delta),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (delta > 0) VsmColors.Positive else VsmColors.Negative,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("$value", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            color = if (value < 30) VsmColors.Negative else tint,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/** Реплика: рассказчик — курсивом, мысль — в скобках, речь — с именем говорящего. */
@Composable
fun LineView(line: Line) {
    when (line.kind) {
        "narration" -> Text(line.text, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        "thought" -> Text("(${line.text})", fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> {
            val mine = line.speaker == "player"
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(10.dp),
            ) {
                Text(if (mine) "Вы" else line.name.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(line.text)
            }
        }
    }
}

/** Последствия шага: что выбрано, как отреагировали пассажиры, как изменились шкалы. */
@Composable
fun ConsequenceCard(step: Step) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (step.isTimeout) "Время вышло" else step.text, style = MaterialTheme.typography.titleSmall)
        step.reaction.forEach { LineView(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Лояльность ${Labels.signed(step.loyaltyDelta)}", color = deltaColor(step.loyaltyDelta), style = MaterialTheme.typography.labelMedium)
            Text("Безопасность ${Labels.signed(step.safetyDelta)}", color = deltaColor(step.safetyDelta), style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun deltaColor(delta: Int) = if (delta < 0) VsmColors.Negative else VsmColors.Positive

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}
