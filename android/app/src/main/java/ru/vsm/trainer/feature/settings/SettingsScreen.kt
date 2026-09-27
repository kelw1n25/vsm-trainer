package ru.vsm.trainer.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.design.components.CardTitle
import ru.vsm.trainer.design.components.DetailRow
import ru.vsm.trainer.design.components.GhostButton
import ru.vsm.trainer.design.components.LocalMyPhoto
import ru.vsm.trainer.design.components.Muted
import ru.vsm.trainer.design.components.Page
import ru.vsm.trainer.design.components.PageTitle
import ru.vsm.trainer.design.components.VsmCard
import ru.vsm.trainer.design.components.VsmSwitch

/** Настройки — `SettingsPage`: учётная запись, аватар (своё фото или конструктор), тёмная тема и «Уменьшить анимацию», выход. */
@Composable
fun SettingsScreen(onLogout: () -> Unit, model: SettingsViewModel = hiltViewModel()) {
    val profile by model.profile.collectAsStateWithLifecycle()
    val theme by model.preferences.theme.collectAsStateWithLifecycle()
    val reduceMotion by model.preferences.reduceMotion.collectAsStateWithLifecycle()
    val avatar by model.avatar.collectAsStateWithLifecycle()
    val avatarStatus by model.avatarStatus.collectAsStateWithLifecycle()
    val uploading by model.uploading.collectAsStateWithLifecycle()
    // Системный выбор фото: доступ ко всей галерее приложению не нужен — только к выбранному снимку
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(model::uploadPhoto)
    }
    Page {
        item { PageTitle("Настройки") }
        item {
            VsmCard {
                CardTitle("Учётная запись")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow("Сотрудник", profile?.fullName ?: "—")
                    DetailRow("Табельный номер", profile?.personnelNumber ?: "—")
                    DetailRow("Роль", profile?.let { Labels.role(it.role) } ?: "—")
                    DetailRow("Бригада и депо", profile?.let { "${it.brigade} · ${it.depot}" } ?: "—")
                }
                Muted("Данные учётной записи ведутся в HR-системе и обновляются через интеграцию.")
            }
        }
        item {
            AvatarEditor(
                avatar = avatar,
                photo = LocalMyPhoto.current,
                status = avatarStatus,
                uploading = uploading,
                onChoose = model::saveAvatar,
                onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePhoto = model::removePhoto,
            )
        }
        item {
            VsmCard(spacing = 16.dp) {
                CardTitle("Интерфейс")
                VsmSwitch(theme == ThemeMode.DARK, model::setDark, "Тёмная тема", "то же, что ползунок в шапке")
                VsmSwitch(reduceMotion, model::setReduceMotion, "Уменьшить анимацию", "отключает переходы и эффекты появления")
            }
        }
        item { GhostButton("Выйти из системы", onLogout, Modifier.fillMaxWidth()) }
    }
}
