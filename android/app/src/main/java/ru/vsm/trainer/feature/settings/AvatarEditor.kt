package ru.vsm.trainer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.vsm.trainer.data.remote.dto.Avatar
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.VsmType
import ru.vsm.trainer.design.components.AvatarOptions
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.Chip
import ru.vsm.trainer.design.components.Swatch
import ru.vsm.trainer.design.components.UserAvatar
import ru.vsm.trainer.design.components.VsmCard

/** Конструктор аватара — `AvatarEditor` сайта: фон, головной убор, галстук; выбор сразу сохраняется на сервере. */
@Composable
fun AvatarEditor(avatar: Avatar, status: String?, onChoose: (Avatar) -> Unit) {
    VsmCard(spacing = 16.dp) {
        CardTitle("Аватар")
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(112.dp, ring = 4.dp, avatar = avatar)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OptionGroup("Фон") {
                    AvatarOptions.backgrounds.forEach { (code, option) ->
                        SwatchButton(option.label, avatar.background == code, Brush.verticalGradient(listOf(option.top, option.bottom))) {
                            onChoose(avatar.copy(background = code))
                        }
                    }
                }
            }
        }
        OptionGroup("Головной убор") {
            AvatarOptions.headwear.forEach { (code, title) ->
                Chip(title, avatar.headwear == code, { onChoose(avatar.copy(headwear = code)) })
            }
        }
        OptionGroup("Галстук") {
            AvatarOptions.ties.forEach { (code, option) ->
                SwatchButton(option.label, avatar.tie == code, SolidColor(option.color)) { onChoose(avatar.copy(tie = code)) }
            }
        }
        Text(
            status ?: "Аватар собирается из деталей формы — фотографии не нужны и не хранятся.",
            style = VsmType.small, color = Vsm.colors.muted,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = VsmType.bodyStrong, color = Vsm.colors.text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun SwatchButton(label: String, selected: Boolean, brush: Brush, onClick: () -> Unit) {
    Swatch(
        brush, selected,
        Modifier.clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label; this.selected = selected },
    )
}
