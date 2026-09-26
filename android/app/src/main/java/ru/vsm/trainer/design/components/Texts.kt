package ru.vsm.trainer.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.R
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.remote.dto.Achievement
import ru.vsm.trainer.data.remote.dto.Level
import ru.vsm.trainer.design.Shapes
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType

/** `.page-title`. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = VsmType.pageTitle, color = Vsm.colors.heading, modifier = modifier.semantics { heading() })
}

/** `.section__title` со счётчиком `.count-badge`. */
@Composable
fun SectionTitle(text: String, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text, style = VsmType.sectionTitle, color = Vsm.colors.heading, modifier = Modifier.semantics { heading() })
        count?.let { CountBadge(it) }
    }
}

/** `h2` внутри карточки. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = VsmType.h2, color = Vsm.colors.heading, modifier = modifier.padding(bottom = 2.dp).semantics { heading() })
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(text, style = if (small) VsmType.small else VsmType.body, color = Vsm.colors.muted, modifier = modifier)
}

/** Маршрут с меткой `.route`: «📍 Москва — Санкт-Петербург · Бизнес». */
@Composable
fun RouteRow(route: String, serviceClass: String) {
    val colors = Vsm.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_pin), null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Text("$route · $serviceClass", style = VsmType.route, color = colors.muted)
    }
}

/** `CompetenceList`: изменения очков по компетенциям, нулевые не показываются. */
@Composable
fun CompetenceList(points: Map<String, Int>, titles: Map<String, String>) {
    val colors = Vsm.colors
    val entries = points.filterValues { it != 0 }
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        entries.forEach { (code, value) -> Bullet("${titles[code] ?: code}: ${Labels.signed(value)}", if (value < 0) colors.negative else colors.positive) }
    }
}

/** `Rewards`: новый уровень и ачивки, полученные только что. */
@Composable
fun Rewards(achievements: List<Achievement>, levelUp: Level?) {
    if (achievements.isEmpty() && levelUp == null) return
    val colors = Vsm.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        levelUp?.let {
            OutlinedBlock(borderColor = colors.brandBorder, background = colors.soft, index = 0) {
                Text("Новый уровень ${it.level}: ${it.title}", style = VsmType.bodyBold, color = colors.text)
            }
        }
        achievements.forEachIndexed { index, achievement ->
            OutlinedBlock(borderColor = colors.successBorder, background = colors.surface, index = index + 1) {
                Text("🏅 ${achievement.title}", style = VsmType.bodyBold, color = colors.text)
                Text(achievement.description, style = VsmType.body, color = colors.muted)
            }
        }
    }
}

/** Подпись и значение, как `<dt>/<dd>` сайта. */
@Composable
fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VsmType.body, color = Vsm.colors.muted)
        Text(value, style = VsmType.bodyStrong, color = Vsm.colors.text)
    }
}

/** Подвал сайта: «ВСМ · Геймификация обучения проводников / Демо-версия · все данные синтетические». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteFooter() {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
        HorizontalDivider(color = Vsm.colors.border)
        FlowRow(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ВСМ · Геймификация обучения проводников", style = VsmType.small, color = Vsm.colors.text)
            Text("Демо-версия · все данные синтетические", style = VsmType.small, color = Vsm.colors.muted)
        }
    }
}

/** Блок награды `.reward` / ачивки `.achievement`: рамка, радиус 16, появление `reward-in` по очереди. */
@Composable
fun OutlinedBlock(
    borderColor: Color,
    background: Color,
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .riseIn(index, stepMs = 150, durationMs = 500)
            .clip(Shapes.block)
            .background(background)
            .border(1.dp, borderColor, Shapes.block)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/** Пункт списка `<li>` с висячим отступом: перенос строки встаёт под текст, а не под маркер. */
@Composable
fun Bullet(text: String, color: Color = Vsm.colors.text, style: androidx.compose.ui.text.TextStyle = VsmType.body) {
    Row {
        Text("•", style = style, color = color, modifier = Modifier.padding(start = 4.dp, end = 10.dp))
        Text(text, style = style, color = color)
    }
}
