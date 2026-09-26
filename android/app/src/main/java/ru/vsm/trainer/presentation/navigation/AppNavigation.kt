package ru.vsm.trainer.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.vsm.trainer.presentation.debrief.DebriefScreen
import ru.vsm.trainer.presentation.home.HomeScreen
import ru.vsm.trainer.presentation.player.PlayerScreen
import ru.vsm.trainer.presentation.profile.AnalyticsScreen
import ru.vsm.trainer.presentation.profile.LeaderboardScreen
import ru.vsm.trainer.presentation.profile.NotificationsScreen
import ru.vsm.trainer.presentation.profile.ProfileScreen
import ru.vsm.trainer.presentation.scenario.ScenarioDetailScreen
import ru.vsm.trainer.presentation.scenario.ScenarioListScreen

private enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Главная", Icons.Default.Home),
    SCENARIOS("scenarios", "Сценарии", Icons.AutoMirrored.Filled.List),
    RATING("rating", "Рейтинг", Icons.Default.Star),
    PROFILE("profile", "Профиль", Icons.Default.Person),
}

/** Вкладки внизу; сценарий открывается поверх них на весь экран — во время решения ничто не отвлекает. */
@Composable
fun AppNavigation(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val showTabs = Tab.entries.any { it.route == route }
    Scaffold(bottomBar = {
        if (showTabs) {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { nav.openTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) },
                    )
                }
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    onScenario = { nav.navigate("scenario/${it.id}") },
                    onPlay = { nav.navigate("player/$it") },
                    onNotifications = { nav.navigate("notifications") },
                )
            }
            composable(Tab.SCENARIOS.route) { ScenarioListScreen(onScenario = { nav.navigate("scenario/${it.id}") }) }
            composable(Tab.RATING.route) { LeaderboardScreen() }
            composable(Tab.PROFILE.route) {
                ProfileScreen(onAnalytics = { nav.navigate("analytics") }, onDebrief = { nav.navigate("debrief/$it") }, onSignOut = onSignOut)
            }
            composable("scenario/{scenarioId}") { entry ->
                ScenarioDetailScreen(
                    scenarioId = entry.arguments?.getString("scenarioId").orEmpty(),
                    onBack = nav::popBackStack,
                    onPlay = { nav.navigate("player/$it") },
                )
            }
            composable("player/{scenarioId}") {
                PlayerScreen(
                    onClose = nav::popBackStack,
                    // Разбор заменяет сценарий в стеке: «назад» из разбора ведёт туда, откуда начинали
                    onDebrief = { runId -> nav.navigate("debrief/$runId") { popUpTo(current?.destination?.id ?: 0) { inclusive = true } } },
                )
            }
            composable("debrief/{runId}") { DebriefScreen(onBack = nav::popBackStack) }
            composable("notifications") { NotificationsScreen(onBack = nav::popBackStack) }
            composable("analytics") { AnalyticsScreen(onBack = nav::popBackStack) }
        }
    }
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
