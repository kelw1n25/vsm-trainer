package ru.vsm.trainer.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.R
import ru.vsm.trainer.data.remote.dto.ServiceClass
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.Bullet
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.LinkAction
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.SectionTitle
import ru.vsm.trainer.design.components.Tag
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.feature.profile.Kpi
import ru.vsm.trainer.feature.profile.KpiGrid

/**
 * «Про ВСМ» на главной — общая картина за полминуты чтения: цифры магистрали, классы обслуживания
 * и три правила, на которых держатся сценарии. Классы и нормы — из справочника кейсодержателя на сервере.
 */
@Composable
fun AboutVsm(serviceClasses: List<ServiceClass>, onHandbook: () -> Unit) {
    val colors = Vsm.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Про ВСМ")
        Muted("Высокоскоростная магистраль Москва — Санкт-Петербург: премиальный сервис, где решения принимаются за секунды.")
        KpiGrid(
            listOf(
                Kpi("Скорость", text = "до 400 км/ч"),
                Kpi("Москва — СПб", text = "≈ 2 ч 15 мин"),
                Kpi("Классы сервиса", text = if (serviceClasses.isEmpty()) "—" else "${serviceClasses.size}"),
                Kpi("Стоянка на станции", text = "≈ 1 мин"),
            ),
        )
        if (serviceClasses.isNotEmpty()) {
            VsmCard(spacing = 10.dp) {
                CardTitle("Классы и ожидание сервиса")
                serviceClasses.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = colors.border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, style = VsmType.bodyBold, color = colors.text, modifier = Modifier.weight(1f))
                        Tag(item.layout)
                        Text("до ${item.maxWaitMinutes} мин", style = VsmType.small, color = colors.muted)
                    }
                }
            }
        }
        VsmCard(spacing = 8.dp) {
            CardTitle("Что важно проводнику")
            Bullet("О прибытии объявлять за 10–15 минут")
            Bullet("Неотложные просьбы, например первая помощь, — вне очереди")
            Bullet("Говорить по ролевой модели: признать → правило → решение → заверить")
        }
        LinkAction("Подробнее — в справочнике", onHandbook, trailing = R.drawable.ic_chevron_right)
    }
}
