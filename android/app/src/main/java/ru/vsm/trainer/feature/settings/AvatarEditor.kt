package ru.vsm.trainer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
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
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Swatch
import ru.vsm.trainer.design.components.UserAvatar
import ru.vsm.trainer.design.components.VsmCard

/**
 * Аватар — `AvatarEditor` сайта: своё фото (только с согласием на обработку) или конструктор — фон,
 * головной убор, галстук. Выбор сразу сохраняется на сервере; пока стоит фото, конструктор неактивен.
 */
@Composable
fun AvatarEditor(
    avatar: Avatar,
    photo: ImageBitmap?,
    status: String?,
    uploading: Boolean,
    onChoose: (Avatar) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val constructor = photo == null
    VsmCard(spacing = 16.dp) {
        CardTitle("Аватар")
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(112.dp, ring = 4.dp, avatar = avatar, photo = photo)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Своё фото", style = VsmType.bodyStrong, color = Vsm.colors.text)
                if (photo != null) {
                    Muted("Его видите только вы: коллеги в рейтинге видят инициалы.", small = true)
                    GhostButton("Убрать фото", onRemovePhoto)
                } else {
                    PhotoUpload(uploading, onPickPhoto)
                }
            }
        }
        if (!constructor) Muted("Конструктор — для аватара без фото.", small = true)
        Column(Modifier.alpha(if (constructor) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OptionGroup("Фон") {
                AvatarOptions.backgrounds.forEach { (code, option) ->
                    SwatchButton(option.label, avatar.background == code, constructor, Brush.verticalGradient(listOf(option.top, option.bottom))) {
                        onChoose(avatar.copy(background = code))
                    }
                }
            }
            OptionGroup("Головной убор") {
                AvatarOptions.headwear.forEach { (code, title) ->
                    Chip(title, avatar.headwear == code, { onChoose(avatar.copy(headwear = code)) }, enabled = constructor)
                }
            }
            OptionGroup("Галстук") {
                AvatarOptions.ties.forEach { (code, option) ->
                    SwatchButton(option.label, avatar.tie == code, constructor, SolidColor(option.color)) { onChoose(avatar.copy(tie = code)) }
                }
            }
        }
        Text(
            status ?: "Без своего фото аватар собирается из деталей формы.",
            style = VsmType.small, color = Vsm.colors.muted,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Согласие на обработку фото (152-ФЗ) — без него выбрать снимок нельзя. */
@Composable
private fun PhotoUpload(uploading: Boolean, onPickPhoto: () -> Unit) {
    var consent by rememberSaveable { mutableStateOf(false) }
    val colors = Vsm.colors
    Row(
        Modifier.fillMaxWidth().toggleable(consent, role = Role.Checkbox) { consent = it },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            consent, onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = colors.brand, uncheckedColor = colors.controlBorder),
        )
        Text(
            "Согласен(на) на обработку моей фотографии для аватара. Фото видно только мне, его можно удалить в любой момент.",
            style = VsmType.small, color = colors.muted,
        )
    }
    GhostButton(if (uploading) "Загружаем…" else "Загрузить фото", onPickPhoto, enabled = consent && !uploading)
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
private fun SwatchButton(label: String, selected: Boolean, enabled: Boolean, brush: Brush, onClick: () -> Unit) {
    Swatch(
        brush, selected,
        Modifier.clip(CircleShape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label; this.selected = selected },
    )
}
