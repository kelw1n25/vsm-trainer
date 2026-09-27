package ru.vsm.trainer.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import ru.vsm.trainer.design.components.LocalMyPhoto
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.vsm.trainer.core.Labels
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.data.remote.dto.Role
import ru.vsm.trainer.design.Vsm
import ru.vsm.trainer.design.components.LocalMyAvatar
import ru.vsm.trainer.design.components.SiteBackground
import ru.vsm.trainer.feature.analytics.AnalyticsScreen
import ru.vsm.trainer.feature.debrief.DebriefScreen
import ru.vsm.trainer.feature.gameplay.StoryPlayerScreen
import ru.vsm.trainer.feature.handbook.HandbookScreen
import ru.vsm.trainer.feature.home.HomeScreen
import ru.vsm.trainer.feature.leaderboard.RatingScreen
import ru.vsm.trainer.feature.notifications.NotificationsScreen
import ru.vsm.trainer.feature.profile.ProfileScreen
import ru.vsm.trainer.feature.scenarios.ScenarioDetailScreen
import ru.vsm.trainer.feature.scenarios.ScenarioListScreen
import ru.vsm.trainer.feature.scenarios.StoryMapScreen
import ru.vsm.trainer.feature.settings.SettingsScreen
import ru.vsm.trainer.feature.team.TeamScreen

/**
 * Маршруты сайта: вкладки внизу (Главная, Сценарии, Рейтинг, Аналитика, Справочник), шапка с аватаром сверху,
 * новелла — на весь экран без шапки и навигации, как `/runs/:id` на сайте.
 */
@Composable
fun AppNavigation(onLogout: () -> Unit, shell: ShellViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val story = route?.startsWith("play/") == true
    val theme by shell.preferences.theme.collectAsStateWithLifecycle()
    val unread by shell.unread.collectAsStateWithLifecycle()
    val shift = with(LocalDensity.current) { 6.dp.roundToPx() }
    val reduce = Vsm.reduceMotion
    // Новый вход — счётчик непрочитанных нового сотрудника, не дожидаясь очередного опроса
    LaunchedEffect(Unit) {
        shell.refreshUnread()
        shell.refreshProfile()
    }
    val avatar by shell.avatar.collectAsStateWithLifecycle()
    val photoBitmap by shell.photo.collectAsStateWithLifecycle()
    val photo = remember(photoBitmap) { photoBitmap?.asImageBitmap() }
    val go = { destination: String -> nav.navigate(destination) }
    val play = { scenarioId: String -> nav.navigate("play/$scenarioId") }

    CompositionLocalProvider(LocalMyAvatar provides avatar, LocalMyPhoto provides photo) {
    SiteBackground(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        if (!story) {
            Box(Modifier.statusBarsPadding()) {
                AppHeader(
                    fullName = shell.fullName,
                    roleTitle = Labels.role(if (shell.instructor) Role.INSTRUCTOR else Role.CONDUCTOR),
                    unread = unread,
                    dark = theme == ThemeMode.DARK,
                    instructor = shell.instructor,
                    onHome = { nav.openTab(Tab.HOME.route) },
                    onToggleTheme = shell::toggleTheme,
                    onProfile = { go("profile") },
                    onNotifications = { go("notifications") },
                    onSettings = { go("settings") },
                    onTeam = { go("team") },
                    onLogout = onLogout,
                )
            }
        }
        NavHost(
            nav, startDestination = Tab.HOME.route, modifier = Modifier.weight(1f),
            // Мягкое появление страницы (`page-in`: прозрачность и сдвиг на 6, 0.28 с)
            enterTransition = { if (reduce) fadeIn(tween(0)) else fadeIn(tween(280)) + slideInVertically(tween(280)) { shift } },
            exitTransition = { fadeOut(tween(if (reduce) 0 else 150)) },
            popEnterTransition = { fadeIn(tween(if (reduce) 0 else 280)) },
            popExitTransition = { fadeOut(tween(if (reduce) 0 else 150)) },
        ) {
            composable(Tab.HOME.route) { HomeScreen(onHandbook = { nav.openTab(Tab.HANDBOOK.route) }) }
            composable(Tab.SCENARIOS.route) { ScenarioListScreen(onScenario = { go("scenario/$it") }, onPlay = play) }
            composable(Tab.RATING.route) { RatingScreen() }
            composable(Tab.ANALYTICS.route) { AnalyticsScreen(onPlay = play, onDebrief = { go("debrief/$it") }) }
            composable(
                "${Tab.HANDBOOK.route}?situation={situation}",
                arguments = listOf(navArgument("situation") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { HandbookScreen(onScenario = { go("scenario/$it") }) }
            composable("profile") { ProfileScreen(onDebrief = { go("debrief/$it") }, onScenarios = { nav.showList(Tab.SCENARIOS.route) }, onSettings = { go("settings") }) }
            composable("notifications") { NotificationsScreen(onChanged = shell::refreshUnread) }
            composable("settings") { SettingsScreen(onLogout = onLogout) }
            composable("team") { TeamScreen(onMember = { go("team/$it") }) }
            composable("team/{employeeId}") { AnalyticsScreen(onPlay = play, onDebrief = { go("debrief/$it") }) }
            composable("scenario/{scenarioId}") {
                ScenarioDetailScreen(
                    onBack = { nav.showList(Tab.SCENARIOS.route) },
                    onPlay = play,
                    onMap = { go("map/$it") },
                    onSituation = { go("${Tab.HANDBOOK.route}?situation=$it") },
                    onDebrief = { go("debrief/$it") },
                )
            }
            composable("map/{scenarioId}") { StoryMapScreen(onBack = { nav.popBackStack() }, onPlay = play) }
            composable("debrief/{runId}") {
                DebriefScreen(onMap = { go("map/$it") }, onScenario = { go("scenario/$it") }, onScenarios = { nav.showList(Tab.SCENARIOS.route) })
            }
            composable("play/{scenarioId}") {
                StoryPlayerScreen(
                    onExit = { scenarioId -> if (!nav.popBackStack()) scenarioId?.let { go("scenario/$it") } },
                    // Из финала — путь и разбор; «назад» из них ведёт туда, откуда начинали, а не в законченную историю
                    onMap = { scenarioId -> nav.navigate("map/$scenarioId") { popUpTo("play/{scenarioId}") { inclusive = true } } },
                    onDebrief = { runId -> nav.navigate("debrief/$runId") { popUpTo("play/{scenarioId}") { inclusive = true } } },
                    onScenarios = { nav.showList(Tab.SCENARIOS.route) },
                )
            }
        }
        if (!story) {
            // Как NavLink сайта: страница сценария и его развитие подсвечивают «Сценарии»
            val current = when {
                route?.startsWith("scenario/") == true || route?.startsWith("map/") == true -> Tab.SCENARIOS
                else -> Tab.entries.firstOrNull { route?.startsWith(it.route) == true }
            }
            BottomNav(current) { tab ->
                // Подсвеченная вкладка (например, «Сценарии» на странице сценария) ведёт к своему списку,
                // как ссылка на сайте, а не восстанавливает ту же страницу
                when {
                    tab != current -> nav.openTab(tab.route)
                    !nav.popBackStack(tab.route, inclusive = false) -> nav.showList(tab.route)
                }
            }
        }
    }
    }
}
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * Список вкладки с чистого листа — «← Все сценарии», «Вернуться к сценариям».
 * [openTab] вернул бы сохранённый стек вкладки, то есть ту же страницу сценария, с которой ушли.
 */
private fun NavHostController.showList(route: String) {
    clearBackStack(route)
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}
