package org.muilab.noti.summary.view.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.muilab.noti.summary.R
import org.muilab.noti.summary.viewModel.APIKeyViewModel
import org.muilab.noti.summary.viewModel.PromptViewModel
import org.muilab.noti.summary.viewModel.ScheduleViewModel


enum class SettingScreenItem(var titleId: Int, var iconId: Int, var description: Int) {
    Start(R.string.main_setting, R.drawable.settings, R.string.empty),
    SettingPrompt(R.string.prompt, R.drawable.setting_sms, R.string.prompt_description),
    SettingScheduler(R.string.scheduled_summary, R.drawable.schedule, R.string.scheduler_description),
    SettingAppFilter(R.string.app_covered, R.drawable.play_store, R.string.app_filter_description),
    SettingNotiFilter(R.string.noti_info_covered, R.drawable.mail, R.string.noti_filter_description),
    SettingAPI(R.string.openai_api_key, R.drawable.setting_key, R.string.api_key_description),
    Feedback(R.string.feedback, R.drawable.feedback, R.string.empty),
    About(R.string.about, R.drawable.about, R.string.empty),
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    promptViewModel: PromptViewModel,
    apiKeyViewModel: APIKeyViewModel,
    scheduleViewModel: ScheduleViewModel,
    context: Context,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen =
        SettingScreenItem.valueOf(
            backStackEntry?.destination?.route ?: SettingScreenItem.Start.name
        )

    Scaffold(
        topBar = {
            SettingTopBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = {
                    navController.navigateUp()
                }
            )
        }
    ) { innerPadding ->
        NavigateSetting(
            navController = navController,
            modifier = modifier.padding(innerPadding),
            context,
            promptViewModel,
            apiKeyViewModel,
            scheduleViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTopBar(
    currentScreen: SettingScreenItem,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showDialog = remember { mutableStateOf(false) }
    if (currentScreen != SettingScreenItem.Start) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(currentScreen.titleId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.size(5.dp))
                    IconButton(
                        modifier = Modifier
                            .size(25.dp)
                            .padding(3.dp),
                        onClick = { showDialog.value = currentScreen.description != R.string.empty }
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Settings Info")
                    }
                }
            },
            modifier = modifier,
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            },
        )
    } else {
        Text(
            text = stringResource(R.string.settings),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
    }
    if (showDialog.value) {
        AlertDialog(
            title = { },
            text = { Text(stringResource(currentScreen.description)) },
            onDismissRequest = { showDialog.value = false },
            confirmButton = {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDialog.value = false }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 30.dp, end = 30.dp)
                    )
                }
            },
        )
    }
}

@Composable
fun NavigateSetting(
    navController: NavHostController,
    modifier: Modifier,
    context: Context,
    promptViewModel: PromptViewModel,
    apiKeyViewModel: APIKeyViewModel,
    scheduleViewModel: ScheduleViewModel
) {
    NavHost(navController, startDestination = SettingScreenItem.Start.name, modifier = modifier) {
        composable(SettingScreenItem.Start.name) {
            MainSettingScreen(context, navController)
        }
        composable(SettingScreenItem.SettingPrompt.name) {
            PromptScreen(context, promptViewModel)
        }
        composable(SettingScreenItem.SettingAPI.name) {
            APIKeyScreen(apiKeyViewModel)
        }
        composable(SettingScreenItem.SettingScheduler.name) {
            SchedulerScreen(context, scheduleViewModel)
        }
        composable(SettingScreenItem.SettingAppFilter.name) {
            AppFilterScreen(context)
        }
        composable(SettingScreenItem.SettingNotiFilter.name) {
            NotiFilterScreen(context)
        }
        composable(SettingScreenItem.Feedback.name) {
            FeedbackScreen()
        }
        composable(SettingScreenItem.About.name) {
            AboutScreen()
        }
    }
}
