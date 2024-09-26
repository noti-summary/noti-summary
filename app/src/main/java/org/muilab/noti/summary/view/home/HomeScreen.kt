package org.muilab.noti.summary.view.home


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.simform.ssjetpackcomposeprogressbuttonlibrary.SSButtonState
import com.simform.ssjetpackcomposeprogressbuttonlibrary.SSButtonType
import com.simform.ssjetpackcomposeprogressbuttonlibrary.SSJetPackComposeProgressButtonMaterial3
import org.muilab.noti.summary.R
import org.muilab.noti.summary.viewModel.PromptViewModel
import org.muilab.noti.summary.viewModel.SummaryViewModel

@Composable
fun HomeScreen(
    context: Context,
    sumViewModel: SummaryViewModel,
    promptViewModel: PromptViewModel
) {

    val (submitButtonState, setSubmitButtonState) = remember { mutableStateOf(SSButtonState.IDLE) }

    val drawerCardState = remember { mutableStateOf(true) }
    val summaryCardState = remember { mutableStateOf(true) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val minorHeight = (
        with(LocalDensity.current) {MaterialTheme.typography.bodyLarge.lineHeight.toDp()}
        + 32.dp + 50.dp + 110.dp
    )
    val maxMainHeight = screenHeight - minorHeight
    val titleHeight = with(LocalDensity.current) {
        MaterialTheme.typography.headlineSmall.lineHeight.toDp()
    }
    val collapseHeight = titleHeight + 16.dp
    val drawerCardHeight by animateDpAsState(
        targetValue = if (drawerCardState.value && summaryCardState.value)
            maxMainHeight / 2
        else if (drawerCardState.value)
            maxMainHeight - collapseHeight
        else
            collapseHeight,
        animationSpec = tween(durationMillis = 500)
    )
    val summaryCardHeight by animateDpAsState(
        targetValue = if (drawerCardState.value && summaryCardState.value)
            maxMainHeight / 2
        else if (summaryCardState.value)
            maxMainHeight - collapseHeight
        else
            collapseHeight,
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                .height(drawerCardHeight)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            drawerCardState.value = if (!summaryCardState.value) {
                                drawerCardState.value
                            } else {
                                !drawerCardState.value
                            }
                        }) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.my_notifications),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!drawerCardState.value)
                            Icon(
                                painter = painterResource(id = R.drawable.expand_arrow),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(titleHeight)
                                    .padding(4.dp)
                            )
                        else if (summaryCardState.value)
                            Icon(
                                painter = painterResource(id = R.drawable.collapse_arrow),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(titleHeight)
                                    .padding(4.dp)
                            )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
                NotiDrawer(context, sumViewModel)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                .height(summaryCardHeight)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            summaryCardState.value = if (!drawerCardState.value) {
                                summaryCardState.value
                            } else {
                                !summaryCardState.value
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.my_summary),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        if (summaryCardState.value) {
                            Spacer(modifier = Modifier.padding(16.dp))
                            val apiSharedPref = context.getSharedPreferences("ApiPref", Context.MODE_PRIVATE)
                            val userAPIKey = apiSharedPref.getString("userAPIKey", stringResource(R.string.key_not_provided))
                            val displayAPIKey = "sk-**********${userAPIKey?.takeLast(4)}"
                            val displayText = "${stringResource(R.string.using_ur_apikey)}\n$displayAPIKey"
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (!summaryCardState.value)
                            Icon(
                                painter = painterResource(id = R.drawable.expand_arrow),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(titleHeight)
                                    .padding(4.dp)
                            )
                        else if (drawerCardState.value)
                            Icon(
                                painter = painterResource(id = R.drawable.collapse_arrow),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(titleHeight)
                                    .padding(4.dp)
                            )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
                SummaryCard(context, sumViewModel, promptViewModel, submitButtonState, setSubmitButtonState)
            }
        }
        Row (
            Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 30.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            SubmitButton(screenWidth - 212.dp, sumViewModel, submitButtonState)
            Spacer(Modifier.size(50.dp))
            ModelToggle(context)
        }
    }

    val appUpdateManager = AppUpdateManagerFactory.create(context)
    val appUpdateInfoTask = appUpdateManager.appUpdateInfo
    var showUpdateDialog by remember { mutableStateOf(0) }

    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        ) { showUpdateDialog = if (showUpdateDialog == -1) -1 else 1 }
    }

    if (showUpdateDialog == 1) {
        fun openAppInPlayStore(context: Context) {
            val packageName = context.packageName
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setPackage(packageName)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Google Play Store app is not installed on the device, open the Play Store website
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setPackage(packageName)
                context.startActivity(intent)
            }
        }
        AlertDialog(
            title = { Text(stringResource(R.string.update_title)) },
            text = { Text(stringResource(R.string.update_text)) },
            onDismissRequest = { showUpdateDialog = -1 },
            confirmButton = {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showUpdateDialog = 1
                        openAppInPlayStore(context)
                    }
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
fun SubmitButton(
    width: Dp,
    sumViewModel: SummaryViewModel,
    submitButtonState: SSButtonState
) {

    Box (Modifier.width(width), contentAlignment = Alignment.BottomCenter) {

        SSJetPackComposeProgressButtonMaterial3(
            type = SSButtonType.CLOCK,
            width = 180.dp,
            height = 50.dp,
            onClick = {
                if (submitButtonState != SSButtonState.LOADING)
                    sumViewModel.getSummaryText()
            },
            assetColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(R.string.generate_summary),
            buttonState = submitButtonState
        )
    }
}

@Composable
fun ModelToggle(context: Context) {

    val sharedPref = context.getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)
    val modelChoice = remember { mutableStateOf(sharedPref.getBoolean("model", false)) }

    Switch(
        modifier = Modifier.width(50.dp),
        checked = modelChoice.value,
        onCheckedChange = {
            modelChoice.value = !modelChoice.value
            with (sharedPref.edit()) {
                putBoolean("model", modelChoice.value)
                apply()
            }
        },
    )
    Spacer(Modifier.size(10.dp))
    Text (
        text = "${stringResource(R.string.using)}\n" +
                if (modelChoice.value) { "GPT-4" } else { "GPT-3.5" },
        modifier = Modifier.width(70.dp),
        textAlign = TextAlign.Center,
    )
}