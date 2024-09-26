package org.muilab.noti.summary.view

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.muilab.noti.summary.view.home.HomeScreen
import org.muilab.noti.summary.view.settings.SettingsScreen
import org.muilab.noti.summary.viewModel.APIKeyViewModel
import org.muilab.noti.summary.viewModel.PromptViewModel
import org.muilab.noti.summary.viewModel.ScheduleViewModel
import org.muilab.noti.summary.viewModel.SummaryViewModel

sealed class BottomNavItem(var title: String, var icon: ImageVector, var screen_route: String) {
    object Home : BottomNavItem("Home", Icons.Filled.Home,"home")
    object Settings: BottomNavItem("Settings", Icons.Filled.Settings,"settings")
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(
    context: Context,
    sumViewModel: SummaryViewModel,
    promptViewModel: PromptViewModel,
    apiKeyViewModel: APIKeyViewModel,
    scheduleViewModel: ScheduleViewModel
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        // Apply the padding globally to the whole navController
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = BottomNavItem.Home.screen_route) {
                composable(BottomNavItem.Home.screen_route) {
                    HomeScreen(context, sumViewModel, promptViewModel)
                }
                composable(BottomNavItem.Settings.screen_route) {
                    SettingsScreen(promptViewModel, apiKeyViewModel, scheduleViewModel, context)
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Settings
    )

    var currentScreen by remember { mutableStateOf(BottomNavItem.Home.title) }

    NavigationBar(
        contentColor = Color.Black
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(30.dp)) },
                selected = currentRoute == item.screen_route,
                onClick = {
                    if (currentScreen != item.title) {
                        currentScreen = item.title
                    }
                    navController.popBackStack()
                    navController.navigate(item.screen_route) {
                        navController.graph.startDestinationRoute?.let { screen_route ->
                            popUpTo(screen_route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
