package ru.vsm.trainer.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.vsm.trainer.core.Loaded
import ru.vsm.trainer.core.ScreenState
import ru.vsm.trainer.core.firstName
import ru.vsm.trainer.core.loadFrom
import ru.vsm.trainer.data.remote.dto.ServiceClass
import ru.vsm.trainer.data.repository.AuthRepository
import ru.vsm.trainer.data.repository.TrainerRepository

/** Слайд hero: приветствие, челлендж недели или рекомендация — как `HeroCarousel` сайта. */
data class HeroSlide(val eyebrow: String, val titleTop: String, val titleBottom: String, val text: String, val note: String)

data class HomeContent(val slides: List<HeroSlide>, val serviceClasses: List<ServiceClass>)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TrainerRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<HomeContent>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<HomeContent>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.loadFrom {
                coroutineScope {
                    val scenarios = async { repository.scenarios() }
                    val meta = async { runCatching { repository.meta().value }.getOrNull() }
                    val analytics = async { runCatching { repository.analytics().value }.getOrNull() }
                    // Классы обслуживания для «Про ВСМ» — из справочника; без него главная всё равно открывается
                    val handbook = async { runCatching { repository.handbook().value }.getOrNull() }
                    val loaded = scenarios.await()
                    val name = auth.current?.fullName?.let(::firstName) ?: "коллега"
                    val slides = mutableListOf(
                        HeroSlide(
                            "Привет, $name!", "Развивай навыки —", "строй будущее ВСМ!",
                            "Пройди сценарии, получай баллы, поднимайся в рейтинге и становись экспертом ВСМ.",
                            "Твой прогресс влияет на общую безопасность!",
                        ),
                    )
                    meta.await()?.weeklyChallenge?.let { challenge ->
                        loaded.value.firstOrNull { it.id == challenge.scenarioId }?.let {
                            slides += HeroSlide(
                                "Челлендж недели", "Пройди на успех:", it.title,
                                "Заверши сценарий успешно до конца недели и получи бонус +${challenge.bonusXp} XP.",
                                "+${challenge.bonusXp} XP за успешное прохождение!",
                            )
                        }
                    }
                    analytics.await()?.recommendation?.let {
                        slides += HeroSlide("Рекомендация для тебя", "Следующий шаг:", it.title, it.reason, "Закрой пробел — и навык вырастет быстрее!")
                    }
                    Loaded(HomeContent(slides, handbook.await()?.serviceClasses.orEmpty()), loaded.staleSince)
                }
            }
        }
    }
}
