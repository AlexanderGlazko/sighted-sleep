package com.sighted.sleep.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.res.painterResource
import com.sighted.sleep.R
import com.sighted.sleep.reminders.AppRequest
import com.sighted.sleep.ui.pages.insights.InsightsScreen
import com.sighted.sleep.ui.pages.timeline.TimelineScreen
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
import com.sighted.sleep.ui.pages.edit.EditSessionScreen
import com.sighted.sleep.ui.pages.edit.EditSessionViewModel
import com.sighted.sleep.ui.pages.settings.SettingsScreen
import com.sighted.sleep.ui.pages.today.TodayScreen

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
 * @param request the latest widget/notification request; a new [AppRequest.token] means a new request.
 */
@Composable
fun AppNavigation(request: AppRequest? = null) {
    val nav = rememberNavController()
    val bottomBar: @Composable () -> Unit = { BottomBar(nav) }

    // Wake and check-in land on Today (which opens the sheet); edit opens the sleep's editor.
    LaunchedEffect(request?.token) {
        when (request?.kind) {
            AppRequest.Kind.WAKE, AppRequest.Kind.CHECK_IN -> nav.navigateToTab(Routes.TODAY)
            AppRequest.Kind.EDIT -> {
                nav.navigateToTab(Routes.TODAY)
                nav.navigate(Routes.edit(request.sessionId))
            }
            null -> Unit
        }
    }

    NavHost(navController = nav, startDestination = Routes.TODAY) {
        composable(Routes.TODAY) {
            TodayScreen(
                request = request,
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
