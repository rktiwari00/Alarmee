package app.wakewalk.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.wakewalk.data.preferences.UserPreferencesRepository
import app.wakewalk.ui.create.CreateEditAlarmScreen
import app.wakewalk.ui.create.CreateEditAlarmViewModel
import app.wakewalk.ui.home.HomeScreen
import app.wakewalk.ui.home.HomeViewModel
import app.wakewalk.ui.onboarding.OnboardingScreen
import app.wakewalk.ui.settings.SettingsScreen
import app.wakewalk.ui.settings.SettingsViewModel
import app.wakewalk.ui.statistics.StatisticsScreen
import app.wakewalk.ui.statistics.StatisticsViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object CreateEditAlarm : Screen("create_edit_alarm/{alarmId}") {
        fun createRoute(alarmId: Long = 0L) = "create_edit_alarm/$alarmId"
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        title = "Alarms",
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm
    ),
    BottomNavItem(
        route = Screen.Statistics.route,
        title = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun WakeWalkNavigation(
    preferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController()
) {
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    if (preferences == null) {
        return // Wait for preferences to load
    }

    val startDestination = if (preferences?.onboardingCompleted == true) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        coroutineScope.launch {
                            preferencesRepository.setOnboardingCompleted(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateEditAlarm.createRoute(0L))
                    },
                    onNavigateToEdit = { alarmId ->
                        navController.navigate(Screen.CreateEditAlarm.createRoute(alarmId))
                    }
                )
            }

            composable(
                route = Screen.CreateEditAlarm.route,
                arguments = listOf(
                    navArgument("alarmId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
                val createViewModel: CreateEditAlarmViewModel = hiltViewModel()
                CreateEditAlarmScreen(
                    alarmId = alarmId,
                    viewModel = createViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Statistics.route) {
                val statsViewModel: StatisticsViewModel = hiltViewModel()
                StatisticsScreen(
                    viewModel = statsViewModel
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
