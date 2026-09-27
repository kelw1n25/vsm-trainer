package ru.vsm.trainer.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.data.local.AppPreferences
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.data.remote.dto.Avatar
import ru.vsm.trainer.data.remote.dto.Profile
import ru.vsm.trainer.data.repository.TrainerRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TrainerRepository,
    val preferences: AppPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val profile = MutableStateFlow<Profile?>(null)
    val avatar = repository.myAvatar
    val photo = repository.myPhoto

    /** Итог последнего сохранения аватара или фото: «Сохранено» или текст ошибки. */
    val avatarStatus = MutableStateFlow<String?>(null)
    val uploading = MutableStateFlow(false)

    init {
        viewModelScope.launch { runCatching { profile.value = repository.profile().value } }
    }

    fun saveAvatar(next: Avatar) {
        avatarStatus.value = null
        viewModelScope.launch {
            avatarStatus.value = try {
                repository.updateAvatar(next)
                "Сохранено"
            } catch (error: ApiError) {
                error.message
            }
        }
    }

    /** Фото из галереи — только после согласия сотрудника (галочка в карточке «Аватар»). */
    fun uploadPhoto(uri: Uri) {
        avatarStatus.value = null
        uploading.value = true
        viewModelScope.launch {
            avatarStatus.value = try {
                val jpeg = withContext(Dispatchers.Default) { PhotoFile.jpeg(context, uri) }
                repository.uploadPhoto(jpeg)
                "Фото сохранено"
            } catch (error: IOException) {
                "Не удалось открыть фото — выберите другой снимок"
            } catch (error: ApiError) {
                error.message
            } finally {
                uploading.value = false
            }
        }
    }

    fun removePhoto() {
        avatarStatus.value = null
        viewModelScope.launch {
            avatarStatus.value = try {
                repository.deletePhoto()
                "Фото удалено с сервера"
            } catch (error: ApiError) {
                error.message
            }
        }
    }

    fun setDark(dark: Boolean) = preferences.setTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)

    fun setReduceMotion(enabled: Boolean) = preferences.setReduceMotion(enabled)
}
