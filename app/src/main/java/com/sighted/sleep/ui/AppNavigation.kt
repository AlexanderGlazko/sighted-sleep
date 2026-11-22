package com.sighted.sleep.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.res.painterResource
import com.sighted.sleep.R
import com.sighted.sleep.ui.insights.InsightsScreen
import com.sighted.sleep.ui.timeline.TimelineScreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sighted.sleep.ui.edit.EditSessionScreen
import com.sighted.sleep.ui.edit.EditSessionViewModel
import com.sighted.sleep.ui.settings.SettingsScreen
import com.sighted.sleep.ui.today.TodayScreen

private object Routes {
    const val TODAY = "today"
    const val TIMELINE = "timeline"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val EDIT = "edit/{${EditSessionViewModel.ARG_ID}}"
    fun edit(id: Long?) = "edit/${id ?: -1}"
}

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

private val tabs = listOf(
    Tab(Routes.TODAY, "Today") { Icon(Icons.Filled.Home, contentDescription = null) },
    Tab(Routes.TIMELINE, "Timeline") { Icon(Icons.Filled.DateRange, contentDescription = null) },
    Tab(Routes.INSIGHTS, "Insights") { Icon(painterResource(R.drawable.ic_insights), contentDescription = null) },
    Tab(Routes.SETTINGS, "Settings") { Icon(Icons.Filled.Settings, contentDescription = null) },
)

/**
 * @param wakeRequest changes each time the widget asks to end the current sleep (0 = none).
 */
@Composable
fun AppNavigation(wakeRequest: Long = 0L) {
    val nav = rememberNavController()
    val bottomBar: @Composable () -> Unit = { BottomBar(nav) }

    // A wake request from the widget always lands on Today, where the check-in sheet opens.
    LaunchedEffect(wakeRequest) {
        if (wakeRequest != 0L) nav.navigateToTab(Routes.TODAY)
    }

    NavHost(navController = nav, startDestination = Routes.TODAY) {
        composable(Routes.TODAY) {
            TodayScreen(
                wakeRequest = wakeRequest,
                onEditSession = { nav.navigate(Routes.edit(it)) },
                onAddSession = { nav.navigate(Routes.edit(null)) },
                bottomBar = bottomBar,
            )
        }
        composable(Routes.TIMELINE) {
            TimelineScreen(
                onEditSession = { nav.navigate(Routes.edit(it)) },
                onAddSession = { nav.navigate(Routes.edit(null)) },
                bottomBar = bottomBar,
            )
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen(bottomBar = bottomBar)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(bottomBar = bottomBar)
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument(EditSessionViewModel.ARG_ID) { type = NavType.LongType }),
        ) {
            EditSessionScreen(onDone = { nav.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { nav.navigateToTab(tab.route) },
                icon = tab.icon,
                label = { Text(tab.label) },
            )
        }
    }
}
